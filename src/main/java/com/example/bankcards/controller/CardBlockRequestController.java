package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardBlockRequest;
import com.example.bankcards.dto.request.ProcessCardBlockRequest;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardBlockRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Заявки на блокировку карт",
        description = """
                Создание заявок пользователями
                и обработка заявок администраторами
                """
)
public class CardBlockRequestController {

    private final CardBlockRequestService cardBlockRequestService;

    // -------------------------------------------------------------------------
    // Операции пользователя
    // -------------------------------------------------------------------------

    @PostMapping("/cards/{cardId}/block-requests")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Создать заявку на блокировку карты",
            description = """
                    Создаёт заявку на блокировку карты,
                    принадлежащей текущему пользователю.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Заявка успешно создана"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Переданы некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Карта принадлежит другому пользователю"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Карта уже заблокирована
                            или активная заявка уже существует
                            """
            )
    })
    public ResponseEntity<CardBlockRequest> createBlockRequest(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId,

            @Valid @RequestBody CreateCardBlockRequest request
    ) {
        CardBlockRequest response =
                cardBlockRequestService.createBlockRequest(
                        principal.getId(),
                        cardId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // -------------------------------------------------------------------------
    // Операции администратора
    // -------------------------------------------------------------------------

    @GetMapping("/admin/block-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить заявки на блокировку",
            description = """
                    Возвращает страницу заявок на блокировку карт.
                    Поддерживает фильтрацию по статусу и пагинацию.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница заявок успешно получена"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            )
    })
    public ResponseEntity<Page<CardBlockRequest>> getAllBlockRequests(
            @Parameter(
                    description = "Статус заявки",
                    example = "PENDING"
            )
            @RequestParam(required = false)
            CardBlockRequestStatus status,

            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                cardBlockRequestService.getAllBlockRequests(
                        status,
                        pageable
                )
        );
    }

    @GetMapping("/admin/block-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить заявку",
            description = "Возвращает заявку на блокировку по идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Заявка успешно получена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заявка не найдена"
            )
    })
    public ResponseEntity<CardBlockRequest> getBlockRequestById(
            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                cardBlockRequestService.getBlockRequestById(requestId)
        );
    }

    @PatchMapping("/admin/block-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Подтвердить заявку",
            description = """
                    Подтверждает заявку и блокирует связанную с ней карту.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Заявка подтверждена, карта заблокирована"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Переданы некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заявка не найдена"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Заявка уже была обработана"
            )
    })
    public ResponseEntity<CardBlockRequest> approveBlockRequest(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId,

            @Valid @RequestBody ProcessCardBlockRequest request
    ) {
        return ResponseEntity.ok(
                cardBlockRequestService.approveBlockRequest(
                        principal.getId(),
                        requestId,
                        request
                )
        );
    }

    @PatchMapping("/admin/block-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Отклонить заявку",
            description = """
                    Отклоняет заявку на блокировку.
                    Статус карты при этом не изменяется.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Заявка успешно отклонена"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Переданы некорректные данные"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заявка не найдена"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Заявка уже была обработана"
            )
    })
    public ResponseEntity<CardBlockRequest> rejectBlockRequest(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId,

            @Valid @RequestBody ProcessCardBlockRequest request
    ) {
        return ResponseEntity.ok(
                cardBlockRequestService.rejectBlockRequest(
                        principal.getId(),
                        requestId,
                        request
                )
        );
    }
}