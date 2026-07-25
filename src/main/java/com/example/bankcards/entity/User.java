package com.example.bankcards.entity;

import jakarta.persistence.*;
import com.example.bankcards.entity.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "middle_name", length = 50)
    private String middleName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected User() {
    }

    public User(
            String firstName,
            String middleName,
            String lastName,
            UserRole role,
            String login,
            String passwordHash
    ) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.role = role;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public void updatePersonalData(
            String firstName,
            String middleName,
            String lastName
    ) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public String getFullName() {
        if (middleName == null || middleName.isBlank()) {
            return lastName + " " + firstName;
        }

        return lastName + " " + firstName + " " + middleName;
    }
}
