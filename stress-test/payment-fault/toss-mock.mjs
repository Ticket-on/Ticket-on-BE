import http from 'node:http';

const PORT = Number(process.env.PORT || 8080);
const READ_TIMEOUT_EXCEEDING_MS = 25_000;

const approved = new Map();
const stats = { confirm: {}, query: {} };
let timeline = [{ name: 'normal', mode: 'normal', durationSec: 1e9 }];
let timelineStartedAt = Date.now();

const MODES = {
    normal: { approveThenError: 0, errorNoApprove: 0, hangApproved: 0, hangNotApproved: 0, latencyMs: [30, 120] },
    error: { approveThenError: 0.3, errorNoApprove: 0.7, hangApproved: 0, hangNotApproved: 0, latencyMs: [5, 30] },
    slow: { approveThenError: 0, errorNoApprove: 0, hangApproved: 0.5, hangNotApproved: 0.5, latencyMs: [30, 120] },
};

function currentPhase() {
    let elapsed = (Date.now() - timelineStartedAt) / 1000;
    for (const phase of timeline) {
        if (elapsed < phase.durationSec) return phase;
        elapsed -= phase.durationSec;
    }
    return { name: 'after', mode: 'normal' };
}

function count(kind, outcome) {
    const phase = currentPhase().name;
    stats[kind][phase] ??= {};
    stats[kind][phase][outcome] = (stats[kind][phase][outcome] || 0) + 1;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const between = ([min, max]) => min + Math.random() * (max - min);
const kst = (date) => new Date(date.getTime() + 9 * 3600_000).toISOString().replace('Z', '+09:00');

function readBody(req) {
    return new Promise((resolve) => {
        let data = '';
        req.on('data', (c) => (data += c));
        req.on('end', () => resolve(data ? JSON.parse(data) : {}));
    });
}

function send(res, status, body) {
    if (res.destroyed) return;
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(body));
}

function approve(orderId, paymentKey, amount) {
    if (!approved.has(orderId)) {
        const now = new Date();
        approved.set(orderId, {
            orderId, paymentKey, totalAmount: amount, status: 'DONE',
            requestedAt: kst(new Date(now.getTime() - 50)), approvedAt: kst(now),
        });
    }
    return approved.get(orderId);
}

const SERVER_ERROR = { code: 'FAILED_INTERNAL_SYSTEM_PROCESSING', message: 'mock outage' };

async function applyFault(kind, onApprove) {
    const mode = MODES[currentPhase().mode];
    const roll = Math.random();
    let edge = mode.approveThenError;
    if (roll < edge) { onApprove(); await sleep(between(mode.latencyMs)); return { status: 503, body: SERVER_ERROR, outcome: 'approved_then_503' }; }
    if (roll < (edge += mode.errorNoApprove)) { await sleep(between(mode.latencyMs)); return { status: 503, body: SERVER_ERROR, outcome: '503' }; }
    if (roll < (edge += mode.hangApproved)) { onApprove(); await sleep(READ_TIMEOUT_EXCEEDING_MS); return { status: 504, body: SERVER_ERROR, outcome: 'approved_then_hang' }; }
    if (roll < (edge += mode.hangNotApproved)) { await sleep(READ_TIMEOUT_EXCEEDING_MS); return { status: 504, body: SERVER_ERROR, outcome: 'hang' }; }
    await sleep(between(mode.latencyMs));
    return null;
}

async function confirm(req, res) {
    const { orderId, paymentKey, amount } = await readBody(req);
    const fault = await applyFault('confirm', () => approve(orderId, paymentKey, amount));
    if (fault) { count('confirm', fault.outcome); return send(res, fault.status, fault.body); }
    count('confirm', approved.has(orderId) ? 'idempotent_replay' : 'approved');
    send(res, 200, approve(orderId, paymentKey, amount));
}

async function query(res, orderId) {
    const fault = await applyFault('query', () => {});
    if (fault) { count('query', fault.outcome); return send(res, fault.status, fault.body); }
    const payment = approved.get(orderId);
    count('query', payment ? 'done' : 'not_found');
    if (!payment) return send(res, 404, { code: 'NOT_FOUND_PAYMENT', message: 'not found' });
    send(res, 200, payment);
}

const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://mock');
    try {
        if (req.method === 'POST' && url.pathname === '/v1/payments/confirm') return await confirm(req, res);
        const orderMatch = url.pathname.match(/^\/v1\/payments\/orders\/(.+)$/);
        if (req.method === 'GET' && orderMatch) return await query(res, decodeURIComponent(orderMatch[1]));
        if (req.method === 'POST' && url.pathname === '/__timeline') {
            timeline = (await readBody(req)).phases;
            timelineStartedAt = Date.now();
            return send(res, 200, { ok: true });
        }
        if (req.method === 'POST' && url.pathname === '/__reset') {
            approved.clear(); stats.confirm = {}; stats.query = {};
            timeline = [{ name: 'normal', mode: 'normal', durationSec: 1e9 }];
            timelineStartedAt = Date.now();
            return send(res, 200, { ok: true });
        }
        if (req.method === 'GET' && url.pathname === '/__state') {
            return send(res, 200, { startedAt: timelineStartedAt, phase: currentPhase(), stats, approvedOrderIds: [...approved.keys()] });
        }
        send(res, 404, { code: 'NOT_FOUND', message: req.url });
    } catch (e) {
        send(res, 500, { code: 'MOCK_ERROR', message: String(e) });
    }
});

server.keepAliveTimeout = 65_000;
server.requestTimeout = 0;
server.listen(PORT, () => console.log(`toss-mock listening on ${PORT}`));
