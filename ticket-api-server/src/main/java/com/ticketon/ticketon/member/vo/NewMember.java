package com.ticketon.ticketon.member.vo;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;

public record NewMember(String email, String rawPassword) {

    public NewMember {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AppException(ErrorType.INVALID_REQUEST);
        }
    }
}
