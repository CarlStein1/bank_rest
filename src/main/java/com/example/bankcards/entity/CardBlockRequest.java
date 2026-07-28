package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import com.example.bankcards.exception.CardBlockRequestAlreadyProcessedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "card_block_requests")
public class CardBlockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_block_request")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 20)
    private CardBlockRequestStatus status;

    @Size(max = 500)
    @Column(name = "reason", length = 500)
    private String reason;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Size(max = 500)
    @Column(name = "admin_comment", length = 500)
    private String adminComment;

    protected CardBlockRequest() {
    }

    public CardBlockRequest(Card card, String reason) {
        this.card = card;
        this.reason = reason;
        this.status = CardBlockRequestStatus.PENDING;
    }

    public void approve(User processedBy, String adminComment) {
        ensurePending();

        this.status = CardBlockRequestStatus.APPROVED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }

    public void reject(User processedBy, String adminComment) {
        ensurePending();

        this.status = CardBlockRequestStatus.REJECTED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }

    public boolean isPending() {
        return status == CardBlockRequestStatus.PENDING;
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new CardBlockRequestAlreadyProcessedException(id);
        }
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}