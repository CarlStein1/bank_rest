package com.example.bankcards.service;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardAccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.CardNumberMasker;
import com.example.bankcards.util.crypto.CardNumberCrypto;
import com.example.bankcards.util.crypto.EncryptedCardData;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CARD_ID = 10L;

    private static final String CARD_NUMBER = "1234567890123456";
    private static final String LAST_FOUR = "3456";
    private static final String MASKED_NUMBER = "**** **** **** 3456";

    @Mock
    private CardNumberMasker cardNumberMasker;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private CardNumberCrypto cardNumberCrypto;

    @InjectMocks
    private CardService cardService;

    @Test
    void createCard_shouldCreateAndReturnCard_whenUserExists() {
        // Arrange
        User user = mock(User.class);
        Card savedCard = mock(Card.class);
        EncryptedCardData encryptedCardData =
                mock(EncryptedCardData.class);

        byte[] encryptedNumber = {1, 2, 3, 4};
        byte[] iv = {5, 6, 7, 8};
        short keyVersion = 1;

        LocalDate expirationDate =
                LocalDate.now().plusYears(5);

        BigDecimal balance = BigDecimal.ZERO;

        when(userService.getUserEntityById(USER_ID))
                .thenReturn(user);

        when(cardNumberGenerator.generate())
                .thenReturn(CARD_NUMBER);

        when(cardNumberCrypto.encrypt(CARD_NUMBER))
                .thenReturn(encryptedCardData);

        when(encryptedCardData.encryptedNumber())
                .thenReturn(encryptedNumber);

        when(encryptedCardData.iv())
                .thenReturn(iv);

        when(encryptedCardData.keyVersion())
                .thenReturn(keyVersion);

        when(cardRepository.save(any(Card.class)))
                .thenReturn(savedCard);

        stubCardForResponse(
                savedCard,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.ACTIVE,
                balance,
                MASKED_NUMBER
        );

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.ACTIVE,
                balance
        );

        // Act
        CardResponse actualResponse =
                cardService.createCard(USER_ID);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        ArgumentCaptor<Card> cardCaptor =
                ArgumentCaptor.forClass(Card.class);

        verify(cardRepository).save(cardCaptor.capture());

        Card createdCard = cardCaptor.getValue();

        assertSame(user, createdCard.getUser());
        assertEquals(LAST_FOUR, createdCard.getNumberLastFour());
        assertEquals(expirationDate, createdCard.getExpirationDate());

        verify(userService).getUserEntityById(USER_ID);
        verify(cardNumberGenerator).generate();
        verify(cardNumberCrypto).encrypt(CARD_NUMBER);
        verify(cardNumberMasker).mask(LAST_FOUR);
    }

    @Test
    void getAllCards_shouldReturnPageOfCardResponses() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        User firstUser = mock(User.class);
        User secondUser = mock(User.class);

        Card firstCard = mock(Card.class);
        Card secondCard = mock(Card.class);

        LocalDate firstExpiration =
                LocalDate.now().plusYears(3);

        LocalDate secondExpiration =
                LocalDate.now().plusYears(4);

        BigDecimal firstBalance =
                new BigDecimal("1000.00");

        BigDecimal secondBalance =
                new BigDecimal("2500.00");

        stubCardForResponse(
                firstCard,
                firstUser,
                10L,
                USER_ID,
                "1111",
                firstExpiration,
                CardStatus.ACTIVE,
                firstBalance,
                "**** **** **** 1111"
        );

        stubCardForResponse(
                secondCard,
                secondUser,
                20L,
                OTHER_USER_ID,
                "2222",
                secondExpiration,
                CardStatus.BLOCKED,
                secondBalance,
                "**** **** **** 2222"
        );

        Page<Card> cards = new PageImpl<>(
                List.of(firstCard, secondCard),
                pageable,
                2
        );

        when(cardRepository.findAll(pageable))
                .thenReturn(cards);

        CardResponse firstExpected = new CardResponse(
                10L,
                USER_ID,
                "**** **** **** 1111",
                firstExpiration,
                CardStatus.ACTIVE,
                firstBalance
        );

        CardResponse secondExpected = new CardResponse(
                20L,
                OTHER_USER_ID,
                "**** **** **** 2222",
                secondExpiration,
                CardStatus.BLOCKED,
                secondBalance
        );

        // Act
        Page<CardResponse> result =
                cardService.getAllCards(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());

        assertEquals(
                List.of(firstExpected, secondExpected),
                result.getContent()
        );

        verify(cardRepository).findAll(pageable);
    }

    @Test
    void getCardById_shouldReturnCardResponse_whenCardExists() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        LocalDate expirationDate =
                LocalDate.now().plusYears(3);

        BigDecimal balance =
                new BigDecimal("1500.00");

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        stubCardForResponse(
                card,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.ACTIVE,
                balance,
                MASKED_NUMBER
        );

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.ACTIVE,
                balance
        );

        // Act
        CardResponse actualResponse =
                cardService.getCardById(CARD_ID);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(cardRepository).findById(CARD_ID);
        verify(cardNumberMasker).mask(LAST_FOUR);
    }

    @Test
    void getCardById_shouldThrowException_whenCardDoesNotExist() {
        // Arrange
        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                CardNotFoundException.class,
                () -> cardService.getCardById(CARD_ID)
        );

        verify(cardRepository).findById(CARD_ID);
        verifyNoInteractions(cardNumberMasker);
    }

    @Test
    void getUserCards_shouldReturnOnlyUserCards() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        User user = mock(User.class);
        Card card = mock(Card.class);

        LocalDate expirationDate =
                LocalDate.now().plusYears(3);

        BigDecimal balance =
                new BigDecimal("500.00");

        stubCardForResponse(
                card,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.ACTIVE,
                balance,
                MASKED_NUMBER
        );

        Page<Card> cards = new PageImpl<>(
                List.of(card),
                pageable,
                1
        );

        when(cardRepository.findAllByUser_Id(USER_ID, pageable))
                .thenReturn(cards);

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.ACTIVE,
                balance
        );

        // Act
        Page<CardResponse> result =
                cardService.getUserCards(USER_ID, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedResponse, result.getContent().get(0));

        verify(cardRepository)
                .findAllByUser_Id(USER_ID, pageable);
    }

    @Test
    void getUserCard_shouldReturnCard_whenCardBelongsToUser() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        LocalDate expirationDate =
                LocalDate.now().plusYears(3);

        BigDecimal balance =
                new BigDecimal("750.00");

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        stubCardForResponse(
                card,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.ACTIVE,
                balance,
                MASKED_NUMBER
        );

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.ACTIVE,
                balance
        );

        // Act
        CardResponse actualResponse =
                cardService.getUserCard(USER_ID, CARD_ID);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(cardRepository).findById(CARD_ID);
        verify(cardNumberMasker).mask(LAST_FOUR);
    }

    @Test
    void getUserCard_shouldThrowException_whenCardBelongsToAnotherUser() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        when(card.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(OTHER_USER_ID);

        // Act and Assert
        assertThrows(
                CardAccessDeniedException.class,
                () -> cardService.getUserCard(USER_ID, CARD_ID)
        );

        verify(cardRepository).findById(CARD_ID);
        verifyNoInteractions(cardNumberMasker);
    }

    @Test
    void getUserCardBalance_shouldReturnBalance_whenCardBelongsToUser() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        BigDecimal balance =
                new BigDecimal("3250.50");

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        when(card.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        when(card.getBalance())
                .thenReturn(balance);

        // Act
        BigDecimal result =
                cardService.getUserCardBalance(USER_ID, CARD_ID);

        // Assert
        assertEquals(balance, result);

        verify(cardRepository).findById(CARD_ID);
        verify(card).getBalance();
    }

    @Test
    void getUserCardBalance_shouldThrowException_whenCardBelongsToAnotherUser() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        when(card.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(OTHER_USER_ID);

        // Act and Assert
        assertThrows(
                CardAccessDeniedException.class,
                () -> cardService.getUserCardBalance(
                        USER_ID,
                        CARD_ID
                )
        );

        verify(card, never()).getBalance();
    }

    @Test
    void blockCard_shouldBlockAndReturnCard_whenCardExists() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        LocalDate expirationDate =
                LocalDate.now().plusYears(3);

        BigDecimal balance =
                new BigDecimal("1000.00");

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        /*
         * После вызова card.block() сервис формирует ответ.
         * Поэтому мок должен вернуть уже итоговый статус BLOCKED.
         */
        stubCardForResponse(
                card,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.BLOCKED,
                balance,
                MASKED_NUMBER
        );

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.BLOCKED,
                balance
        );

        // Act
        CardResponse actualResponse =
                cardService.blockCard(CARD_ID);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(card).block();
        verify(cardRepository).findById(CARD_ID);

        verify(cardRepository, never())
                .save(card);
    }

    @Test
    void activateCard_shouldActivateAndReturnCard_whenCardExists() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        LocalDate expirationDate =
                LocalDate.now().plusYears(3);

        BigDecimal balance =
                new BigDecimal("1000.00");

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        stubCardForResponse(
                card,
                user,
                CARD_ID,
                USER_ID,
                LAST_FOUR,
                expirationDate,
                CardStatus.ACTIVE,
                balance,
                MASKED_NUMBER
        );

        CardResponse expectedResponse = new CardResponse(
                CARD_ID,
                USER_ID,
                MASKED_NUMBER,
                expirationDate,
                CardStatus.ACTIVE,
                balance
        );

        // Act
        CardResponse actualResponse =
                cardService.activateCard(CARD_ID);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(card).activate();
        verify(cardRepository).findById(CARD_ID);

        verify(cardRepository, never())
                .save(card);
    }

    @Test
    void deleteCard_shouldDeleteCard_whenCardExists() {
        // Arrange
        Card card = mock(Card.class);

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        // Act
        cardService.deleteCard(CARD_ID);

        // Assert
        verify(cardRepository).findById(CARD_ID);
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowException_whenCardDoesNotExist() {
        // Arrange
        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                CardNotFoundException.class,
                () -> cardService.deleteCard(CARD_ID)
        );

        verify(cardRepository).findById(CARD_ID);

        verify(cardRepository, never())
                .delete(any(Card.class));
    }

    @Test
    void getCardEntityById_shouldReturnEntity_whenCardExists() {
        // Arrange
        Card card = mock(Card.class);

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        // Act
        Card result =
                cardService.getCardEntityById(CARD_ID);

        // Assert
        assertSame(card, result);

        verify(cardRepository).findById(CARD_ID);
    }

    @Test
    void getUserCardEntity_shouldReturnEntity_whenCardBelongsToUser() {
        // Arrange
        User user = mock(User.class);
        Card card = mock(Card.class);

        when(cardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(card));

        when(card.getUser())
                .thenReturn(user);

        when(user.getId())
                .thenReturn(USER_ID);

        // Act
        Card result =
                cardService.getUserCardEntity(USER_ID, CARD_ID);

        // Assert
        assertSame(card, result);

        verify(cardRepository).findById(CARD_ID);
    }

    private void stubCardForResponse(
            Card card,
            User user,
            Long cardId,
            Long userId,
            String lastFour,
            LocalDate expirationDate,
            CardStatus status,
            BigDecimal balance,
            String maskedNumber
    ) {
        when(card.getId()).thenReturn(cardId);
        when(card.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(card.getNumberLastFour()).thenReturn(lastFour);
        when(card.getExpirationDate()).thenReturn(expirationDate);
        when(card.getStatus()).thenReturn(status);
        when(card.getBalance()).thenReturn(balance);

        when(cardNumberMasker.mask(lastFour))
                .thenReturn(maskedNumber);
    }
}