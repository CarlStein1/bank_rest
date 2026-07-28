package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final String LOGIN = "test-user";
    private static final String PASSWORD = "test-password";
    private static final String TOKEN = "test-jwt-token";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal principal;

    @Mock
    private User user;

    @Mock
    private AuthResponse expectedResponse;

    @InjectMocks
    private AuthService authService;

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        System.out.println(
                encoder.encode("StrongPass123")
        );
    }

    @Test
    void login_shouldAuthenticateUserAndReturnAuthResponse_whenCredentialsAreValid() {
        // Arrange
        LoginRequest request = new LoginRequest(
                LOGIN,
                PASSWORD
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(principal);

        when(principal.getId())
                .thenReturn(USER_ID);

        when(jwtService.generateAccessToken(principal))
                .thenReturn(TOKEN);

        when(userService.getUserEntityById(USER_ID))
                .thenReturn(user);

        try (MockedStatic<AuthResponse> authResponseMock =
                     mockStatic(AuthResponse.class)) {

            authResponseMock
                    .when(() -> AuthResponse.from(user, TOKEN))
                    .thenReturn(expectedResponse);

            // Act
            AuthResponse actualResponse =
                    authService.login(request);

            // Assert
            assertSame(expectedResponse, actualResponse);

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
            );

            verify(authentication).getPrincipal();

            verify(jwtService).generateAccessToken(principal);

            verify(userService).getUserEntityById(USER_ID);

            authResponseMock.verify(
                    () -> AuthResponse.from(user, TOKEN)
            );
        }
    }

    @Test
    void login_shouldPassLoginAndPasswordToAuthenticationManager() {
        // Arrange
        LoginRequest request = new LoginRequest(
                LOGIN,
                PASSWORD
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(principal);

        when(principal.getId())
                .thenReturn(USER_ID);

        when(jwtService.generateAccessToken(principal))
                .thenReturn(TOKEN);

        when(userService.getUserEntityById(USER_ID))
                .thenReturn(user);

        try (MockedStatic<AuthResponse> authResponseMock =
                     mockStatic(AuthResponse.class)) {

            authResponseMock
                    .when(() -> AuthResponse.from(user, TOKEN))
                    .thenReturn(expectedResponse);

            // Act
            authService.login(request);

            // Assert
            ArgumentCaptor<Authentication> authenticationCaptor =
                    ArgumentCaptor.forClass(Authentication.class);

            verify(authenticationManager).authenticate(
                    authenticationCaptor.capture()
            );

            Authentication authenticationRequest =
                    authenticationCaptor.getValue();

            assertInstanceOf(
                    UsernamePasswordAuthenticationToken.class,
                    authenticationRequest
            );

            assertEquals(
                    LOGIN,
                    authenticationRequest.getPrincipal()
            );

            assertEquals(
                    PASSWORD,
                    authenticationRequest.getCredentials()
            );
        }
    }

    @Test
    void login_shouldGenerateTokenForAuthenticatedPrincipal() {
        // Arrange
        LoginRequest request = new LoginRequest(
                LOGIN,
                PASSWORD
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(principal);

        when(principal.getId())
                .thenReturn(USER_ID);

        when(jwtService.generateAccessToken(principal))
                .thenReturn(TOKEN);

        when(userService.getUserEntityById(USER_ID))
                .thenReturn(user);

        try (MockedStatic<AuthResponse> authResponseMock =
                     mockStatic(AuthResponse.class)) {

            authResponseMock
                    .when(() -> AuthResponse.from(user, TOKEN))
                    .thenReturn(expectedResponse);

            // Act
            authService.login(request);

            // Assert
            verify(jwtService).generateAccessToken(principal);
        }
    }

    @Test
    void login_shouldLoadUserByPrincipalId() {
        // Arrange
        LoginRequest request = new LoginRequest(
                LOGIN,
                PASSWORD
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(principal);

        when(principal.getId())
                .thenReturn(USER_ID);

        when(jwtService.generateAccessToken(principal))
                .thenReturn(TOKEN);

        when(userService.getUserEntityById(USER_ID))
                .thenReturn(user);

        try (MockedStatic<AuthResponse> authResponseMock =
                     mockStatic(AuthResponse.class)) {

            authResponseMock
                    .when(() -> AuthResponse.from(user, TOKEN))
                    .thenReturn(expectedResponse);

            // Act
            authService.login(request);

            // Assert
            verify(userService).getUserEntityById(USER_ID);
        }
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenCredentialsAreInvalid() {
        // Arrange
        LoginRequest request = new LoginRequest(
                LOGIN,
                "wrong-password"
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(
                        new BadCredentialsException(
                                "Неверный логин или пароль"
                        )
                );

        // Act
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        // Assert
        assertEquals(
                "Неверный логин или пароль",
                exception.getMessage()
        );

        verify(authenticationManager).authenticate(
                any(Authentication.class)
        );

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userService);
        verifyNoInteractions(authentication);
        verifyNoInteractions(principal);
    }
}