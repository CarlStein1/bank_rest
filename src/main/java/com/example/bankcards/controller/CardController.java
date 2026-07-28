package com.example.bankcards.controller;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Карты",
        description = "Создание, управление и просмотр банковских карт"
)
public class CardController {

    private final CardService cardService;

    // -------------------------------------------------------------------------
    // Операции администратора
    // -------------------------------------------------------------------------

    @PostMapping("/admin/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Создать карту",
            description = "Создаёт новую банковскую карту для указанного пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Карта успешно создана"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден"
            )
    })
    public ResponseEntity<CardResponse> createCard(
            @Parameter(
                    description = "Идентификатор владельца карты",
                    example = "1"
            )
            @PathVariable Long userId
    ) {
        CardResponse response = cardService.createCard(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/admin/cards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить все карты",
            description = "Возвращает страницу всех банковских карт"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница карт успешно получена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            )
    })
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                cardService.getAllCards(pageable)
        );
    }

    @GetMapping("/admin/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить карту",
            description = "Возвращает банковскую карту по её идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно получена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(
                cardService.getCardById(cardId)
        );
    }

    @PatchMapping("/admin/cards/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Заблокировать карту",
            description = "Блокирует выбранную банковскую карту"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно заблокирована"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(
                cardService.blockCard(cardId)
        );
    }

    @PatchMapping("/admin/cards/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Активировать карту",
            description = "Активирует выбранную банковскую карту"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно активирована"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(
                cardService.activateCard(cardId)
        );
    }

    @DeleteMapping("/admin/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Удалить карту",
            description = "Удаляет банковскую карту по её идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Карта успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<Void> deleteCard(
            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        cardService.deleteCard(cardId);

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Операции пользователя
    // -------------------------------------------------------------------------

    @GetMapping("/cards")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить свои карты",
            description = """
                    Возвращает страницу банковских карт,
                    принадлежащих текущему авторизованному пользователю.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница карт успешно получена"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            )
    })
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                cardService.getUserCards(
                        principal.getId(),
                        pageable
                )
        );
    }

    @GetMapping("/cards/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить свою карту",
            description = """
                    Возвращает карту только в том случае,
                    если она принадлежит текущему пользователю.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Карта успешно получена"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к чужой карте"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<CardResponse> getMyCard(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(
                cardService.getUserCard(
                        principal.getId(),
                        cardId
                )
        );
    }

    @GetMapping("/cards/{cardId}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Получить баланс своей карты",
            description = """
                    Возвращает баланс карты только в том случае,
                    если она принадлежит текущему пользователю.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Баланс успешно получен"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к чужой карте"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Карта не найдена"
            )
    })
    public ResponseEntity<BigDecimal> getMyCardBalance(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "Идентификатор карты",
                    example = "1"
            )
            @PathVariable Long cardId
    ) {
        return ResponseEntity.ok(
                cardService.getUserCardBalance(
                        principal.getId(),
                        cardId
                )
        );
    }
}