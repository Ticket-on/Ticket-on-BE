# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 모듈

| 모듈 | 설명 |
|------|------|
| `ticket-api-server` | 티켓 예매·결제 API (Spring MVC, JPA, Redis, Kafka) |
| `ticket-queue-server` | 대기열 서버 (WebFlux, Reactor Kafka, Redis) |
| `ticket-common` | 두 서버가 함께 쓰는 예외·응답 타입 (`com.ticket.support`), 상수 |

## 코드 아키텍처 규칙

계층 구조, 패키지 구조, POJO·VO, 예외·응답 규칙은 `.claude/rules/architecture.md` 를 따른다.

## 빌드 및 실행 명령어

```bash
docker compose -f docker-compose-local.yml up -d   # 로컬 인프라
./gradlew build
./gradlew :ticket-api-server:test                  # 단위 테스트 (integration 태그 제외)
./gradlew :ticket-api-server:integrationTest       # 통합 테스트 (Testcontainers MySQL + localhost:6379 Redis 필요)
./gradlew :ticket-api-server:test --tests "com.ticketon.ticketon.XxxTest"
./gradlew clean :ticket-api-server:compileJava     # QueryDSL Q클래스 재생성
```

## 로깅 규칙

로그 메시지는 `[카테고리 : 상세내용]: key=value | key=value` 형식으로 작성한다.

```java
log.info("[티켓 예매 : 재고 선점 완료]: orderId={} | ticketTypeId={}", orderId, ticketTypeId);
log.warn("[토스 결제 승인 : 오류 응답]: orderId={} | status={}", orderId, status);
log.error("[티켓 만료 : 재고 복구 실패]: ticketId={} | 수동 확인 필요", ticketId, e);
```

- 카테고리: 도메인 또는 컴포넌트명 (예: `티켓 예매`, `결제 컨슈머`, `토스 결제 승인`)
- 상세내용: 동작 결과 (예: `저장 완료`, `발행 실패`)
- 레벨: 정상 흐름 → `info`, 비즈니스 예외 → `warn`, 시스템 오류 → `error`
- 개인정보(이메일 원문, DB 제약 위반 메시지 원문)는 로그에 남기지 않는다.

## 커밋 메시지

`feat : ...`, `fix : ...`, `refactor : ...`, `chore : ...` 형식(콜론 앞뒤 공백)을 쓴다.
