package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long FROM_CARD_ID = 10L;
    private static final Long TO_CARD_ID = 20L;

    @Mock
    private TransferService transferService;

    @Mock
    private UserPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransferController transferController =
                new TransferController(transferService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                .setCustomArgumentResolvers(
                        authenticationPrincipalResolver()
                )
                .build();
    }

    @Test
    void transfer_shouldReturnOkAndPassDataToService_whenRequestIsValid()
            throws Exception {

        // Arrange
        when(principal.getId())
                .thenReturn(USER_ID);

        when(transferService.transfer(
                eq(USER_ID),
                any(TransferRequest.class)
        )).thenReturn(null);

        String requestBody = """
                {
                  "fromCardId": 10,
                  "toCardId": 20,
                  "amount": 500.00
                }
                """;

        // Act and Assert
        mockMvc.perform(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        ArgumentCaptor<TransferRequest> requestCaptor =
                ArgumentCaptor.forClass(TransferRequest.class);

        verify(transferService).transfer(
                eq(USER_ID),
                requestCaptor.capture()
        );

        TransferRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals(
                FROM_CARD_ID,
                capturedRequest.fromCardId()
        );

        assertEquals(
                TO_CARD_ID,
                capturedRequest.toCardId()
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(capturedRequest.amount())
        );

        verify(principal).getId();
    }

    @Test
    void transfer_shouldReturnBadRequest_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
        verifyNoInteractions(principal);
    }

    @Test
    void transfer_shouldReturnBadRequest_whenJsonIsMalformed()
            throws Exception {

        String malformedJson = """
                {
                  "fromCardId": 10,
                  "toCardId": 20,
                  "amount":
                }
                """;

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedJson)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
        verifyNoInteractions(principal);
    }

    @Test
    void transfer_shouldReturnUnsupportedMediaType_whenContentTypeIsNotJson()
            throws Exception {

        String requestBody = """
                {
                  "fromCardId": 10,
                  "toCardId": 20,
                  "amount": 500.00
                }
                """;

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(transferService);
        verifyNoInteractions(principal);
    }

    @Test
    void transfer_shouldReturnMethodNotAllowed_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(
                        get("/api/transfers")
                )
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(transferService);
        verifyNoInteractions(principal);
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