package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import com.example.bankcards.exception.LoginAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    private static final String FIRST_NAME = "Иван";
    private static final String MIDDLE_NAME = "Иванович";
    private static final String LAST_NAME = "Иванов";

    private static final String LOGIN = "ivanov";
    private static final String PASSWORD = "password123";
    private static final String PASSWORD_HASH = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_shouldReturnPageOfUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        User firstUser = mock(User.class);
        User secondUser = mock(User.class);

        mockUser(
                firstUser,
                1L,
                "Иван",
                "Иванович",
                "Иванов",
                "ivanov",
                UserRole.USER
        );

        mockUser(
                secondUser,
                2L,
                "Пётр",
                null,
                "Петров",
                "petrov",
                UserRole.ADMIN
        );

        Page<User> users = new PageImpl<>(
                List.of(firstUser, secondUser),
                pageable,
                2
        );

        when(userRepository.findAll(pageable))
                .thenReturn(users);

        // Act
        Page<UserResponse> result =
                userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        UserResponse firstResponse = result.getContent().get(0);
        UserResponse secondResponse = result.getContent().get(1);

        assertEquals(1L, firstResponse.id());
        assertEquals("Иван", firstResponse.firstName());
        assertEquals("ivanov", firstResponse.login());
        assertEquals(UserRole.USER, firstResponse.role());

        assertEquals(2L, secondResponse.id());
        assertEquals("Пётр", secondResponse.firstName());
        assertEquals("petrov", secondResponse.login());
        assertEquals(UserRole.ADMIN, secondResponse.role());

        verify(userRepository).findAll(pageable);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        // Arrange
        User user = mock(User.class);

        mockUser(
                user,
                USER_ID,
                FIRST_NAME,
                MIDDLE_NAME,
                LAST_NAME,
                LOGIN,
                UserRole.USER
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // Act
        UserResponse response =
                userService.getUserById(USER_ID);

        // Assert
        assertNotNull(response);
        assertEquals(USER_ID, response.id());
        assertEquals(FIRST_NAME, response.firstName());
        assertEquals(MIDDLE_NAME, response.middleName());
        assertEquals(LAST_NAME, response.lastName());
        assertEquals(LOGIN, response.login());
        assertEquals(UserRole.USER, response.role());

        verify(userRepository).findById(USER_ID);
    }

    @Test
    void getUserById_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(USER_ID)
        );

        verify(userRepository).findById(USER_ID);
    }

    @Test
    void createUser_shouldCreateUser_whenLoginIsAvailable() {
        // Arrange
        CreateUserRequest request =
                mock(CreateUserRequest.class);

        when(request.firstName()).thenReturn(FIRST_NAME);
        when(request.middleName()).thenReturn(MIDDLE_NAME);
        when(request.lastName()).thenReturn(LAST_NAME);
        when(request.role()).thenReturn(UserRole.USER);
        when(request.login()).thenReturn(LOGIN);
        when(request.password()).thenReturn(PASSWORD);

        when(userRepository.existsByLogin(LOGIN))
                .thenReturn(false);

        when(passwordEncoder.encode(PASSWORD))
                .thenReturn(PASSWORD_HASH);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse response =
                userService.createUser(request);

        // Assert
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();

        assertEquals(FIRST_NAME, createdUser.getFirstName());
        assertEquals(MIDDLE_NAME, createdUser.getMiddleName());
        assertEquals(LAST_NAME, createdUser.getLastName());
        assertEquals(UserRole.USER, createdUser.getRole());
        assertEquals(LOGIN, createdUser.getLogin());
        assertEquals(PASSWORD_HASH, createdUser.getPasswordHash());

        assertNotNull(response);
        assertEquals(FIRST_NAME, response.firstName());
        assertEquals(MIDDLE_NAME, response.middleName());
        assertEquals(LAST_NAME, response.lastName());
        assertEquals(LOGIN, response.login());
        assertEquals(UserRole.USER, response.role());

        verify(userRepository).existsByLogin(LOGIN);
        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void createUser_shouldThrowException_whenLoginAlreadyExists() {
        // Arrange
        CreateUserRequest request =
                mock(CreateUserRequest.class);

        when(request.login()).thenReturn(LOGIN);

        when(userRepository.existsByLogin(LOGIN))
                .thenReturn(true);

        // Act and Assert
        assertThrows(
                LoginAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository).existsByLogin(LOGIN);

        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void updateUser_shouldUpdatePersonalDataAndRole_whenUserExists() {
        // Arrange
        String newFirstName = "Пётр";
        String newMiddleName = "Петрович";
        String newLastName = "Петров";

        UpdateUserRequest request =
                mock(UpdateUserRequest.class);

        when(request.firstName()).thenReturn(newFirstName);
        when(request.middleName()).thenReturn(newMiddleName);
        when(request.lastName()).thenReturn(newLastName);
        when(request.role()).thenReturn(UserRole.ADMIN);

        User user = mock(User.class);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        mockUser(
                user,
                USER_ID,
                newFirstName,
                newMiddleName,
                newLastName,
                LOGIN,
                UserRole.ADMIN
        );

        // Act
        UserResponse response =
                userService.updateUser(USER_ID, request);

        // Assert
        verify(user).updatePersonalData(
                newFirstName,
                newMiddleName,
                newLastName
        );

        verify(user).changeRole(UserRole.ADMIN);

        assertNotNull(response);
        assertEquals(USER_ID, response.id());
        assertEquals(newFirstName, response.firstName());
        assertEquals(newMiddleName, response.middleName());
        assertEquals(newLastName, response.lastName());
        assertEquals(LOGIN, response.login());
        assertEquals(UserRole.ADMIN, response.role());

        /*
         * Метод save() здесь не нужен, потому что настоящий User
         * находится в persistence context и Hibernate использует
         * механизм dirty checking.
         */
        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        UpdateUserRequest request =
                mock(UpdateUserRequest.class);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(USER_ID, request)
        );

        verify(userRepository).findById(USER_ID);
        verifyNoInteractions(request);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        // Arrange
        User user = mock(User.class);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(USER_ID);

        // Assert
        verify(userRepository).findById(USER_ID);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(USER_ID)
        );

        verify(userRepository).findById(USER_ID);

        verify(userRepository, never())
                .delete(any(User.class));
    }

    @Test
    void getUserEntityById_shouldReturnEntity_whenUserExists() {
        // Arrange
        User user = mock(User.class);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // Act
        User result =
                userService.getUserEntityById(USER_ID);

        // Assert
        assertSame(user, result);

        verify(userRepository).findById(USER_ID);
    }

    private void mockUser(
            User user,
            Long id,
            String firstName,
            String middleName,
            String lastName,
            String login,
            UserRole role
    ) {
        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getMiddleName()).thenReturn(middleName);
        when(user.getLastName()).thenReturn(lastName);
        when(user.getLogin()).thenReturn(login);
        when(user.getRole()).thenReturn(role);
    }
}