package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String LOGIN = "testuser";
    private static final String PASSWORD = "Password123!";

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController authController =
                new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    @Test
    void login_shouldReturnOkAndCallService_whenRequestIsValid()
            throws Exception {

        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(null);

        String requestBody = """
                {
                  "login": "testuser",
                  "password": "Password123!"
                }
                """;

        // Act and Assert
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isOk());

        ArgumentCaptor<LoginRequest> requestCaptor =
                ArgumentCaptor.forClass(LoginRequest.class);

        verify(authService).login(requestCaptor.capture());

        LoginRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals(LOGIN, capturedRequest.login());
        assertEquals(PASSWORD, capturedRequest.password());
    }

    @Test
    void login_shouldReturnBadRequest_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_shouldReturnUnsupportedMediaType_whenContentTypeIsNotJson()
            throws Exception {

        String requestBody = """
                {
                  "login": "testuser",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(authService);
    }

    @Test
    void login_shouldReturnMethodNotAllowed_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/login")
                )
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(authService);
    }
}