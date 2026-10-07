package com.ticketon.ticketon.ticket.controller;

import com.ticket.support.response.ApiResponse;
import com.ticketon.ticketon.ticket.business.TicketService;
import com.ticketon.ticketon.ticket.controller.request.TicketPurchaseRequest;
import com.ticketon.ticketon.ticket.controller.response.TicketPurchaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseTicket(@Valid @RequestBody TicketPurchaseRequest request) {
        TicketPurchaseResponse response = TicketPurchaseResponse.from(ticketService.reserve(request.toOrder()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expire")
    public ResponseEntity<ApiResponse<Void>> triggerExpiry() {
        ticketService.expirePendingTickets();
        return ResponseEntity.ok(ApiResponse.success());
    }
}
