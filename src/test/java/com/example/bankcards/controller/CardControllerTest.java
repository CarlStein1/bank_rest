package com.example.bankcards.controller;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    private static final LocalDate EXPIRATION_DATE =
            LocalDate.of(2029, 7, 28);

    @Mock
    private CardService cardService;

    @Mock
    private UserPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CardController cardController =
                new CardController(cardService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(cardController)
                .setCustomArgumentResolvers(
                        authenticationPrincipalResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    // -------------------------------------------------------------------------
    // Операции администратора
    // -------------------------------------------------------------------------

    @Test
    void createCard_shouldReturnCreatedAndCallService()
            throws Exception {

        CardResponse response = activeCardResponse();

        when(cardService.createCard(USER_ID))
                .thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/admin/users/{userId}/cards",
                                USER_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$").isMap());

        verify(cardService).createCard(USER_ID);
    }

    @Test
    void getAllCards_shouldReturnOkAndPageOfCards()
            throws Exception {

        CardResponse card = activeCardResponse();

        Pageable servicePageable =
                PageRequest.of(0, 10);

        Page<CardResponse> page = new PageImpl<>(
                List.of(card),
                servicePageable,
                1
        );

        when(cardService.getAllCards(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/admin/cards")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "id,desc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0]").isMap());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cardService).getAllCards(
                pageableCaptor.capture()
        );

        Pageable capturedPageable =
                pageableCaptor.getValue();

        assertEquals(
                0,
                capturedPageable.getPageNumber()
        );

        assertEquals(
                10,
                capturedPageable.getPageSize()
        );

        Sort.Order idOrder =
                capturedPageable
                        .getSort()
                        .getOrderFor("id");

        assertNotNull(idOrder);

        assertEquals(
                Sort.Direction.DESC,
                idOrder.getDirection()
        );
    }

    @Test
    void getCardById_shouldReturnOkAndCard()
            throws Exception {

        CardResponse response = activeCardResponse();

        when(cardService.getCardById(CARD_ID))
                .thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/admin/cards/{cardId}",
                                CARD_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$").isMap());

        verify(cardService).getCardById(CARD_ID);
    }

    @Test
    void blockCard_shouldReturnOkAndBlockedCard()
            throws Exception {

        CardResponse response = blockedCardResponse();

        when(cardService.blockCard(CARD_ID))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/admin/cards/{cardId}/block",
                                CARD_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$").isMap());

        verify(cardService).blockCard(CARD_ID);
    }

    @Test
    void activateCard_shouldReturnOkAndActiveCard()
            throws Exception {

        CardResponse response = activeCardResponse();

        when(cardService.activateCard(CARD_ID))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/admin/cards/{cardId}/activate",
                                CARD_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$").isMap());

        verify(cardService).activateCard(CARD_ID);
    }

    @Test
    void deleteCard_shouldReturnNoContentAndCallService()
            throws Exception {

        doNothing()
                .when(cardService)
                .deleteCard(CARD_ID);

        mockMvc.perform(
                        delete(
                                "/api/admin/cards/{cardId}",
                                CARD_ID
                        )
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cardService).deleteCard(CARD_ID);
    }

    // -------------------------------------------------------------------------
    // Операции пользователя
    // -------------------------------------------------------------------------

    @Test
    void getMyCards_shouldReturnOkAndPassPrincipalIdToService()
            throws Exception {

        CardResponse card = activeCardResponse();

        Pageable servicePageable =
                PageRequest.of(0, 5);

        Page<CardResponse> page = new PageImpl<>(
                List.of(card),
                servicePageable,
                1
        );

        when(principal.getId())
                .thenReturn(USER_ID);

        when(cardService.getUserCards(
                eq(USER_ID),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/cards")
                                .param("page", "0")
                                .param("size", "5")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0]").isMap());

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(cardService).getUserCards(
                eq(USER_ID),
                pageableCaptor.capture()
        );

        Pageable capturedPageable =
                pageableCaptor.getValue();

        assertEquals(
                0,
                capturedPageable.getPageNumber()
        );

        assertEquals(
                5,
                capturedPageable.getPageSize()
        );

        verify(principal).getId();
    }

    @Test
    void getMyCard_shouldReturnOkAndPassPrincipalIdToService()
            throws Exception {

        CardResponse response = activeCardResponse();

        when(principal.getId())
                .thenReturn(USER_ID);

        when(cardService.getUserCard(
                USER_ID,
                CARD_ID
        )).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/cards/{cardId}",
                                CARD_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$").isMap());

        verify(cardService).getUserCard(
                USER_ID,
                CARD_ID
        );
    }

    @Test
    void getMyCardBalance_shouldReturnOkAndBalance()
            throws Exception {

        BigDecimal balance =
                new BigDecimal("1250.50");

        when(principal.getId())
                .thenReturn(USER_ID);

        when(cardService.getUserCardBalance(
                USER_ID,
                CARD_ID
        )).thenReturn(balance);

        mockMvc.perform(
                        get(
                                "/api/cards/{cardId}/balance",
                                CARD_ID
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("1250.50"));

        verify(cardService).getUserCardBalance(
                USER_ID,
                CARD_ID
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

    private CardResponse activeCardResponse() {
        return new CardResponse(
                CARD_ID,
                USER_ID,
                "**** **** **** 3456",
                EXPIRATION_DATE,
                CardStatus.ACTIVE,
                new BigDecimal("1250.50")
        );
    }

    private CardResponse blockedCardResponse() {
        return new CardResponse(
                CARD_ID,
                USER_ID,
                "**** **** **** 3456",
                EXPIRATION_DATE,
                CardStatus.BLOCKED,
                new BigDecimal("1250.50")
        );
    }
}