package com.ticketon.ticketon.domain.eventitem.controller;

import com.ticketon.ticketon.domain.eventitem.entity.dto.EventItemResponse;
import com.ticketon.ticketon.domain.eventitem.service.EventItemService;
import com.ticketon.ticketon.global.constants.Urls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventItemAdminController {

    private final EventItemService eventItemService;

    @GetMapping(Urls.ADMIN_EVENTS)
    public String manageEventPage(Model model) {
        List<EventItemResponse> eventItems = eventItemService.getEventItemList();
        model.addAttribute("eventItems", eventItems);

        return "/eventitem/test/admin/eventitemList";
    }

    @GetMapping(Urls.ADMIN_EVENT_MANAGE)
    public String manageEventListPage(@PathVariable Long id, Model model) {
        EventItemResponse eventItem = eventItemService.getEventItemById(id);
        model.addAttribute("eventItem", eventItem);

        return "/eventitem/test/admin/eventItemManage";
    }

}