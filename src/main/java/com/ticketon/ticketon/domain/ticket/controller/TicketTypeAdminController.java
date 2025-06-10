package com.ticketon.ticketon.domain.ticket.controller;

import com.ticketon.ticketon.domain.ticket.entity.dto.TicketTypeCreateRequest;
import com.ticketon.ticketon.domain.ticket.service.TicketTypeService;
import com.ticketon.ticketon.global.constants.Urls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class TicketTypeAdminController {

    private final TicketTypeService ticketTypeService;

    @PostMapping(Urls.TICKET_TYPE_NEW_ID)
    public String addTicketType(@ModelAttribute TicketTypeCreateRequest request, @PathVariable Long id) {
        ticketTypeService.saveTicketType(request, id);
        return "redirect:/admin/events/" + id;
    }
}
