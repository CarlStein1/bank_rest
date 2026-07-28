package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardAccessDeniedException;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private static final Long SOURCE_CARD_ID = 10L;
    private static final Long TARGET_CARD_ID = 20L;

    @Mock
    private CardService cardService;

    @Mock
    private Card sourceCard;

    @Mock
    private Card targetCard;

    @Mock
    private User sourceCardOwner;

    @Mock
    private User targetCardOwner;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_shouldTransferMoney_whenRequestIsValid() {
        // Arrange
        BigDecimal amount = new BigDecimal("250.00");

        TransferRequest request = new TransferRequest(
                SOURCE_CARD_ID,
                TARGET_CARD_ID,
                amount
        );

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();
        mockCardAvailable(sourceCard);
        mockCardAvailable(targetCard);

        when(sourceCard.getBalance())
                .thenReturn(new BigDecimal("1000.00"));

        // Act
        TransferResponse response = transferService.transfer(
                USER_ID,
                request
        );

        // Assert
        assertNotNull(response);

        verify(sourceCard).withdraw(amount);
        verify(targetCard).deposit(amount);

        verify(cardService).getCardEntityById(SOURCE_CARD_ID);
        verify(cardService).getCardEntityById(TARGET_CARD_ID);
    }

    @Test
    void transfer_shouldThrowException_whenCardsAreTheSame() {
        // Arrange
        TransferRequest request = new TransferRequest(
                SOURCE_CARD_ID,
                SOURCE_CARD_ID,
                new BigDecimal("100.00")
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertEquals(
                "Нельзя выполнить перевод на ту же карту",
                exception.getMessage()
        );

        verifyNoInteractions(cardService);
        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenAmountIsNull() {
        // Arrange
        TransferRequest request = new TransferRequest(
                SOURCE_CARD_ID,
                TARGET_CARD_ID,
                null
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertEquals(
                "Сумма перевода должна быть больше нуля",
                exception.getMessage()
        );

        verifyNoInteractions(cardService);
        verifyNoMoneyTransferred();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100.50"})
    void transfer_shouldThrowException_whenAmountIsNotPositive(
            String amountValue
    ) {
        // Arrange
        TransferRequest request = new TransferRequest(
                SOURCE_CARD_ID,
                TARGET_CARD_ID,
                new BigDecimal(amountValue)
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertEquals(
                "Сумма перевода должна быть больше нуля",
                exception.getMessage()
        );

        verifyNoInteractions(cardService);
        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenSourceCardBelongsToAnotherUser() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();

        when(sourceCard.getUser())
                .thenReturn(sourceCardOwner);

        when(sourceCardOwner.getId())
                .thenReturn(OTHER_USER_ID);

        // Act and Assert
        assertThrows(
                CardAccessDeniedException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenTargetCardBelongsToAnotherUser() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();

        when(sourceCard.getUser())
                .thenReturn(sourceCardOwner);

        when(sourceCardOwner.getId())
                .thenReturn(USER_ID);

        when(targetCard.getUser())
                .thenReturn(targetCardOwner);

        when(targetCardOwner.getId())
                .thenReturn(OTHER_USER_ID);

        // Act and Assert
        assertThrows(
                CardAccessDeniedException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenSourceCardIsBlocked() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();

        when(sourceCard.getStatus())
                .thenReturn(CardStatus.BLOCKED);

        // Act and Assert
        assertThrows(
                CardBlockedException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenTargetCardIsBlocked() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();

        mockCardAvailable(sourceCard);

        when(targetCard.getStatus())
                .thenReturn(CardStatus.BLOCKED);

        // Act and Assert
        assertThrows(
                CardBlockedException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenSourceCardHasExpiredStatus() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();

        when(sourceCard.getStatus())
                .thenReturn(CardStatus.EXPIRED);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertTrue(
                exception.getMessage().contains("истёк")
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenSourceCardExpirationDateHasPassed() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();

        when(sourceCard.getStatus())
                .thenReturn(CardStatus.ACTIVE);

        when(sourceCard.getExpirationDate())
                .thenReturn(LocalDate.now().minusDays(1));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertTrue(
                exception.getMessage().contains("истёк")
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenTargetCardExpirationDateHasPassed() {
        // Arrange
        TransferRequest request = validRequest();

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();

        mockCardAvailable(sourceCard);

        when(targetCard.getStatus())
                .thenReturn(CardStatus.ACTIVE);

        when(targetCard.getExpirationDate())
                .thenReturn(LocalDate.now().minusDays(1));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        // Assert
        assertTrue(
                exception.getMessage().contains("истёк")
        );

        verifyNoMoneyTransferred();
    }

    @Test
    void transfer_shouldThrowException_whenSourceCardHasInsufficientFunds() {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("500.00");

        TransferRequest request = new TransferRequest(
                SOURCE_CARD_ID,
                TARGET_CARD_ID,
                transferAmount
        );

        mockCardsFound();
        mockBothCardsOwnedByCurrentUser();
        mockCardAvailable(sourceCard);
        mockCardAvailable(targetCard);

        when(sourceCard.getBalance())
                .thenReturn(new BigDecimal("499.99"));

        // Act and Assert
        assertThrows(
                InsufficientFundsException.class,
                () -> transferService.transfer(USER_ID, request)
        );

        verifyNoMoneyTransferred();
    }

    private TransferRequest validRequest() {
        return new TransferRequest(
                SOURCE_CARD_ID,
                TARGET_CARD_ID,
                new BigDecimal("100.00")
        );
    }

    private void mockCardsFound() {
        when(cardService.getCardEntityById(SOURCE_CARD_ID))
                .thenReturn(sourceCard);

        when(cardService.getCardEntityById(TARGET_CARD_ID))
                .thenReturn(targetCard);
    }

    private void mockBothCardsOwnedByCurrentUser() {
        when(sourceCard.getUser())
                .thenReturn(sourceCardOwner);

        when(sourceCardOwner.getId())
                .thenReturn(USER_ID);

        when(targetCard.getUser())
                .thenReturn(targetCardOwner);

        when(targetCardOwner.getId())
                .thenReturn(USER_ID);
    }

    private void mockCardAvailable(Card card) {
        when(card.getStatus())
                .thenReturn(CardStatus.ACTIVE);

        when(card.getExpirationDate())
                .thenReturn(LocalDate.now().plusYears(1));
    }

    private void verifyNoMoneyTransferred() {
        verify(sourceCard, never())
                .withdraw(any(BigDecimal.class));

        verify(targetCard, never())
                .deposit(any(BigDecimal.class));
    }
}