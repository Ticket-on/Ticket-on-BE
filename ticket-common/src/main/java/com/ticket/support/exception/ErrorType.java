package com.ticket.support.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    REQUIRED_AUTH(HttpStatus.UNAUTHORIZED, ErrorCode.E401, "리소스에 접근하기 위한 인증이 필요합니다.", LogLevel.WARN),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN, ErrorCode.E403, "요청한 리소스에 접근할 권한이 없습니다.", LogLevel.WARN),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E400, "요청 값이 올바르지 않습니다.", LogLevel.WARN),
    NOT_FOUND_DATA(HttpStatus.NOT_FOUND, ErrorCode.E404, "해당 데이터를 찾을 수 없습니다.", LogLevel.WARN),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, ErrorCode.E409, "이미 등록된 정보입니다.", LogLevel.WARN),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.E429, "너무 많은 요청을 보냈습니다.", LogLevel.WARN),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, ErrorCode.E1000, "존재하지 않는 회원입니다.", LogLevel.WARN),

    NOT_FOUND_EVENT_ITEM(HttpStatus.NOT_FOUND, ErrorCode.E2000, "존재하지 않는 공연입니다.", LogLevel.WARN),

    NOT_FOUND_TICKET_TYPE(HttpStatus.NOT_FOUND, ErrorCode.E3000, "존재하지 않는 티켓 종류입니다.", LogLevel.WARN),
    TICKET_SOLD_OUT(HttpStatus.CONFLICT, ErrorCode.E3001, "티켓이 매진되었습니다.", LogLevel.INFO),
    NOT_FOUND_TICKET(HttpStatus.NOT_FOUND, ErrorCode.E3002, "존재하지 않는 티켓입니다.", LogLevel.WARN),
    NOT_TICKET_OWNER(HttpStatus.FORBIDDEN, ErrorCode.E3003, "본인의 티켓만 취소할 수 있습니다.", LogLevel.WARN),
    TICKET_ALREADY_CANCELLED(HttpStatus.CONFLICT, ErrorCode.E3004, "이미 취소된 티켓입니다.", LogLevel.WARN),
    NO_ISSUED_TICKET_TO_CANCEL(HttpStatus.CONFLICT, ErrorCode.E3005, "취소할 수 있는 발급 티켓이 없습니다.", LogLevel.WARN),
    TICKET_STOCK_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E3006, "티켓 재고 정보가 없습니다.", LogLevel.ERROR),
    PENDING_TICKET_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E3007, "예약 대기 티켓 정보를 처리하지 못했습니다.", LogLevel.ERROR),

    PAYMENT_RESERVATION_EXPIRED(HttpStatus.GONE, ErrorCode.E4000, "시간이 지나 만료된 요청입니다. 결제를 진행할 수 없습니다.", LogLevel.WARN),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, ErrorCode.E4001, "존재하지 않는 결제 정보입니다.", LogLevel.WARN),
    PAYMENT_REJECTED(HttpStatus.BAD_REQUEST, ErrorCode.E4002, "결제가 승인되지 않았습니다.", LogLevel.WARN),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, ErrorCode.E4003, "결제 취소 요청에 실패했습니다.", LogLevel.ERROR),
    OUTBOX_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E4004, "결제 메시지를 저장하지 못했습니다.", LogLevel.ERROR),
    PAYMENT_RESULT_UNKNOWN(HttpStatus.ACCEPTED, ErrorCode.E4005, "결제 결과를 확인하고 있습니다. 잠시 후 결제 내역을 확인해주세요.", LogLevel.WARN),
    PG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.E4006, "결제 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    WAITING_LINE_CONSUME_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E5000, "대기열 메시지를 처리하지 못했습니다.", LogLevel.ERROR),
    WAITING_LINE_FLUSH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E5001, "대기열을 Redis 에 반영하지 못했습니다.", LogLevel.ERROR);

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode errorCode, String message, LogLevel logLevel) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
    }
}
