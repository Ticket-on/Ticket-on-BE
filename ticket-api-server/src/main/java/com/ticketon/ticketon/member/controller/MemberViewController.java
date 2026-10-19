package com.ticketon.ticketon.member.controller;

import com.ticketon.ticketon.common.constants.Urls;
import com.ticketon.ticketon.member.business.MemberService;
import com.ticketon.ticketon.member.controller.request.MemberSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class MemberViewController {

    private final MemberService memberService;

    @PostMapping(value = Urls.SIGN_UP, consumes = "application/json")
    public String signupJson(@RequestBody MemberSignUpRequest request) {
        memberService.signUp(request.toNewMember());
        return "redirect:/login";
    }

    @PostMapping(value = Urls.SIGN_UP, consumes = "application/x-www-form-urlencoded")
    public String signupForm(@ModelAttribute MemberSignUpRequest request) {
        memberService.signUp(request.toNewMember());
        return "redirect:/login";
    }

    @GetMapping(Urls.SIGN_UP)
    public String signupPage() {
        return "login/signup";
    }
}
