package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController userController =
                new UserController(userService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    void createUser_shouldReturnCreatedAndCallService_whenRequestIsValid()
            throws Exception {

        // Arrange
        UserResponse serviceResponse = new UserResponse(
                USER_ID,
                "Иван",
                "Иванович",
                "Иванов",
                "ivanov",
                UserRole.USER
        );

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(serviceResponse);

        String requestBody = """
                {
                  "firstName": "Иван",
                  "middleName": "Иванович",
                  "lastName": "Иванов",
                  "role": "USER",
                  "login": "ivanov",
                  "password": "Password123!"
                }
                """;

        // Act and Assert
        mockMvc.perform(
                        post("/api/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.middleName").value("Иванович"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.login").value("ivanov"))
                .andExpect(jsonPath("$.role").value("USER"));

        ArgumentCaptor<CreateUserRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateUserRequest.class);

        verify(userService).createUser(requestCaptor.capture());

        CreateUserRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals("Иван", capturedRequest.firstName());
        assertEquals("Иванович", capturedRequest.middleName());
        assertEquals("Иванов", capturedRequest.lastName());
        assertEquals(UserRole.USER, capturedRequest.role());
        assertEquals("ivanov", capturedRequest.login());
        assertEquals("Password123!", capturedRequest.password());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenRequestBodyIsMissing()
            throws Exception {

        // Act and Assert
        mockMvc.perform(
                        post("/api/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void getAllUsers_shouldReturnOkAndPageOfUsers()
            throws Exception {

        // Arrange
        UserResponse firstUser = new UserResponse(
                1L,
                "Иван",
                "Иванович",
                "Иванов",
                "ivanov",
                UserRole.USER
        );

        UserResponse secondUser = new UserResponse(
                2L,
                "Пётр",
                null,
                "Петров",
                "petrov",
                UserRole.ADMIN
        );

        Page<UserResponse> serviceResponse =
                new PageImpl<>(List.of(firstUser, secondUser));

        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(serviceResponse);

        // Act and Assert
        mockMvc.perform(
                        get("/api/admin/users")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "id,desc")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(
                        jsonPath("$.content[0].login")
                                .value("ivanov")
                )
                .andExpect(
                        jsonPath("$.content[0].role")
                                .value("USER")
                )
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(
                        jsonPath("$.content[1].login")
                                .value("petrov")
                )
                .andExpect(
                        jsonPath("$.content[1].role")
                                .value("ADMIN")
                );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(userService).getAllUsers(
                pageableCaptor.capture()
        );

        Pageable capturedPageable =
                pageableCaptor.getValue();

        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());

        assertNotNull(
                capturedPageable.getSort().getOrderFor("id")
        );

        assertEquals(
                "DESC",
                capturedPageable
                        .getSort()
                        .getOrderFor("id")
                        .getDirection()
                        .name()
        );
    }

    @Test
    void getUserById_shouldReturnOkAndUser()
            throws Exception {

        // Arrange
        UserResponse serviceResponse = new UserResponse(
                USER_ID,
                "Иван",
                "Иванович",
                "Иванов",
                "ivanov",
                UserRole.USER
        );

        when(userService.getUserById(USER_ID))
                .thenReturn(serviceResponse);

        // Act and Assert
        mockMvc.perform(
                        get("/api/admin/users/{userId}", USER_ID)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.middleName").value("Иванович"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.login").value("ivanov"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).getUserById(USER_ID);
    }

    @Test
    void updateUser_shouldReturnOkAndCallService_whenRequestIsValid()
            throws Exception {

        // Arrange
        UserResponse serviceResponse = new UserResponse(
                USER_ID,
                "Пётр",
                "Петрович",
                "Петров",
                "ivanov",
                UserRole.ADMIN
        );

        when(userService.updateUser(
                eq(USER_ID),
                any(UpdateUserRequest.class)
        )).thenReturn(serviceResponse);

        String requestBody = """
                {
                  "firstName": "Пётр",
                  "middleName": "Петрович",
                  "lastName": "Петров",
                  "role": "ADMIN"
                }
                """;

        // Act and Assert
        mockMvc.perform(
                        put("/api/admin/users/{userId}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.middleName").value("Петрович"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.login").value("ivanov"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        ArgumentCaptor<UpdateUserRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateUserRequest.class);

        verify(userService).updateUser(
                eq(USER_ID),
                requestCaptor.capture()
        );

        UpdateUserRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals("Пётр", capturedRequest.firstName());
        assertEquals("Петрович", capturedRequest.middleName());
        assertEquals("Петров", capturedRequest.lastName());
        assertEquals(UserRole.ADMIN, capturedRequest.role());
    }

    @Test
    void updateUser_shouldReturnBadRequest_whenRequestBodyIsMissing()
            throws Exception {

        // Act and Assert
        mockMvc.perform(
                        put("/api/admin/users/{userId}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_shouldReturnNoContentAndCallService()
            throws Exception {

        // Arrange
        doNothing()
                .when(userService)
                .deleteUser(USER_ID);

        // Act and Assert
        mockMvc.perform(
                        delete("/api/admin/users/{userId}", USER_ID)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).deleteUser(USER_ID);
    }
}