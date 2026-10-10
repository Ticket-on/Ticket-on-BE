# 아키텍처 규칙 (ticket-api-server)

## 계층 구조 (순방향 참조만 허용)

```
진입점(Controller · Consumer · Scheduler) → Business(Service) → Implement → DataAccess(JPA · QueryDSL · JDBC · Redis)
```

- Business 는 Repository 를 직접 참조하지 않는다. 레이어를 건너뛰지 않는다.
- Implement 끼리의 협력은 허용한다 (예: Manager 가 Reader + Writer 호출, 다른 도메인의 Reader 호출).
- Kafka 컨슈머와 스케줄러는 Controller 와 같은 **진입점**이다. 로직 없이 Business 를 호출한다.
  Implement 가 Business 를 호출하는 역방향 참조를 만들지 않기 위해서다.

## 최상위 패키지

| 패키지 | 설명 |
|--------|------|
| `member` | 회원 가입, 로그인용 회원 조회 |
| `eventitem` | 공연 조회 |
| `ticket` | 티켓 예매(재고 선점) · 배치 발급 · 만료 · 취소 |
| `payment` | 결제 승인 · 취소, PG 연동, 아웃박스, 결제 완료 처리 |
| `waitingqueue` | 대기열 화면 |
| `security` | Spring Security 연동 (`CustomUserDetails`, `@CurrentUser`) |
| `common` | `ApiControllerAdvice`, 상수(`Urls`), 유틸 |
| `support` | QueryDSL 리포지토리 지원 클래스 |
| `config` | Security, Redis, Kafka, RestClient 설정 |

예외·응답 타입(`AppException`, `ErrorType`, `ErrorCode`, `ApiResponse`)은 queue-server 와 함께 쓰므로
**ticket-common** 의 `com.ticket.support` 에 둔다.

## 도메인 내부 패키지 구조

```
{domain}/
├── controller/     # REST·View Controller
│   ├── request/    # 요청 record. toXxx() 로 vo 변환
│   └── response/   # 응답 record. from(vo) 로 생성
├── consumer/       # Kafka 리스너 (진입점)
├── scheduler/      # 스케줄러 (진입점)
├── business/       # Service — 유즈케이스 오케스트레이션
├── implement/      # Reader, Writer, Manager, Validator, ...
├── dataaccess/
│   ├── entity/
│   └── repository/ # JPA, QueryDSL(custom/), JDBC, Redis
└── vo/             # 불변 record — 레이어 사이를 흐르는 도메인 객체
```

`implement/` 의 클래스가 많아지면 관심사별 하위 패키지로 나눈다. 도메인 전체에서 쓰는 클래스만 `implement/` 바로 아래 둔다.

```
ticket/implement/
├── issue/      # TicketIssueStrategy 와 구현체(Redis Lua, 비관락, 원자적 UPDATE)
├── stock/      # TicketStockManager, TicketStockInitializer
└── TicketReader.java, TicketWriter.java, TicketManager.java, ...   # 공용

payment/implement/
├── gateway/    # PaymentGateway, TossPaymentGateway, TossErrorClassifier, dto/(토스 요청·응답)
└── outbox/     # OutboxWriter
```

## POJO · VO 규칙

- `vo/` 는 해당 도메인의 개념을 담는 **불변 record** 다. Spring·JPA 에 의존하지 않는다.
- entity → vo 변환은 vo 의 `from(entity)` 에서 한다. Reader 가 변환해서 반환한다.
- 입력 불변식은 vo 의 compact constructor 에서 검증한다 (예: `TicketOrder`, `PaymentConfirmation`).
- 판단 로직은 가능한 한 vo·entity 같은 순수 객체에 둔다 (예: `StockDecreaseResult.from(luaResult)`, `Ticket.cancel()`).
- Controller 는 entity 를 다루지 않는다. request → vo → (business) → vo → response 로만 흐른다.
- Kafka·아웃박스·Redis 에 JSON 으로 저장되는 record(`NewTicketEvent`, `PaymentMessage`, `PendingTicket`)는
  필드 이름이 곧 메시지 스키마다. 이름을 바꾸면 이미 쌓인 메시지를 읽지 못한다.

## Implement 계층 역할 분리

| 역할 | 사용 시점 | 예시 |
|------|----------|------|
| **Reader** | 조회 전용 | `TicketReader`, `MemberReader`, `PaymentReader` |
| **Writer** | 저장·수정·삭제 | `TicketWriter`, `MemberWriter`, `OutboxWriter` |
| **Manager** | Reader + Writer 복합 오케스트레이션, `@Transactional` | `TicketManager`, `PaymentManager`, `TicketExpiryManager` |
| **Validator** | 비즈니스 규칙 검증, AppException throw | `TicketValidator` |

- 단순 조회만 → Reader, 단순 저장만 → Writer
- 조회 + 저장을 하나의 트랜잭션으로 묶어야 하면 → Manager
- 입력값이 아니라 비즈니스 규칙(소유자, 중복, 한도) 검증 → Validator

**과분리 금지:** 같은 저장소를 다루는 Reader 와 Writer 가 각각 메서드 하나뿐이면 Manager 하나로 합친다
(예: Redis 결제 대기 티켓은 `PendingTicketManager` 하나).

**외부 호출과 트랜잭션:** PG 같은 외부 HTTP 호출은 DB 트랜잭션 밖에서 한다.
Business 에서 PG 를 호출한 뒤 Manager(`@Transactional`)로 상태를 반영한다 (`PaymentService.cancel` 참고).

## 예외 규칙

- 비즈니스 예외는 `throw new AppException(ErrorType.XXX)` 하나로 통일한다. 예외 클래스를 새로 만들지 않는다.
- 새 에러는 `ErrorType` 에 추가한다. HTTP status · `ErrorCode` · 메시지 · 로그 레벨을 한 곳에서 정한다.
- `ErrorCode` 는 도메인별 번호 대역을 쓴다: Member `E1xxx`, EventItem `E2xxx`, Ticket `E3xxx`, Payment `E4xxx`, WaitingQueue `E5xxx`.
  한 번 배포된 코드는 의미를 바꾸거나 재사용하지 않는다. 쓰지 않게 되면 결번으로 남긴다.
- 원인 예외가 있으면 `new AppException(ErrorType.XXX, e)` 로 감싸 스택트레이스를 잃지 않는다.
- 외부 연동 실패도 `AppException` 으로 표현한다. PG 결과는 `ErrorType` 으로 구분한다.
  `PAYMENT_REJECTED`(승인 거절 확정), `PAYMENT_RESULT_UNKNOWN`(PG 는 응답했지만 승인 여부 불명), `PG_UNAVAILABLE`(경로 이상, 서킷 집계 대상).
  토스 원본 code·message 는 `data` 에 `PgError` 로 담는다 (클라이언트 응답에도 노출된다).
- `ApiControllerAdvice` 는 `@RestController` 에만 적용된다. View Controller 의 예외는 에러 페이지로 간다.

## 응답 규칙

REST API 는 항상 `ApiResponse` 로 감싼다.

```json
{ "resultType": "SUCCESS", "data": { "orderId": "01J..." }, "error": null }
{ "resultType": "ERROR", "data": null, "error": { "errorCode": "E3001", "message": "티켓이 매진되었습니다.", "data": null } }
```

```java
return ResponseEntity.ok(ApiResponse.success(response));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
```

## Lombok · 엔티티 규칙 (`lombok.config` 로 강제)

- `@Setter`, `@AllArgsConstructor`, `@Data` 금지.
- 엔티티는 `@NoArgsConstructor(access = PROTECTED)` + 필요한 필드만 받는 생성자에 `@Builder`.
  의미 있는 생성은 정적 팩토리로 한다 (예: `Ticket.createPending`, `Payment.success`).
- 설정 값은 setter 대신 `@ConfigurationProperties` record 로 받는다 (예: `PaymentProperties`).

## 주석

코드에 주석을 쓰지 않는다. 의도는 이름(클래스·메서드·vo)과 이 문서, 테스트의 `@DisplayName` 으로 드러낸다.

## 중첩 타입 금지

클래스·record·interface 안에 다른 클래스·record·enum·interface 를 선언하지 않는다. 모든 타입은 독립 파일로 분리한다.
