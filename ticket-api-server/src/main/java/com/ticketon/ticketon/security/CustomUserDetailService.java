package com.ticketon.ticketon.security;

import com.ticketon.ticketon.member.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailService implements UserDetailsService {

    private final MemberReader memberReader;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberReader.findCredentialByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));
    }
}
