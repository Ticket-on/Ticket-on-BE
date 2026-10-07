package com.ticketon.ticketon.member.controller.request;

import com.ticketon.ticketon.member.vo.NewMember;
import jakarta.validation.constraints.NotBlank;

public record MemberSignUpRequest(
        @NotBlank String email,
        @NotBlank String password
) {
    public NewMember toNewMember() {
        return new NewMember(email, password);
    }
}
