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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardBlockRequestServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long CARD_ID = 10L;
    private static final Long REQUEST_ID = 20L;

    @Mock
    private CardBlockRequestRepository blockRequestRepository;

    @Mock
    private CardService cardService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CardBlockRequestService blockRequestService;

    @Test
    void createBlockRequest_shouldCreateRequest_whenDataIsValid() {
        // Arrange
        Card card = mock(Card.class);
        CreateCardBlockRequest request =
                mock(CreateCardBlockRequest.class);

        when(cardService.getUserCardEntity(USER_ID, CARD_ID))
                .thenReturn(card);

        when(card.isBlocked())
                .thenReturn(false);

        when(blockRequestRepository.existsByCard_IdAndStatus(
                CARD_ID,
                CardBlockRequestStatus.PENDING
        )).thenReturn(false);

        when(request.reason())
                .thenReturn("  Карта потеряна  ");

        when(blockRequestRepository.save(
                any(CardBlockRequest.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardBlockRequest result =
                blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                );

        // Assert
        assertNotNull(result);
        assertSame(card, result.getCard());

        verify(cardService)
                .getUserCardEntity(USER_ID, CARD_ID);

        verify(card).isBlocked();

        verify(blockRequestRepository)
                .existsByCard_IdAndStatus(
                        CARD_ID,
                        CardBlockRequestStatus.PENDING
                );

        verify(request).reason();

        verify(blockRequestRepository)
                .save(any(CardBlockRequest.class));

        verifyNoInteractions(userService);
    }

    @Test
    void createBlockRequest_shouldThrowException_whenCardIsBlocked() {
        // Arrange
        Card card = mock(Card.class);
        CreateCardBlockRequest request =
                mock(CreateCardBlockRequest.class);

        when(cardService.getUserCardEntity(USER_ID, CARD_ID))
                .thenReturn(card);

        when(card.isBlocked())
                .thenReturn(true);

        // Act and Assert
        assertThrows(
                CardBlockedException.class,
                () -> blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                )
        );

        verify(cardService)
                .getUserCardEntity(USER_ID, CARD_ID);

        verify(card).isBlocked();

        verify(blockRequestRepository, never())
                .existsByCard_IdAndStatus(
                        anyLong(),
                        any(CardBlockRequestStatus.class)
                );

        verify(blockRequestRepository, never())
                .save(any(CardBlockRequest.class));

        verifyNoInteractions(request);
    }

    @Test
    void createBlockRequest_shouldThrowException_whenPendingRequestAlreadyExists() {
        // Arrange
        Card card = mock(Card.class);
        CreateCardBlockRequest request =
                mock(CreateCardBlockRequest.class);

        when(cardService.getUserCardEntity(USER_ID, CARD_ID))
                .thenReturn(card);

        when(card.isBlocked())
                .thenReturn(false);

        when(blockRequestRepository.existsByCard_IdAndStatus(
                CARD_ID,
                CardBlockRequestStatus.PENDING
        )).thenReturn(true);

        // Act and Assert
        assertThrows(
                DuplicateBlockRequestException.class,
                () -> blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                )
        );

        verify(blockRequestRepository)
                .existsByCard_IdAndStatus(
                        CARD_ID,
                        CardBlockRequestStatus.PENDING
                );

        verify(blockRequestRepository, never())
                .save(any(CardBlockRequest.class));

        verifyNoInteractions(request);
    }

    @Test
    void getAllBlockRequests_shouldReturnAllRequests_whenStatusIsNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Page<CardBlockRequest> expectedPage =
                Page.empty(pageable);

        when(blockRequestRepository.findAll(pageable))
                .thenReturn(expectedPage);

        // Act
        Page<CardBlockRequest> result =
                blockRequestService.getAllBlockRequests(
                        null,
                        pageable
                );

        // Assert
        assertSame(expectedPage, result);

        verify(blockRequestRepository).findAll(pageable);

        verify(blockRequestRepository, never())
                .findAllByStatus(
                        any(CardBlockRequestStatus.class),
                        any(Pageable.class)
                );
    }

    @Test
    void getAllBlockRequests_shouldFilterRequests_whenStatusIsSpecified() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Page<CardBlockRequest> expectedPage =
                Page.empty(pageable);

        when(blockRequestRepository.findAllByStatus(
                CardBlockRequestStatus.PENDING,
                pageable
        )).thenReturn(expectedPage);

        // Act
        Page<CardBlockRequest> result =
                blockRequestService.getAllBlockRequests(
                        CardBlockRequestStatus.PENDING,
                        pageable
                );

        // Assert
        assertSame(expectedPage, result);

        verify(blockRequestRepository)
                .findAllByStatus(
                        CardBlockRequestStatus.PENDING,
                        pageable
                );

        verify(blockRequestRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void getBlockRequestById_shouldReturnRequest_whenRequestExists() {
        // Arrange
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        // Act
        CardBlockRequest result =
                blockRequestService.getBlockRequestById(REQUEST_ID);

        // Assert
        assertSame(blockRequest, result);

        verify(blockRequestRepository).findById(REQUEST_ID);
    }

    @Test
    void getBlockRequestById_shouldThrowException_whenRequestDoesNotExist() {
        // Arrange
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.empty());

        // Act
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> blockRequestService.getBlockRequestById(
                        REQUEST_ID
                )
        );

        // Assert
        assertEquals(
                "Заявка на блокировку с id "
                        + REQUEST_ID
                        + " не найдена",
                exception.getMessage()
        );

        verify(blockRequestRepository).findById(REQUEST_ID);
    }

    @Test
    void approveBlockRequest_shouldApproveRequestAndBlockCard() {
        // Arrange
        User admin = mock(User.class);
        Card card = mock(Card.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        when(request.adminComment())
                .thenReturn("  Заявка одобрена  ");

        when(blockRequest.getCard())
                .thenReturn(card);

        // Act
        CardBlockRequest result =
                blockRequestService.approveBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                );

        // Assert
        assertSame(blockRequest, result);

        verify(userService)
                .getUserEntityById(ADMIN_ID);

        verify(blockRequestRepository)
                .findById(REQUEST_ID);

        /*
         * Проверяем, что пробелы в начале и конце комментария
         * были удалены.
         */
        verify(blockRequest).approve(
                admin,
                "Заявка одобрена"
        );

        verify(card).block();

        InOrder inOrder = inOrder(blockRequest, card);

        inOrder.verify(blockRequest).approve(
                admin,
                "Заявка одобрена"
        );

        inOrder.verify(blockRequest).getCard();
        inOrder.verify(card).block();

        verify(blockRequestRepository, never())
                .save(any(CardBlockRequest.class));

        verifyNoInteractions(cardService);
    }

    @Test
    void approveBlockRequest_shouldPassNull_whenAdminCommentIsBlank() {
        // Arrange
        User admin = mock(User.class);
        Card card = mock(Card.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        when(request.adminComment())
                .thenReturn("   ");

        when(blockRequest.getCard())
                .thenReturn(card);

        // Act
        blockRequestService.approveBlockRequest(
                ADMIN_ID,
                REQUEST_ID,
                request
        );

        // Assert
        verify(blockRequest).approve(admin, null);
        verify(card).block();
    }

    @Test
    void rejectBlockRequest_shouldRejectRequestAndNotBlockCard() {
        // Arrange
        User admin = mock(User.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        when(request.adminComment())
                .thenReturn("  Недостаточно информации  ");

        // Act
        CardBlockRequest result =
                blockRequestService.rejectBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                );

        // Assert
        assertSame(blockRequest, result);

        verify(userService)
                .getUserEntityById(ADMIN_ID);

        verify(blockRequestRepository)
                .findById(REQUEST_ID);

        verify(blockRequest).reject(
                admin,
                "Недостаточно информации"
        );

        /*
         * При отклонении заявки связанная карта
         * вообще не должна извлекаться или блокироваться.
         */
        verify(blockRequest, never()).getCard();

        verify(blockRequestRepository, never())
                .save(any(CardBlockRequest.class));

        verifyNoInteractions(cardService);
    }

    @Test
    void rejectBlockRequest_shouldPassNull_whenAdminCommentIsNull() {
        // Arrange
        User admin = mock(User.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        when(request.adminComment())
                .thenReturn(null);

        // Act
        CardBlockRequest result =
                blockRequestService.rejectBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                );

        // Assert
        assertSame(blockRequest, result);

        verify(blockRequest).reject(admin, null);
        verify(blockRequest, never()).getCard();
    }

    @Test
    void approveBlockRequest_shouldThrowException_whenRequestDoesNotExist() {
        // Arrange
        User admin = mock(User.class);
        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                NoSuchElementException.class,
                () -> blockRequestService.approveBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                )
        );

        verify(userService)
                .getUserEntityById(ADMIN_ID);

        verify(blockRequestRepository)
                .findById(REQUEST_ID);

        verifyNoInteractions(request);
        verifyNoInteractions(cardService);
    }

    @Test
    void getBlockRequestEntityById_shouldReturnEntity_whenRequestExists() {
        // Arrange
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        // Act
        CardBlockRequest result =
                blockRequestService.getBlockRequestEntityById(
                        REQUEST_ID
                );

        // Assert
        assertSame(blockRequest, result);

        verify(blockRequestRepository).findById(REQUEST_ID);
    }
}