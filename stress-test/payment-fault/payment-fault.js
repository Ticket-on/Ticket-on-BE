import http from 'k6/http';
import { sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const MOCK_URL = __ENV.MOCK_URL || 'http://localhost:18080';
const RATE = Number(__ENV.RATE || 30);

const PHASES = [
    { name: '1_normal', mode: 'normal', durationSec: 45 },
    { name: '2_pg_5xx', mode: 'error', durationSec: 45 },
    { name: '3_recover', mode: 'normal', durationSec: 45 },
    { name: '4_pg_hang', mode: 'slow', durationSec: 45 },
    { name: '5_recover', mode: 'normal', durationSec: 60 },
];
const TOTAL_SEC = PHASES.reduce((sum, p) => sum + p.durationSec, 0);

const OUTCOME_BY_CODE = {
    E4002: 'rejected',
    E4005: 'unknown',
    E4006: 'pg_unavailable',
    E4007: 'circuit_open',
};

const OUTCOMES = ['approved', 'rejected', 'unknown', 'pg_unavailable', 'circuit_open', 'purchase_failed', 'other'];

const confirmOutcome = new Counter('confirm_outcome');
const confirmDuration = new Trend('confirm_duration', true);
const circuitState = new Counter('circuit_state_sample');

export const options = {
    setupTimeout: '30s',
    scenarios: {
        buyers: {
            executor: 'constant-arrival-rate',
            exec: 'buy',
            rate: RATE,
            timeUnit: '1s',
            duration: `${TOTAL_SEC}s`,
            preAllocatedVUs: 200,
            maxVUs: 2000,
        },
        observer: {
            executor: 'constant-vus',
            exec: 'observe',
            vus: 1,
            duration: `${TOTAL_SEC}s`,
        },
    },
    summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
    thresholds: Object.fromEntries([
        ...PHASES.flatMap((p) => OUTCOMES.map((o) => [`confirm_outcome{phase:${p.name},outcome:${o}}`, ['count>=0']])),
        ...PHASES.map((p) => [`confirm_duration{phase:${p.name}}`, ['p(95)>=0']]),
    ]),
};

export function setup() {
    http.post(`${MOCK_URL}/__reset`);
    http.post(`${MOCK_URL}/__timeline`, JSON.stringify({ phases: PHASES }), { headers: { 'Content-Type': 'application/json' } });
    return { startedAt: Date.now() };
}

function phaseAt(startedAt) {
    let elapsed = (Date.now() - startedAt) / 1000;
    for (const phase of PHASES) {
        if (elapsed < phase.durationSec) return phase.name;
        elapsed -= phase.durationSec;
    }
    return 'after';
}

const headers = { 'Content-Type': 'application/json' };

export function buy(data) {
    const memberId = Math.floor(Math.random() * 100000) + 1;
    const purchase = http.post(`${BASE_URL}/v1/api/tickets/purchase`,
        JSON.stringify({ ticketTypeId: 1, memberId, amount: 150000, quantity: 1 }),
        { headers, tags: { name: 'purchase' } });
    if (purchase.status !== 200) {
        confirmOutcome.add(1, { phase: phaseAt(data.startedAt), outcome: 'purchase_failed' });
        if (Math.random() < 0.05) console.error(`purchase ${purchase.status} ${purchase.body}`);
        return;
    }
    const orderId = purchase.json('data.orderId');

    sleep(1);

    const phase = phaseAt(data.startedAt);
    const res = http.post(`${BASE_URL}/v1/api/payments/confirm`,
        JSON.stringify({ memberId, orderId, paymentKey: `pk-${orderId}`, amount: 150000 }),
        { headers, tags: { name: 'confirm', phase }, timeout: '60s' });

    let outcome = 'approved';
    if (res.status !== 200) {
        let code = null;
        try { code = res.json('error.errorCode'); } catch (e) { }
        outcome = OUTCOME_BY_CODE[code] || 'other';
        if (outcome === 'other' && Math.random() < 0.05) console.error(`confirm ${res.status} ${res.body}`);
    }
    confirmOutcome.add(1, { phase, outcome });
    confirmDuration.add(res.timings.duration, { phase, outcome });
}

let lastState = null;

export function observe(data) {
    const res = http.get(`${BASE_URL}/actuator/circuitbreakers`, { tags: { name: 'observer' } });
    if (res.status === 200) {
        const breakers = res.json('circuitBreakers') || {};
        const state = `confirm=${breakers['toss-confirm']?.state} query=${breakers['toss-query']?.state}`;
        if (state !== lastState) {
            console.log(`[${phaseAt(data.startedAt)} t=${Math.round((Date.now() - data.startedAt) / 1000)}s] circuit ${state}`);
            lastState = state;
        }
        circuitState.add(1, { phase: phaseAt(data.startedAt), state: breakers['toss-confirm']?.state });
    }
    sleep(1);
}

export function handleSummary(data) {
    const rows = {};
    for (const [key, metric] of Object.entries(data.metrics)) {
        const match = key.match(/^confirm_outcome\{(.*)\}$/);
        if (!match) continue;
        const tags = Object.fromEntries(match[1].split(',').map((kv) => kv.split(':')));
        rows[tags.phase] ??= {};
        if (metric.values.count > 0) rows[tags.phase][tags.outcome] = metric.values.count;
    }
    const latency = {};
    for (const [key, metric] of Object.entries(data.metrics)) {
        const match = key.match(/^confirm_duration\{phase:([^}]*)\}$/);
        if (match) latency[match[1]] = Object.fromEntries(Object.entries(metric.values).map(([k, v]) => [k, Math.round(v)]));
    }
    const out = { outcomes: rows, confirmLatencyMs: latency };
    return {
        stdout: `\n=== confirm outcome by phase ===\n${JSON.stringify(out, null, 2)}\n`,
        'result/k6-summary.json': JSON.stringify(out, null, 2),
    };
}
