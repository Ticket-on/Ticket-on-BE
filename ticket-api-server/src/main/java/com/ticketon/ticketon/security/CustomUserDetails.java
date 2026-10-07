package com.ticketon.ticketon.security;

import com.ticketon.ticketon.member.vo.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final MemberCredential credential;

    public long getMemberId() {
        return credential.memberId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return credential.encodedPassword();
    }

    @Override
    public String getUsername() {
        return credential.email();
    }
}
