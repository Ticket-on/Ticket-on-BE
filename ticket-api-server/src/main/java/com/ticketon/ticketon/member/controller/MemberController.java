package com.ticketon.ticketon.member.controller;

import com.ticket.support.response.ApiResponse;
import com.ticketon.ticketon.member.business.MemberService;
import com.ticketon.ticketon.member.controller.request.MemberSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody MemberSignUpRequest request) {
        memberService.signUp(request.toNewMember());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
