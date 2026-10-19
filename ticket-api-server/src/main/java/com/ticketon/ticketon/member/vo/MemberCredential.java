package com.ticketon.ticketon.member.vo;

import com.ticketon.ticketon.member.dataaccess.entity.Member;

public record MemberCredential(long memberId, String email, String encodedPassword) {

    public static MemberCredential from(Member member) {
        return new MemberCredential(member.getId(), member.getEmail(), member.getPassword());
    }
}
