package com.ticketon.ticketon.member.implement;

import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.dataaccess.repository.MemberRepository;
import com.ticketon.ticketon.member.vo.NewMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberWriter {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(NewMember newMember) {
        memberRepository.save(Member.builder()
                .email(newMember.email())
                .password(passwordEncoder.encode(newMember.rawPassword()))
                .build());
    }
}
