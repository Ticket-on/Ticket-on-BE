import json, sys

PROM = {"type": "prometheus", "uid": "prometheus"}
MYSQL = {"type": "mysql", "uid": "mysql"}
CONFIRM = 'uri="/v1/api/payments/confirm"'
panels = []
pid = [0]
y = [0]

def nid():
    pid[0] += 1
    return pid[0]

def row(title):
    panels.append({"type": "row", "id": nid(), "title": title, "collapsed": False,
                   "gridPos": {"h": 1, "w": 24, "x": 0, "y": y[0]}, "panels": []})
    y[0] += 1

def prom(expr, legend, ref="A"):
    return {"datasource": PROM, "expr": expr, "legendFormat": legend, "refId": ref, "range": True}

def sql(query, ref="A", fmt="table"):
    return {"datasource": MYSQL, "rawSql": query, "format": fmt, "refId": ref, "rawQuery": True, "editorMode": "code"}

def ts(title, desc, targets, x, w, h=8, unit="short", stack=False, overrides=None, max_=None):
    defaults = {"unit": unit, "custom": {"lineWidth": 2, "fillOpacity": 15 if stack else 5,
                                          "stacking": {"mode": "normal" if stack else "none"},
                                          "showPoints": "never"}}
    if max_ is not None:
        defaults["max"] = max_
    return {"type": "timeseries", "id": nid(), "title": title, "description": desc, "datasource": targets[0]["datasource"],
            "targets": targets, "gridPos": {"h": h, "w": w, "x": x, "y": y[0]},
            "fieldConfig": {"defaults": defaults, "overrides": overrides or []},
            "options": {"legend": {"displayMode": "table", "placement": "bottom", "calcs": ["max", "lastNotNull"]},
                        "tooltip": {"mode": "multi", "sort": "desc"}}}

def stat(title, desc, targets, x, w, h=5, steps=None, unit="short"):
    return {"type": "stat", "id": nid(), "title": title, "description": desc, "datasource": targets[0]["datasource"],
            "targets": targets, "gridPos": {"h": h, "w": w, "x": x, "y": y[0]},
            "fieldConfig": {"defaults": {"unit": unit, "color": {"mode": "thresholds"},
                                         "thresholds": {"mode": "absolute", "steps": steps or [{"color": "green", "value": None}]}},
                            "overrides": []},
            "options": {"colorMode": "background", "graphMode": "none", "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                        "textMode": "value_and_name"}}

def color(name, c):
    return {"matcher": {"id": "byName", "options": name}, "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": c}}]}

def place(*ps, h=8):
    panels.extend(ps)
    y[0] += h

row("1. 사용자 체감 — 결제 승인 API")
place(
    ts("승인 요청 결과 (req/s)", "200=승인, 202=결과 불명(UNKNOWN), 503=PG 불가·서킷 OPEN, 4xx=거절",
       [prom(f'sum by (status) (rate(http_server_requests_seconds_count{{{CONFIRM}}}[$__rate_interval]))', "{{status}}")],
       0, 12, stack=True, unit="reqps",
       overrides=[color("200", "green"), color("202", "orange"), color("503", "red"), color("400", "purple")]),
    ts("승인 응답시간", "p95 가 PG 읽기 타임아웃(20s)에 붙으면 PG 무응답 구간",
       [prom(f'histogram_quantile(0.5, sum by (le) (rate(http_server_requests_seconds_bucket{{{CONFIRM}}}[$__rate_interval])))', "p50", "A"),
        prom(f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{{CONFIRM}}}[$__rate_interval])))', "p95", "B"),
        prom(f'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{{{CONFIRM}}}[$__rate_interval])))', "p99", "C")],
       12, 12, unit="s"),
)

row("2. PG · 서킷 브레이커")
circuit_state = {
    "type": "state-timeline", "id": nid(), "title": "서킷 상태", "description": "CLOSED → OPEN → HALF_OPEN 전이. OPEN 동안 PG 호출 없이 즉시 실패",
    "datasource": PROM,
    "targets": [prom('2 * max by (name) (resilience4j_circuitbreaker_state{state="open"}) + max by (name) (resilience4j_circuitbreaker_state{state="half_open"})', "{{name}}")],
    "gridPos": {"h": 6, "w": 12, "x": 0, "y": y[0]},
    "fieldConfig": {"defaults": {"color": {"mode": "thresholds"},
                                 "mappings": [{"type": "value", "options": {
                                     "0": {"text": "CLOSED", "color": "green"},
                                     "1": {"text": "HALF_OPEN", "color": "yellow"},
                                     "2": {"text": "OPEN", "color": "red"}}}],
                                 "thresholds": {"mode": "absolute", "steps": [{"color": "green", "value": None}, {"color": "yellow", "value": 1}, {"color": "red", "value": 2}]}},
                    "overrides": []},
    "options": {"showValue": "never", "mergeValues": True, "rowHeight": 0.8, "legend": {"showLegend": False}},
}
place(
    circuit_state,
    ts("서킷 실패율 · 느린 호출 비율", "50% 넘으면 OPEN. 느린 호출은 응답이 끝나야 집계됨",
       [prom('resilience4j_circuitbreaker_failure_rate', "{{name}} 실패율", "A"),
        prom('resilience4j_circuitbreaker_slow_call_rate', "{{name}} 느린 호출", "B")],
       12, 12, h=6, unit="percent"),
    h=6,
)
place(
    ts("PG 호출 결과 (calls/s)", "successful / failed(PG_UNAVAILABLE) / not_permitted(서킷 OPEN 으로 차단)",
       [prom('sum by (name, kind) (rate(resilience4j_circuitbreaker_calls_seconds_count[$__rate_interval]))', "{{name}} {{kind}}", "A"),
        prom('sum by (name) (rate(resilience4j_circuitbreaker_not_permitted_calls_total[$__rate_interval]))', "{{name}} not_permitted", "B")],
       0, 12, unit="reqps"),
    ts("PG 호출 평균 시간", "toss-confirm · toss-query 평균 응답시간",
       [prom('sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_sum[$__rate_interval])) / sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count[$__rate_interval]))', "{{name}}")],
       12, 12, unit="s"),
)

row("3. 자원 — PG 가 느려질 때 먼저 바닥나는 것")
place(
    ts("Tomcat 스레드", "busy 가 max 에 붙으면 결제와 무관한 API 도 멈춤",
       [prom('tomcat_threads_busy_threads', "busy", "A"), prom('tomcat_threads_config_max_threads', "max", "B"),
        prom(f'sum(http_server_requests_active_seconds_gcount{{{CONFIRM}}})', "처리 중인 승인 요청", "C")],
       0, 8),
    ts("DB 커넥션 풀 (Hikari)", "pending > 0 이면 커넥션 대기 발생",
       [prom('sum(hikaricp_connections_active)', "active", "A"), prom('sum(hikaricp_connections_pending)', "pending", "B"),
        prom('sum(hikaricp_connections_max)', "max", "C")],
       8, 8),
    ts("JVM 스레드 상태", "",
       [prom('jvm_threads_states_threads', "{{state}}", "A")],
       16, 8),
)

row("4. 비동기 파이프라인 — outbox → Debezium → Kafka → 컨슈머")
place(
    ts("컨슈머 lag", "쌓이기만 하면 컨슈머가 못 따라가는 중",
       [prom('sum by (consumergroup, topic) (kafka_consumergroup_lag{topic=~"payment.*|ticket.*"})', "{{consumergroup}} / {{topic}}")],
       0, 12),
    ts("토픽 유입량 (msg/s)", "payment-unknown 유입 = UNKNOWN 발생, dlt 유입 = 사람이 봐야 할 건",
       [prom('sum by (topic) (rate(kafka_topic_partition_current_offset{topic=~"payment.*|ticket.*"}[30s]))', "{{topic}}")],
       12, 12, unit="short"),
)
place(
    ts("리스너 처리 결과 (/s)", "#0=PaymentConsumer, #1=PaymentUnknownConsumer(-retry, -dlt), #2=티켓 발급. KafkaBackoffException 은 재시도 대기라 정상",
       [prom('sum by (name, result, exception) (rate(spring_kafka_listener_seconds_count[$__rate_interval]))', "{{name}} {{result}} {{exception}}")],
       0, 24, h=7),
    h=7,
)

row("5. 정합성 (MySQL) — 돈과 티켓이 맞는가")
lost_sql = ("SELECT COUNT(*) AS `승인됐는데 결제 행 없음` FROM outbox_message o "
            "LEFT JOIN payment p ON p.order_id = JSON_UNQUOTE(JSON_EXTRACT(o.payload, '$.orderId')) "
            "WHERE o.topic = 'payment-confirm' AND p.payment_id IS NULL")
place(
    stat("UNKNOWN 결제", "결과 불명으로 남은 결제. 계속 남아 있으면 DLT 확인 필요",
         [sql("SELECT COUNT(*) AS `UNKNOWN` FROM payment WHERE payment_status = 'UNKNOWN'")], 0, 6,
         steps=[{"color": "green", "value": None}, {"color": "orange", "value": 1}]),
    stat("승인됐는데 결제 행 없음", "payment-confirm outbox 는 있는데 payment 가 없음 = 돈은 빠졌는데 티켓 미확정 의심 (처리 중인 건 잠깐 포함)",
         [sql(lost_sql)], 6, 6,
         steps=[{"color": "green", "value": None}, {"color": "red", "value": 1}]),
    stat("결제 상태", "",
         [sql("SELECT SUM(payment_status = 'SUCCESS') AS SUCCESS, SUM(payment_status = 'FAILED') AS FAILED, SUM(payment_status = 'UNKNOWN') AS UNKNOWN FROM payment")], 12, 6,
         steps=[{"color": "blue", "value": None}]),
    stat("티켓 상태", "",
         [sql("SELECT SUM(status = 'CONFIRMED') AS CONFIRMED, SUM(status = 'PENDING') AS PENDING, SUM(status = 'CANCELLED') AS CANCELLED FROM tickets WHERE order_id NOT LIKE 'ORD-%'")], 18, 6,
         steps=[{"color": "blue", "value": None}]),
    h=5,
)
place(
    ts("결제 결과 추이 (10초 단위)", "requested_at 기준. DB 시각이 KST 라 UTC 로 변환해 그림",
       [sql("SELECT $__timeGroupAlias(t, '10s'), payment_status AS metric, COUNT(*) AS value "
            "FROM (SELECT CONVERT_TZ(requested_at, '+09:00', '+00:00') AS t, payment_status FROM payment) x "
            "WHERE $__timeFilter(t) GROUP BY 1, 2 ORDER BY 1", fmt="time_series")],
       0, 24,
       overrides=[color("SUCCESS", "green"), color("UNKNOWN", "orange"), color("FAILED", "red")]),
)

dashboard = {
    "uid": "payment-reliability", "title": "결제 안정성 (payment-fault)", "tags": ["ticket-on", "payment"],
    "timezone": "browser", "schemaVersion": 39, "refresh": "5s",
    "time": {"from": "now-15m", "to": "now"}, "panels": panels,
}
json.dump(dashboard, open(sys.argv[1], "w"), ensure_ascii=False, indent=2)
