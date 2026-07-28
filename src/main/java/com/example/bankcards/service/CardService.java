package com.example.bankcards.service;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardAccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.crypto.CardNumberCrypto;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.CardNumberMasker;
import com.example.bankcards.util.crypto.EncryptedCardData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private static final int CARD_VALIDITY_YEARS = 5;

    private final CardNumberMasker cardNumberMasker;
    private final CardRepository cardRepository;
    private final UserService userService;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardNumberCrypto cardNumberCrypto;

    @Transactional
    public CardResponse createCard(Long userId) {
        User user = userService.getUserEntityById(userId);

        String cardNumber = cardNumberGenerator.generate();

        EncryptedCardData encryptedCardData =
                cardNumberCrypto.encrypt(cardNumber);

        String numberLastFour = cardNumber.substring(
                cardNumber.length() - 4
        );

        LocalDate expirationDate = LocalDate.now()
                .plusYears(CARD_VALIDITY_YEARS);

        Card card = new Card(
                user,
                encryptedCardData.encryptedNumber(),
                encryptedCardData.iv(),
                numberLastFour,
                encryptedCardData.keyVersion(),
                expirationDate
        );

        Card savedCard = cardRepository.save(card);

        return toResponse(savedCard);
    }

    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public CardResponse getCardById(Long cardId) {
        return toResponse(
                getCardEntityById(cardId)
        );
    }

    public Page<CardResponse> getUserCards(
            Long userId,
            Pageable pageable
    ) {
        return cardRepository.findAllByUser_Id(
                        userId,
                        pageable
                )
                .map(this::toResponse);
    }

    public CardResponse getUserCard(
            Long userId,
            Long cardId
    ) {
        return toResponse(
                getUserCardEntity(userId, cardId)
        );
    }

    public BigDecimal getUserCardBalance(
            Long userId,
            Long cardId
    ) {
        Card card = getUserCardEntity(userId, cardId);

        return card.getBalance();
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = getCardEntityById(cardId);

        card.block();

        return toResponse(card);
    }

    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = getCardEntityById(cardId);

        card.activate();

        return toResponse(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = getCardEntityById(cardId);

        cardRepository.delete(card);
    }

    public Card getCardEntityById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(
                        () -> new CardNotFoundException(cardId)
                );
    }

    public Card getUserCardEntity(
            Long userId,
            Long cardId
    ) {
        Card card = getCardEntityById(cardId);

        if (!card.getUser().getId().equals(userId)) {
            throw new CardAccessDeniedException(cardId);
        }

        return card;
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getUser().getId(),
                cardNumberMasker.mask(
                        card.getNumberLastFour()
                ),
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance()
        );
    }
}