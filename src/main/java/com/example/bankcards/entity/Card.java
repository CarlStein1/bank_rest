package com.example.bankcards.entity;

import jakarta.persistence.*;
import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_card")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Size(max = 64)
    @Column(name = "number_encrypted", nullable = false, length = 64)
    private byte[] encryptedNumber;

    @NotNull
    @Size(min = 12, max = 12)
    @Column(name = "number_iv", nullable = false, length = 12)
    private byte[] numberIv;

    @NotNull
    @Pattern(regexp = "\\d{4}")
    @Column(name = "number_last_four", nullable = false, length = 4)
    private String numberLastFour;

    @NotNull
    @Positive
    @Column(name = "encryption_key_version", nullable = false)
    private Short encryptionKeyVersion;

    @NotNull
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "card_status", nullable = false, length = 20)
    private CardStatus status;

    @NotNull
    @PositiveOrZero
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Card() {
    }

    public Card(
            User user,
            byte[] encryptedNumber,
            byte[] numberIv,
            String numberLastFour,
            Short encryptionKeyVersion,
            LocalDate expirationDate
    ) {
        this.user = user;
        this.encryptedNumber = encryptedNumber;
        this.numberIv = numberIv;
        this.numberLastFour = numberLastFour;
        this.encryptionKeyVersion = encryptionKeyVersion;
        this.expirationDate = expirationDate;

        this.status = CardStatus.ACTIVE;
        this.balance = BigDecimal.ZERO;
    }

    public void block() {
        if (status == CardStatus.BLOCKED) {
            throw new IllegalStateException("Карта уже заблокирована");
        }

        if (status == CardStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Нельзя заблокировать просроченную карту"
            );
        }

        status = CardStatus.BLOCKED;
    }

    public void activate() {
        if (status == CardStatus.ACTIVE) {
            throw new IllegalStateException("Карта уже активна");
        }

        if (status == CardStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Нельзя активировать просроченную карту"
            );
        }

        status = CardStatus.ACTIVE;
    }

    public void markExpired() {
        status = CardStatus.EXPIRED;
    }

    public void deposit(BigDecimal amount) {
        validatePositiveAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Недостаточно средств на карте"
            );
        }

        balance = balance.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Сумма операции должна быть положительной"
            );
        }
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == CardStatus.BLOCKED;
    }

    public boolean isExpired() {
        return status == CardStatus.EXPIRED;
    }

    public boolean hasExpiredByDate(LocalDate currentDate) {
        return expirationDate.isBefore(currentDate);
    }

    public String getMaskedNumber() {
        return "**** **** **** " + numberLastFour;
    }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
