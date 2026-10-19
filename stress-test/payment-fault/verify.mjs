import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

const MOCK_URL = process.env.MOCK_URL || 'http://localhost:18080';
const API_URL = process.env.API_URL || 'http://localhost:8081';
const COMPOSE = ['compose', '--env-file', '../../.env'];

const env = Object.fromEntries(readFileSync('../../.env', 'utf8').split('\n')
    .filter((line) => line.includes('=')).map((line) => [line.slice(0, line.indexOf('=')), line.slice(line.indexOf('=') + 1)]));

function sql(query) {
    const out = execFileSync('docker', [...COMPOSE, 'exec', '-T', '-e', `MYSQL_PWD=${env.DB_PASSWORD}`, 'mysql',
        'mysql', '-uroot', '-N', '-B', env.DB_NAME, '-e', query], { encoding: 'utf8' });
    return out.trim() ? out.trim().split('\n').map((row) => row.split('\t')) : [];
}

function sh(...args) {
    try { return execFileSync('docker', [...COMPOSE, ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim(); }
    catch (e) { return ''; }
}

const mock = await (await fetch(`${MOCK_URL}/__state`)).json();
const pgApproved = new Set(mock.approvedOrderIds);

const ULID_CHARS = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const ulidTime = (id) => [...id.slice(0, 10)].reduce((acc, c) => acc * 32 + ULID_CHARS.indexOf(c), 0);
const inThisRun = ([orderId]) => ulidTime(orderId) >= mock.startedAt;

const payments = new Map(sql('SELECT order_id, payment_status FROM payment').filter(inThisRun).map(([orderId, status]) => [orderId, status]));
const tickets = new Map(sql("SELECT order_id, status FROM tickets WHERE order_id NOT LIKE 'ORD-%'").filter(inThisRun).map(([orderId, status]) => [orderId, status]));

const tally = (iter) => [...iter].reduce((acc, key) => ({ ...acc, [key]: (acc[key] || 0) + 1 }), {});

const moneyWithoutTicket = [];
const ticketWithoutMoney = [];
const stillUnknown = [];
for (const orderId of pgApproved) {
    const status = payments.get(orderId);
    if (status === 'UNKNOWN') stillUnknown.push(orderId);
    else if (status !== 'SUCCESS') moneyWithoutTicket.push({ orderId, payment: status ?? null, ticket: tickets.get(orderId) ?? null });
}
for (const [orderId, status] of payments) {
    if (status === 'SUCCESS' && !pgApproved.has(orderId)) ticketWithoutMoney.push(orderId);
}
const confirmedTicketWithoutSuccess = [...tickets].filter(([orderId, status]) =>
    status === 'CONFIRMED' && payments.get(orderId) !== 'SUCCESS').map(([orderId]) => orderId);

const offsets = sh('exec', '-T', 'kafka', 'kafka-run-class', 'kafka.tools.GetOffsetShell', '--bootstrap-server', 'kafka:9092');
const topicSizes = {};
for (const line of offsets.split('\n')) {
    const [topic, , offset] = line.split(':');
    if (topic?.startsWith('payment')) topicSizes[topic] = (topicSizes[topic] || 0) + Number(offset);
}

let circuits = null;
try { circuits = (await (await fetch(`${API_URL}/actuator/circuitbreakers`)).json()).circuitBreakers; } catch (e) { }

const report = {
    pg: { approved: pgApproved.size, confirmStats: mock.stats.confirm, queryStats: mock.stats.query },
    db: { paymentStatus: tally(payments.values()), ticketStatus: tally(tickets.values()) },
    invariants: {
        moneyWithoutTicket: moneyWithoutTicket.length,
        ticketWithoutMoney: ticketWithoutMoney.length,
        confirmedTicketWithoutSuccess: confirmedTicketWithoutSuccess.length,
        pgApprovedButStillUnknown: stillUnknown.length,
    },
    samples: {
        moneyWithoutTicket: moneyWithoutTicket.slice(0, 5),
        ticketWithoutMoney: ticketWithoutMoney.slice(0, 5),
        confirmedTicketWithoutSuccess: confirmedTicketWithoutSuccess.slice(0, 5),
    },
    kafkaTopicMessages: topicSizes,
    circuitBreakers: circuits && Object.fromEntries(Object.entries(circuits).map(([name, cb]) =>
        [name, { state: cb.state, failureRate: cb.failureRate, slowCallRate: cb.slowCallRate, notPermitted: cb.notPermittedCalls }])),
};
console.log(JSON.stringify(report, null, 2));
