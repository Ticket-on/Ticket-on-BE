package com.ticketon.ticketon.ticket.vo;

public enum StockDecreaseResult {
    DECREASED,
    SOLD_OUT,
    NOT_INITIALIZED;

    public static StockDecreaseResult from(Long scriptResult) {
        if (scriptResult == null || scriptResult == -1L) {
            return NOT_INITIALIZED;
        }
        if (scriptResult == 0L) {
            return SOLD_OUT;
        }
        return DECREASED;
    }
}
