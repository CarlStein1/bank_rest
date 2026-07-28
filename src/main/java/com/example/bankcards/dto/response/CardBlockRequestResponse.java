package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;

import java.time.LocalDateTime;

public record CardBlockRequestResponse(
        Long id,
        Long cardId,
        CardBlockRequestStatus status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        Long processedById,
        String adminComment
) {

    public static CardBlockRequestResponse from(
            CardBlockRequest blockRequest
    ) {
        Long processedById =
                blockRequest.getProcessedBy() == null
                        ? null
                        : blockRequest.getProcessedBy().getId();

        return new CardBlockRequestResponse(
                blockRequest.getId(),
                blockRequest.getCard().getId(),
                blockRequest.getStatus(),
                blockRequest.getReason(),
                blockRequest.getCreatedAt(),
                blockRequest.getProcessedAt(),
                processedById,
                blockRequest.getAdminComment()
        );
    }
}