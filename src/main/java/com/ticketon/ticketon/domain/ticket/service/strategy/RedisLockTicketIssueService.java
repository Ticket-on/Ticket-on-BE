package com.ticketon.ticketon.domain.ticket.service.strategy;

import com.ticketon.ticketon.domain.member.entity.Member;
import com.ticketon.ticketon.domain.member.repository.MemberRepository;
import com.ticketon.ticketon.domain.ticket.entity.Ticket;
import com.ticketon.ticketon.domain.ticket.entity.TicketType;
import com.ticketon.ticketon.domain.ticket.entity.dto.TicketPurchaseRequest;
import com.ticketon.ticketon.domain.ticket.repository.TicketRepository;
import com.ticketon.ticketon.domain.ticket.repository.TicketTypeRepository;
import com.ticketon.ticketon.exception.custom.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service("redis")
@Qualifier("redis")
@RequiredArgsConstructor
public class RedisLockTicketIssueService implements TicketIssueStrategy{

    private final RedissonClient redissonClient;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final MemberRepository memberRepository;

    // !! 레디스 유틸 클래스 생성 후 대체 필요 !!
    @Transactional
    @Override
    public void purchaseTicket(TicketPurchaseRequest request, Long memberId) {
        String lockName = "ticketTypeLock" + request.getTicketTypeId();
        RLock lock = redissonClient.getLock(lockName);

        try {
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                Long ticketTypeId = request.getTicketTypeId();
                TicketType ticketType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
                        .orElseThrow(() -> new NotFoundDataException("티켓 타입 없음 (ticket_id=" + ticketTypeId + ")" ));

                // 쿼리 날리지 않고 프록시로 조회
                Member member = memberRepository.getReferenceById(memberId);

                ticketType.increaseIssuedQuantity();

                Ticket ticket = Ticket.createNormalTicket(ticketType, member);
                ticketRepository.save(ticket);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } finally {
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
