package com.ticketon.ticketon.common.controller;

import com.ticket.support.exception.AppException;
import com.ticket.support.exception.ErrorType;
import com.ticketon.ticketon.ticket.controller.request.TicketPurchaseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdviceTestController {

    @GetMapping("/test/app-exception")
    public void appException() {
        throw new AppException(ErrorType.TICKET_SOLD_OUT);
    }

    @GetMapping("/test/unknown")
    public void unknown() {
        throw new IllegalStateException("boom");
    }

    @PostMapping("/test/validation")
    public void validation(@Valid @RequestBody TicketPurchaseRequest request) {
    }
}
