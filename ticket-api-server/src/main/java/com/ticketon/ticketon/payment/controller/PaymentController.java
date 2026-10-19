package com.ticketon.ticketon.payment.controller;

import com.ticket.support.response.ApiResponse;
import com.ticketon.ticketon.payment.business.PaymentService;
import com.ticketon.ticketon.payment.controller.request.PaymentCancelRequest;
import com.ticketon.ticketon.payment.controller.request.PaymentConfirmRequest;
import com.ticketon.ticketon.payment.controller.response.PaymentConfirmResponse;
import com.ticketon.ticketon.payment.controller.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {
        PaymentConfirmResponse response = PaymentConfirmResponse.from(paymentService.confirm(request.toConfirmation()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(@Valid @RequestBody PaymentCancelRequest request) {
        paymentService.cancel(request.toCancellation());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/info/{ticketId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTicketId(@PathVariable Long ticketId) {
        PaymentResponse response = PaymentResponse.from(paymentService.getByTicketId(ticketId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
