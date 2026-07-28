package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.dto.response.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Переводы",
        description = "Переводы денежных средств между собственными картами"
)
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Перевести средства между своими картами",
            description = """
                    Выполняет перевод между двумя картами,
                    принадлежащими текущему авторизованному пользователю.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description =
                            "Перевод успешно выполнен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        Некорректная сумма или выбрана
                        одна и та же карта
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description =
                            "Пользователь не авторизован",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = """
                        Одна из карт принадлежит
                        другому пользователю
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description =
                            "Одна из карт не найдена",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        Недостаточно средств, карта заблокирована
                        или срок её действия истёк
                        """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            ApiErrorResponse.class
                            )
                    )
            )
    })
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(
                transferService.transfer(
                        principal.getId(),
                        request
                )
        );
    }
}