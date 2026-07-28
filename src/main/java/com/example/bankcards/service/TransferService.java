package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardExpiredException;
import com.example.bankcards.exception.CardAccessDeniedException;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardService cardService;

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TransferResponse transfer(
            Long userId,
            TransferRequest request
    ) {
        validateTransferRequest(request);

        Card sourceCard = cardService.getCardEntityById(
                request.fromCardId()
        );

        Card targetCard = cardService.getCardEntityById(
                request.toCardId()
        );

        checkCardOwnership(sourceCard, userId);
        checkCardOwnership(targetCard, userId);

        checkCardAvailableForTransfer(sourceCard);
        checkCardAvailableForTransfer(targetCard);

        BigDecimal amount = request.amount();

        if (sourceCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sourceCard.getId());
        }

        sourceCard.withdraw(amount);
        targetCard.deposit(amount);

        return TransferResponse.of(
                sourceCard,
                targetCard,
                amount
        );
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new IllegalArgumentException(
                    "Нельзя выполнить перевод на ту же карту"
            );
        }

        if (request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумма перевода должна быть больше нуля"
            );
        }
    }

    private void checkCardOwnership(Card card, Long userId) {
        if (!card.getUser().getId().equals(userId)) {
            throw new CardAccessDeniedException(card.getId());
        }
    }

    private void checkCardAvailableForTransfer(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardBlockedException(card.getId());
        }

        if (card.getStatus() == CardStatus.EXPIRED
                || card.getExpirationDate()
                .isBefore(LocalDate.now())) {

            throw new CardExpiredException(card.getId());
        }
    }
}