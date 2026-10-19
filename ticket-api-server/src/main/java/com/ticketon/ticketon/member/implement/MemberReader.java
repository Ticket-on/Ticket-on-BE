package com.ticketon.ticketon.member.implement;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.dataaccess.repository.MemberRepository;
import com.ticketon.ticketon.member.vo.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_MEMBER));
    }

    public Map<Long, Member> findAllByIds(Collection<Long> memberIds) {
        return memberRepository.findAllById(memberIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    public Optional<MemberCredential> findCredentialByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(MemberCredential::from);
    }
}
