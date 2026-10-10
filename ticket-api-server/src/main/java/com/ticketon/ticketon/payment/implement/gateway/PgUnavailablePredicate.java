package com.ticketon.ticketon.payment.implement.gateway;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;

import java.util.function.Predicate;

public class PgUnavailablePredicate implements Predicate<Throwable> {

    /**
     * 서킷 브레이커에서 '해당 예외를 카운트할지?' 를 판단해주는 함수형 인터페이스 (참/거짓 판단)
     * true : 실패로 집계
     * false : 실패로 집계 X
     */
    @Override
    public boolean test(Throwable throwable) {
        return throwable instanceof AppException e && e.getErrorType() == ErrorType.PG_UNAVAILABLE;
    }
}
