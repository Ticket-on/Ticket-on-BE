package com.ticketon.ticketon.member.business;

import com.ticketon.ticketon.member.implement.MemberWriter;
import com.ticketon.ticketon.member.vo.NewMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberWriter memberWriter;

    public void signUp(NewMember newMember) {
        memberWriter.register(newMember);
    }
}
