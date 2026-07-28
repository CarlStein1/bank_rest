package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardBlockRequest;
import com.example.bankcards.dto.request.ProcessCardBlockRequest;
import com.example.bankcards.dto.response.ApiErrorResponse;
import com.example.bankcards.dto.response.CardBlockRequestResponse;
import com.example.bankcards.entity.enums.CardBlockRequestStatus;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardBlockRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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
                    description = "Заявка успешно создана",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            CardBlockRequestResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Переданы некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = """
                            Карта принадлежит другому пользователю
                            или у пользователя недостаточно прав
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Карта уже заблокирована
                            или активная заявка уже существует
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<CardBlockRequestResponse> createBlockRequest(
            @AuthenticationPrincipal
            UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId,

            @Valid
            @RequestBody
            CreateCardBlockRequest request
    ) {
        CardBlockRequestResponse response =
                cardBlockRequestService.createBlockRequest(
                        principal.getId(),
                        cardId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

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
                    responseCode = "400",
                    description = "Передано неизвестное значение статуса",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<PagedModel<CardBlockRequestResponse>>
    getAllBlockRequests(
            @Parameter(
                    description = "Статус заявки",
                    example = "PENDING"
            )
            @RequestParam(required = false)
            CardBlockRequestStatus status,

            @ParameterObject
            Pageable pageable
    ) {
        Page<CardBlockRequestResponse> responsePage =
                cardBlockRequestService.getAllBlockRequests(
                        status,
                        pageable
                );

        return ResponseEntity.ok(
                new PagedModel<>(responsePage)
        );
    }

    @GetMapping("/admin/block-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить заявку",
            description = """
                    Возвращает заявку на блокировку
                    по её идентификатору.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Заявка успешно получена"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Заявка не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<CardBlockRequestResponse>
    getBlockRequestById(
            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                cardBlockRequestService.getBlockRequestById(
                        requestId
                )
        );
    }

    @PatchMapping("/admin/block-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Подтвердить заявку",
            description = """
                    Подтверждает заявку и блокирует
                    связанную с ней карту.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = """
                            Заявка подтверждена,
                            карта заблокирована
                            """
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Переданы некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Заявка, карта или администратор
                            не найдены
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Заявка уже была обработана
                            или карта не может быть заблокирована
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<CardBlockRequestResponse>
    approveBlockRequest(
            @AuthenticationPrincipal
            UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId,

            @Valid
            @RequestBody
            ProcessCardBlockRequest request
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
                    description = "Переданы некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                            Заявка или администратор
                            не найдены
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Заявка уже была обработана",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<CardBlockRequestResponse>
    rejectBlockRequest(
            @AuthenticationPrincipal
            UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор заявки",
                    example = "1"
            )
            @PathVariable Long requestId,

            @Valid
            @RequestBody
            ProcessCardBlockRequest request
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