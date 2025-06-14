package com.ticketon.ticketon.ticket;

import com.ticketon.ticketon.domain.ticket.entity.dto.TicketPurchaseRequest;
import com.ticketon.ticketon.domain.ticket.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class TicketIssueConcurrencyTest {

    @Autowired
    private TicketService ticketService;

    private final int THREAD_COUNT = 50;

    private void runConcurrencyPurchase(String strategyType) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // 티켓 구매 요청 DTO, memberId는 테스트용으로 고정하거나 변경 가능
                    TicketPurchaseRequest request = new TicketPurchaseRequest();
                    request.setTicketTypeId(1L);
                    request.setQuantity(1);

                    Long testMemberId = 1L;

                    ticketService.purchaseTicket(strategyType, request, testMemberId);
                } catch (Exception e) {
                    e.printStackTrace();  // 실패한 요청 로그 확인
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 검증 메소드
    }



}
