package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardBlockRequest;
import com.example.bankcards.dto.request.ProcessCardBlockRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.DuplicateBlockRequestException;
import com.example.bankcards.repository.CardBlockRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardBlockRequestService {

    private final CardBlockRequestRepository blockRequestRepository;
    private final CardService cardService;
    private final UserService userService;

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CardBlockRequest createBlockRequest(
            Long userId,
            Long cardId,
            CreateCardBlockRequest request
    ) {
        Card card = cardService.getUserCardEntity(userId, cardId);

        if (card.isBlocked()) {
            throw new CardBlockedException(cardId);
        }

        boolean pendingRequestExists =
                blockRequestRepository.existsByCard_IdAndStatus(
                        cardId,
                        CardBlockRequestStatus.PENDING
                );

        if (pendingRequestExists) {
            throw new DuplicateBlockRequestException(cardId);
        }

        CardBlockRequest blockRequest = new CardBlockRequest(
                card,
                normalizeNullableText(request.reason())
        );

        return blockRequestRepository.save(blockRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardBlockRequest> getAllBlockRequests(
            CardBlockRequestStatus status,
            Pageable pageable
    ) {
        if (status == null) {
            return blockRequestRepository.findAll(pageable);
        }

        return blockRequestRepository.findAllByStatus(
                status,
                pageable
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CardBlockRequest getBlockRequestById(Long requestId) {
        return getBlockRequestEntityById(requestId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CardBlockRequest approveBlockRequest(
            Long adminId,
            Long requestId,
            ProcessCardBlockRequest request
    ) {
        User admin = userService.getUserEntityById(adminId);

        CardBlockRequest blockRequest =
                getBlockRequestEntityById(requestId);

        blockRequest.approve(
                admin,
                normalizeNullableText(request.adminComment())
        );

        blockRequest.getCard().block();

        return blockRequest;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CardBlockRequest rejectBlockRequest(
            Long adminId,
            Long requestId,
            ProcessCardBlockRequest request
    ) {
        User admin = userService.getUserEntityById(adminId);

        CardBlockRequest blockRequest =
                getBlockRequestEntityById(requestId);

        blockRequest.reject(
                admin,
                normalizeNullableText(request.adminComment())
        );

        return blockRequest;
    }

    public CardBlockRequest getBlockRequestEntityById(Long requestId) {
        return blockRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Заявка на блокировку с id "
                                        + requestId
                                        + " не найдена"
                        )
                );
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}