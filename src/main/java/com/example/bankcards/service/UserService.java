package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateUserRequest;
import com.example.bankcards.dto.request.UpdateUserRequest;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.LoginAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(Long userId) {
        return toResponse(getUserEntityById(userId));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new LoginAlreadyExistsException(request.login());
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.role(),
                request.login(),
                passwordHash
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(
            Long userId,
            UpdateUserRequest request
    ) {
        User user = getUserEntityById(userId);

        user.updatePersonalData(
                request.firstName(),
                request.middleName(),
                request.lastName()
        );

        user.changeRole(request.role());

        return toResponse(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        User user = getUserEntityById(userId);

        userRepository.delete(user);
    }

    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getLogin(),
                user.getRole()
        );
    }
}