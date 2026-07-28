package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardBlockRequest;
import com.example.bankcards.dto.request.ProcessCardBlockRequest;
import com.example.bankcards.dto.response.CardBlockRequestResponse;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardBlockRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CardBlockRequestControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long CARD_ID = 10L;
    private static final Long REQUEST_ID = 20L;

    @Mock
    private CardBlockRequestService cardBlockRequestService;

    @Mock
    private UserPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CardBlockRequestController controller =
                new CardBlockRequestController(
                        cardBlockRequestService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        authenticationPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setMessageConverters(
                        new JacksonJsonHttpMessageConverter()
                )
                .build();
    }

    @Test
    void createBlockRequest_shouldReturnCreatedAndPassDataToService()
            throws Exception {

        when(principal.getId())
                .thenReturn(USER_ID);

        when(cardBlockRequestService.createBlockRequest(
                eq(USER_ID),
                eq(CARD_ID),
                any(CreateCardBlockRequest.class)
        )).thenReturn(response(
                CardBlockRequestStatus.PENDING,
                null
        ));

        String requestBody = """
                {
                  "reason": "Карта потеряна"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/cards/{cardId}/block-requests",
                                CARD_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(REQUEST_ID))
                .andExpect(jsonPath("$.cardId").value(CARD_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("Карта потеряна"));

        ArgumentCaptor<CreateCardBlockRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateCardBlockRequest.class
                );

        verify(cardBlockRequestService).createBlockRequest(
                eq(USER_ID),
                eq(CARD_ID),
                requestCaptor.capture()
        );

        assertEquals(
                "Карта потеряна",
                requestCaptor.getValue().reason()
        );

        verify(principal).getId();
    }

    @Test
    void createBlockRequest_shouldReturnBadRequest_whenBodyIsMissing()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/cards/{cardId}/block-requests",
                                CARD_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardBlockRequestService);
        verifyNoInteractions(principal);
    }

    @Test
    void getAllBlockRequests_shouldReturnOk_whenStatusIsMissing()
            throws Exception {

        when(cardBlockRequestService.getAllBlockRequests(
                isNull(),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(
                                response(
                                        CardBlockRequestStatus.PENDING,
                                        null
                                )
                        )
                )
        );

        mockMvc.perform(
                        get("/api/admin/block-requests")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "id,desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(REQUEST_ID));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cardBlockRequestService)
                .getAllBlockRequests(
                        isNull(),
                        pageableCaptor.capture()
                );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(
                "DESC",
                pageable.getSort()
                        .getOrderFor("id")
                        .getDirection()
                        .name()
        );
    }

    @Test
    void getAllBlockRequests_shouldPassStatusToService()
            throws Exception {

        when(cardBlockRequestService.getAllBlockRequests(
                eq(CardBlockRequestStatus.PENDING),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(
                                response(
                                        CardBlockRequestStatus.PENDING,
                                        null
                                )
                        )
                )
        );

        mockMvc.perform(
                        get("/api/admin/block-requests")
                                .param("status", "PENDING")
                                .param("page", "1")
                                .param("size", "5")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status")
                        .value("PENDING"));

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cardBlockRequestService)
                .getAllBlockRequests(
                        eq(CardBlockRequestStatus.PENDING),
                        pageableCaptor.capture()
                );

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }

    @Test
    void getAllBlockRequests_shouldReturnBadRequest_whenStatusIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/block-requests")
                                .param("status", "UNKNOWN")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardBlockRequestService);
    }

    @Test
    void getBlockRequestById_shouldReturnOkAndCallService()
            throws Exception {

        when(cardBlockRequestService.getBlockRequestById(REQUEST_ID))
                .thenReturn(
                        response(
                                CardBlockRequestStatus.PENDING,
                                null
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/admin/block-requests/{requestId}",
                                REQUEST_ID
                        )
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REQUEST_ID))
                .andExpect(jsonPath("$.cardId").value(CARD_ID));

        verify(cardBlockRequestService)
                .getBlockRequestById(REQUEST_ID);
    }

    @Test
    void approveBlockRequest_shouldReturnOkAndPassDataToService()
            throws Exception {

        when(principal.getId())
                .thenReturn(ADMIN_ID);

        when(cardBlockRequestService.approveBlockRequest(
                eq(ADMIN_ID),
                eq(REQUEST_ID),
                any(ProcessCardBlockRequest.class)
        )).thenReturn(
                response(
                        CardBlockRequestStatus.APPROVED,
                        ADMIN_ID
                )
        );

        String requestBody = """
                {
                  "adminComment": "Заявка подтверждена"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/admin/block-requests/{requestId}/approve",
                                REQUEST_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.processedById").value(ADMIN_ID));

        ArgumentCaptor<ProcessCardBlockRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ProcessCardBlockRequest.class
                );

        verify(cardBlockRequestService)
                .approveBlockRequest(
                        eq(ADMIN_ID),
                        eq(REQUEST_ID),
                        requestCaptor.capture()
                );

        assertEquals(
                "Заявка подтверждена",
                requestCaptor.getValue().adminComment()
        );

        verify(principal).getId();
    }

    @Test
    void approveBlockRequest_shouldReturnBadRequest_whenBodyIsMissing()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/admin/block-requests/{requestId}/approve",
                                REQUEST_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardBlockRequestService);
        verifyNoInteractions(principal);
    }

    @Test
    void rejectBlockRequest_shouldReturnOkAndPassDataToService()
            throws Exception {

        when(principal.getId())
                .thenReturn(ADMIN_ID);

        when(cardBlockRequestService.rejectBlockRequest(
                eq(ADMIN_ID),
                eq(REQUEST_ID),
                any(ProcessCardBlockRequest.class)
        )).thenReturn(
                response(
                        CardBlockRequestStatus.REJECTED,
                        ADMIN_ID
                )
        );

        String requestBody = """
                {
                  "adminComment": "Недостаточно информации"
                }
                """;

        mockMvc.perform(
                        patch(
                                "/api/admin/block-requests/{requestId}/reject",
                                REQUEST_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.processedById").value(ADMIN_ID));

        ArgumentCaptor<ProcessCardBlockRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ProcessCardBlockRequest.class
                );

        verify(cardBlockRequestService)
                .rejectBlockRequest(
                        eq(ADMIN_ID),
                        eq(REQUEST_ID),
                        requestCaptor.capture()
                );

        assertEquals(
                "Недостаточно информации",
                requestCaptor.getValue().adminComment()
        );

        verify(principal).getId();
    }

    @Test
    void rejectBlockRequest_shouldReturnBadRequest_whenBodyIsMissing()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/admin/block-requests/{requestId}/reject",
                                REQUEST_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardBlockRequestService);
        verifyNoInteractions(principal);
    }

    private CardBlockRequestResponse response(
            CardBlockRequestStatus status,
            Long processedById
    ) {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 7, 28, 12, 0);

        LocalDateTime processedAt =
                processedById == null
                        ? null
                        : LocalDateTime.of(2026, 7, 28, 12, 30);

        String adminComment =
                processedById == null
                        ? null
                        : status == CardBlockRequestStatus.APPROVED
                          ? "Заявка подтверждена"
                          : "Недостаточно информации";

        return new CardBlockRequestResponse(
                REQUEST_ID,
                CARD_ID,
                status,
                "Карта потеряна",
                createdAt,
                processedAt,
                processedById,
                adminComment
        );
    }

    private HandlerMethodArgumentResolver
    authenticationPrincipalResolver() {

        return new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(
                    MethodParameter parameter
            ) {
                return parameter.hasParameterAnnotation(
                        AuthenticationPrincipal.class
                ) && UserPrincipal.class.isAssignableFrom(
                        parameter.getParameterType()
                );
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return principal;
            }
        };
    }
}
