package com.ticketon.ticketon.ticket.implement.issue;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItem;
import com.ticketon.ticketon.eventitem.dataaccess.entity.EventItemStatus;
import com.ticketon.ticketon.eventitem.dataaccess.repository.EventItemRepository;
import com.ticketon.ticketon.integration.E2ETest;
import com.ticketon.ticketon.member.dataaccess.entity.Member;
import com.ticketon.ticketon.member.dataaccess.repository.MemberRepository;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketType;
import com.ticketon.ticketon.ticket.dataaccess.entity.TicketTypeStatus;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketRepository;
import com.ticketon.ticketon.ticket.dataaccess.repository.TicketTypeRepository;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import de.huxhorn.sulky.ulid.ULID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TicketIssueStrategyTest extends E2ETest {

    private static final int THREAD_COUNT = 32;

    @Autowired
    private PessimisticLockTicketIssueStrategy pessimisticLockStrategy;

    @Autowired
    private AtomicDbTicketIssueStrategy atomicDbStrategy;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EventItemRepository eventItemRepository;

    private Long ticketTypeId;
    private List<Long> memberIds;

    @BeforeEach
    void setUp() {
        EventItem eventItem = eventItemRepository.save(EventItem.builder()
                .title("흠뻑쇼")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .eventItemStatus(EventItemStatus.OPEN)
                .build());

        ticketTypeId = ticketTypeRepository.save(TicketType.builder()
                .eventItem(eventItem)
                .name("티켓")
                .issuedQuantity(0L)
                .maxQuantity(1L)
                .price(1000)
                .status(TicketTypeStatus.READY)
                .build()).getId();

        memberIds = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> memberRepository.save(Member.builder()
                        .email("user" + i + "@test.com")
                        .password("password")
                        .build()).getId())
                .toList();
    }

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
        ticketTypeRepository.deleteAllInBatch();
        eventItemRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("비관락: 32명이 재고 1장에 동시에 접근해도 1명만 발급되고 나머지는 매진으로 실패한다")
    void pessimisticLock() throws InterruptedException {
        assertOnlyOneIssued(pessimisticLockStrategy);
    }

    @Test
    @DisplayName("원자적 UPDATE: 32명이 재고 1장에 동시에 접근해도 1명만 발급되고 나머지는 매진으로 실패한다")
    void atomicUpdate() throws InterruptedException {
        assertOnlyOneIssued(atomicDbStrategy);
    }

    private void assertOnlyOneIssued(TicketIssueStrategy strategy) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger soldOutCount = new AtomicInteger();
        Queue<Exception> unexpected = new ConcurrentLinkedQueue<>();

        for (Long memberId : memberIds) {
            executorService.submit(() -> {
                try {
                    strategy.issue(new TicketOrder(ticketTypeId, memberId, 1000), new ULID().nextULID());
                    successCount.incrementAndGet();
                } catch (AppException e) {
                    if (e.getErrorType() == ErrorType.TICKET_SOLD_OUT) {
                        soldOutCount.incrementAndGet();
                    } else {
                        unexpected.add(e);
                    }
                } catch (Exception e) {
                    unexpected.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        assertThat(unexpected).isEmpty();
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId).orElseThrow();
        assertThat(ticketType.getIssuedQuantity()).isEqualTo(1L);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(soldOutCount.get()).isEqualTo(THREAD_COUNT - 1);
    }
}
