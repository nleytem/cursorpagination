package com.nmleytem.api;

import com.nmleytem.dto.EventPageResponse;
import com.nmleytem.dto.Pagination;
import com.nmleytem.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getEvents_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Arrange
        EventPageResponse response = new EventPageResponse(Collections.emptyList(), new Pagination(null, 0L));
        when(eventService.getEvents(anyLong(), anyLong(), anyInt(), any(), any(), any(), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/events")
                        .param("startTime", "1000")
                        .param("endTime", "2000")
                        .param("limit", "10")
                        .param("category", "music")
                        .param("city", "New+York")
                        .param("tags", "rock,pop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.pagination.total_count").value(0));
    }

    @Test
    void getEvents_ShouldReturnError_WhenRequiredParamsAreMissing() throws Exception {
        // Act & Assert
        // startTime and endTime are required by EventRequest.validate()
        // Note: standaloneSetup might not handle exceptions as Spring Boot does by default, 
        // so we might need to verify the exception or its mapping.
        mockMvc.perform(get("/events"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_ShouldReturnError_WhenStartTimeIsAfterEndTime() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/events")
                        .param("startTime", "2000")
                        .param("endTime", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_ShouldReturnError_WhenCursorIsInvalid() throws Exception {
        // Arrange
        when(eventService.getEvents(anyLong(), anyLong(), anyInt(), eq("invalid-cursor"), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid cursor"));

        // Act & Assert
        mockMvc.perform(get("/events")
                        .param("startTime", "1000")
                        .param("endTime", "2000")
                        .param("limit", "10")
                        .param("cursor", "invalid-cursor"))
                .andExpect(status().isBadRequest());
    }
}
