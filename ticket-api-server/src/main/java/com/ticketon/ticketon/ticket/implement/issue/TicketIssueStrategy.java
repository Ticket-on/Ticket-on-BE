package com.ticketon.ticketon.ticket.implement.issue;

import com.ticketon.ticketon.ticket.vo.TicketOrder;
import com.ticketon.ticketon.ticket.vo.TicketReservation;

public interface TicketIssueStrategy {

    TicketReservation issue(TicketOrder order, String orderId);
}
