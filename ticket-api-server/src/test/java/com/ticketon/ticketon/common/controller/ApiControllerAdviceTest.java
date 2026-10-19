package com.ticketon.ticketon.common.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdviceTestController())
                .setControllerAdvice(new ApiControllerAdvice())
                .build();
    }

    @Test
    @DisplayName("AppException 은 ErrorType 의 status 와 errorCode 로 응답한다")
    void appException() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultType").value("ERROR"))
                .andExpect(jsonPath("$.error.errorCode").value("E3001"));
    }

    @Test
    @DisplayName("요청 본문 검증 실패는 400 INVALID_REQUEST")
    void validation() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("E400"));
    }

    @Test
    @DisplayName("처리하지 못한 예외는 500 DEFAULT_ERROR")
    void unknown() throws Exception {
        mockMvc.perform(get("/test/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.errorCode").value("E500"));
    }
}
