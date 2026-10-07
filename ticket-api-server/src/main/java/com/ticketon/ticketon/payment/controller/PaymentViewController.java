package com.ticketon.ticketon.payment.controller;

import com.ticketon.ticketon.common.constants.Urls;
import com.ticketon.ticketon.eventitem.business.EventItemService;
import com.ticketon.ticketon.security.CurrentUser;
import com.ticketon.ticketon.security.CustomUserDetails;
import com.ticketon.ticketon.ticket.business.TicketService;
import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(Urls.PAYMENT)
@RequiredArgsConstructor
public class PaymentViewController {

    private final TicketService ticketService;
    private final EventItemService eventItemService;

    @Value("${toss.client-key}")
    private String clientKey;

    @GetMapping(Urls.PAYMENT_PREPARE)
    public String paymentRequest(@RequestParam Long ticketTypeId,
                                 @RequestParam @Max(TicketOrder.QUANTITY) int quantity,
                                 @CurrentUser CustomUserDetails userDetails, Model model) {
        TicketOrder order = ticketService.createOrder(userDetails.getMemberId(), ticketTypeId);
        TicketReservation reservation = ticketService.reserve(order);
        String eventTitle = eventItemService.getTitleByTicketTypeId(ticketTypeId);
        model.addAttribute("clientKey", clientKey);
        model.addAttribute("orderId", reservation.orderId());
        model.addAttribute("ticketTypeId", ticketTypeId);
        model.addAttribute("memberId", reservation.memberId());
        model.addAttribute("amount", reservation.price());
        model.addAttribute("orderName", eventTitle);
        return "payment/paymentConfirm";
    }

    @GetMapping("/success")
    public String confirmPayment(@RequestParam String paymentKey,
                                 @RequestParam String orderId,
                                 @RequestParam int amount,
                                 Model model) {
        model.addAttribute("paymentKey", paymentKey);
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        return "payment/paymentSuccess";
    }

    @GetMapping("/fail")
    public String failPayment(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String message,
                              Model model) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "payment/paymentFailure";
    }

    @GetMapping("/cancel")
    public String cancelPage(@RequestParam String paymentKey,
                             @RequestParam Integer amount,
                             Model model) {
        model.addAttribute("paymentKey", paymentKey);
        model.addAttribute("cancelAmount", amount);
        return "payment/paymentCancel";
    }
}
