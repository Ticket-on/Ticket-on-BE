package com.ticketon.ticketon.eventitem.controller;

import com.ticketon.ticketon.common.constants.Urls;
import com.ticketon.ticketon.eventitem.business.EventItemService;
import com.ticketon.ticketon.eventitem.controller.response.EventItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class EventItemViewController {

    private final EventItemService eventItemService;

    @GetMapping(Urls.EVENTS)
    public String eventItemList(Model model) {
        List<EventItemResponse> eventItems = eventItemService.getEventItems()
                .stream()
                .map(EventItemResponse::from)
                .toList();
        model.addAttribute("eventItems", eventItems);
        return "eventitem/test/eventItemList";
    }

    @GetMapping(Urls.EVENT_DETAIL)
    public String eventItemDetail(@PathVariable Long id, Model model) {
        model.addAttribute("eventItem", EventItemResponse.from(eventItemService.getEventItem(id)));
        return "eventitem/test/eventItem";
    }
}
