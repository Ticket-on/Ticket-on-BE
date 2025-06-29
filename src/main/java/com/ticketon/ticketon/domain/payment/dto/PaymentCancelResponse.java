package com.ticketon.ticketon.domain.payment.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class PaymentCancelResponse {

    // 취소 시간이 토스 응답 객체 리스트 안에 있어서 끄집어 내야함
    @JsonProperty("cancels")
    private List<CancelDetail> cancels;

    public OffsetDateTime getCanceledAt() {
        if (cancels != null && !cancels.isEmpty()) {
            return cancels.get(0).getCanceledAt(); // 첫 번째 취소 기준
        }
        return null;
    }


}
