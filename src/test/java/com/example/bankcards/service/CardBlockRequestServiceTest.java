package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardBlockRequest;
import com.example.bankcards.dto.request.ProcessCardBlockRequest;
import com.example.bankcards.dto.response.CardBlockRequestResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        Card card = mock(Card.class);
        CreateCardBlockRequest request =
                mock(CreateCardBlockRequest.class);

        when(cardService.getUserCardEntity(USER_ID, CARD_ID))
                .thenReturn(card);
        when(card.isBlocked())
                .thenReturn(false);
        when(card.getId())
                .thenReturn(CARD_ID);
        when(blockRequestRepository.existsByCard_IdAndStatus(
                CARD_ID,
                CardBlockRequestStatus.PENDING
        )).thenReturn(false);
        when(request.reason())
                .thenReturn("  Карта потеряна  ");
        when(blockRequestRepository.save(
                any(CardBlockRequest.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        CardBlockRequestResponse result =
                blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                );

        assertNotNull(result);
        assertEquals(CARD_ID, result.cardId());
        assertEquals(
                CardBlockRequestStatus.PENDING,
                result.status()
        );
        assertEquals("Карта потеряна", result.reason());

        verify(cardService)
                .getUserCardEntity(USER_ID, CARD_ID);
        verify(card).isBlocked();
        verify(blockRequestRepository)
                .existsByCard_IdAndStatus(
                        CARD_ID,
                        CardBlockRequestStatus.PENDING
                );
        verify(blockRequestRepository)
                .save(any(CardBlockRequest.class));
        verifyNoInteractions(userService);
    }

    @Test
    void createBlockRequest_shouldThrowException_whenCardIsBlocked() {
        Card card = mock(Card.class);
        CreateCardBlockRequest request =
                mock(CreateCardBlockRequest.class);

        when(cardService.getUserCardEntity(USER_ID, CARD_ID))
                .thenReturn(card);
        when(card.isBlocked())
                .thenReturn(true);

        assertThrows(
                CardBlockedException.class,
                () -> blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                )
        );

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

        assertThrows(
                DuplicateBlockRequestException.class,
                () -> blockRequestService.createBlockRequest(
                        USER_ID,
                        CARD_ID,
                        request
                )
        );

        verify(blockRequestRepository, never())
                .save(any(CardBlockRequest.class));
        verifyNoInteractions(request);
    }

    @Test
    void getAllBlockRequests_shouldReturnAllRequests_whenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);
        Card card = mock(Card.class);

        stubBlockRequestEntity(
                blockRequest,
                card,
                null,
                CardBlockRequestStatus.PENDING
        );

        when(blockRequestRepository.findAll(pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(blockRequest),
                                pageable,
                                1
                        )
                );

        Page<CardBlockRequestResponse> result =
                blockRequestService.getAllBlockRequests(
                        null,
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                REQUEST_ID,
                result.getContent().get(0).id()
        );

        verify(blockRequestRepository).findAll(pageable);
        verify(blockRequestRepository, never())
                .findAllByStatus(
                        any(CardBlockRequestStatus.class),
                        any(Pageable.class)
                );
    }

    @Test
    void getAllBlockRequests_shouldFilterRequests_whenStatusIsSpecified() {
        Pageable pageable = PageRequest.of(0, 10);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);
        Card card = mock(Card.class);

        stubBlockRequestEntity(
                blockRequest,
                card,
                null,
                CardBlockRequestStatus.PENDING
        );

        when(blockRequestRepository.findAllByStatus(
                CardBlockRequestStatus.PENDING,
                pageable
        )).thenReturn(
                new PageImpl<>(
                        List.of(blockRequest),
                        pageable,
                        1
                )
        );

        Page<CardBlockRequestResponse> result =
                blockRequestService.getAllBlockRequests(
                        CardBlockRequestStatus.PENDING,
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                CardBlockRequestStatus.PENDING,
                result.getContent().get(0).status()
        );

        verify(blockRequestRepository)
                .findAllByStatus(
                        CardBlockRequestStatus.PENDING,
                        pageable
                );
        verify(blockRequestRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void getBlockRequestById_shouldReturnResponse_whenRequestExists() {
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);

        Card card = mock(Card.class);
        User processedBy = mock(User.class);

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 7, 28, 12, 0);

        LocalDateTime processedAt =
                LocalDateTime.of(2026, 7, 28, 12, 30);

        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));

        when(blockRequest.getId())
                .thenReturn(REQUEST_ID);

        when(blockRequest.getCard())
                .thenReturn(card);

        when(card.getId())
                .thenReturn(CARD_ID);

        when(blockRequest.getStatus())
                .thenReturn(CardBlockRequestStatus.APPROVED);

        when(blockRequest.getReason())
                .thenReturn("Карта потеряна");

        when(blockRequest.getCreatedAt())
                .thenReturn(createdAt);

        when(blockRequest.getProcessedAt())
                .thenReturn(processedAt);

        when(blockRequest.getProcessedBy())
                .thenReturn(processedBy);

        when(processedBy.getId())
                .thenReturn(ADMIN_ID);

        when(blockRequest.getAdminComment())
                .thenReturn("Заявка подтверждена");

        CardBlockRequestResponse result =
                blockRequestService.getBlockRequestById(
                        REQUEST_ID
                );

        assertEquals(REQUEST_ID, result.id());
        assertEquals(CARD_ID, result.cardId());
        assertEquals(
                CardBlockRequestStatus.APPROVED,
                result.status()
        );
        assertEquals(
                "Карта потеряна",
                result.reason()
        );
        assertEquals(createdAt, result.createdAt());
        assertEquals(processedAt, result.processedAt());
        assertEquals(ADMIN_ID, result.processedById());
        assertEquals(
                "Заявка подтверждена",
                result.adminComment()
        );

        verify(blockRequestRepository)
                .findById(REQUEST_ID);
    }

    @Test
    void getBlockRequestById_shouldThrowException_whenRequestDoesNotExist() {
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> blockRequestService.getBlockRequestById(
                        REQUEST_ID
                )
        );

        assertEquals(
                "Заявка на блокировку с id "
                        + REQUEST_ID
                        + " не найдена",
                exception.getMessage()
        );
    }

    @Test
    void approveBlockRequest_shouldApproveRequestAndBlockCard() {
        User admin = mock(User.class);
        Card card = mock(Card.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);
        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        stubBlockRequestEntity(
                blockRequest,
                card,
                admin,
                CardBlockRequestStatus.APPROVED
        );

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));
        when(request.adminComment())
                .thenReturn("  Заявка одобрена  ");

        CardBlockRequestResponse result =
                blockRequestService.approveBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                );

        assertEquals(
                CardBlockRequestStatus.APPROVED,
                result.status()
        );
        assertEquals(ADMIN_ID, result.processedById());

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
        User admin = mock(User.class);
        Card card = mock(Card.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);
        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        stubBlockRequestEntity(
                blockRequest,
                card,
                admin,
                CardBlockRequestStatus.APPROVED
        );

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));
        when(request.adminComment())
                .thenReturn("   ");

        blockRequestService.approveBlockRequest(
                ADMIN_ID,
                REQUEST_ID,
                request
        );

        verify(blockRequest).approve(admin, null);
        verify(card).block();
    }

    @Test
    void rejectBlockRequest_shouldRejectRequestAndNotBlockCard() {
        User admin = mock(User.class);
        Card card = mock(Card.class);
        CardBlockRequest blockRequest =
                mock(CardBlockRequest.class);
        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        stubBlockRequestEntity(
                blockRequest,
                card,
                admin,
                CardBlockRequestStatus.REJECTED
        );

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(blockRequest));
        when(request.adminComment())
                .thenReturn("  Недостаточно информации  ");

        CardBlockRequestResponse result =
                blockRequestService.rejectBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                );

        assertEquals(
                CardBlockRequestStatus.REJECTED,
                result.status()
        );

        verify(blockRequest).reject(
                admin,
                "Недостаточно информации"
        );
        verify(card, never()).block();
        verifyNoInteractions(cardService);
    }

    @Test
    void approveBlockRequest_shouldThrowException_whenRequestDoesNotExist() {
        User admin = mock(User.class);
        ProcessCardBlockRequest request =
                mock(ProcessCardBlockRequest.class);

        when(userService.getUserEntityById(ADMIN_ID))
                .thenReturn(admin);
        when(blockRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> blockRequestService.approveBlockRequest(
                        ADMIN_ID,
                        REQUEST_ID,
                        request
                )
        );

        verifyNoInteractions(request);
        verifyNoInteractions(cardService);
    }

    private CardBlockRequest getBlockRequestEntityById(
            Long requestId
    ) {
        return blockRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Заявка на блокировку с id "
                                        + requestId
                                        + " не найдена"
                        )
                );
    }

    private void stubBlockRequestEntity(
            CardBlockRequest blockRequest,
            Card card,
            User processedBy,
            CardBlockRequestStatus status
    ) {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 7, 28, 12, 0);

        LocalDateTime processedAt =
                processedBy == null
                        ? null
                        : LocalDateTime.of(
                        2026,
                        7,
                        28,
                        12,
                        30
                );

        when(blockRequest.getId())
                .thenReturn(REQUEST_ID);

        when(blockRequest.getCard())
                .thenReturn(card);

        when(card.getId())
                .thenReturn(CARD_ID);

        when(blockRequest.getStatus())
                .thenReturn(status);

        when(blockRequest.getReason())
                .thenReturn("Карта потеряна");

        when(blockRequest.getCreatedAt())
                .thenReturn(createdAt);

        when(blockRequest.getProcessedAt())
                .thenReturn(processedAt);

        when(blockRequest.getProcessedBy())
                .thenReturn(processedBy);

        when(blockRequest.getAdminComment())
                .thenReturn(
                        processedBy == null
                                ? null
                                : "Комментарий администратора"
                );

        if (processedBy != null) {
            when(processedBy.getId())
                    .thenReturn(ADMIN_ID);
        }
    }
}
