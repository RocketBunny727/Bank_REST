package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Card Management", description = "API for managing bank cards")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Create new card", description = "Only for ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card create successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card data"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    public ResponseEntity<CardResponseDTO> createCard(@RequestBody CardCreateDTO dto) {
        return ResponseEntity.ok(cardService.createCard(dto));
    }

    @Operation(summary = "Find card by ID", description = "ADMIN can see any cards, USER can see only own cards")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDTO> findCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @Operation(summary = "Get list of available all cards", description = "Admin can see all cards of all customers," +
            " USER can only see list of their own cards")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Cards not found")
    })
    @GetMapping("/all")
    public ResponseEntity<Page<CardResponseDTO>> findAllCards(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @RequestParam(required = false) String status){
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @Operation(summary = "Get filtered list of cards", description = "Admin can see all cards of all customers," +
            " USER can only see list of their own cards by filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Cards not found")
    })
    @GetMapping
    public ResponseEntity<Page<CardResponseDTO>> filterCards(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "Card user ID") Long userId,
            @RequestParam(required = false) @Parameter(description = "Card number (16 digits)") String number,
            @RequestParam(required = false) @Parameter(description = "Card status (ACTIVE, BLOCKED, EXPIRED)") CardStatus status,
            @RequestParam(required = false) @Parameter(description = "Card owner name (Name + Surname)") String owner,
            @RequestParam(required = false) @Parameter(description = "Card balance") Double balance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Card expiry date") LocalDate expiryDate,
            @RequestParam(required = false) @Parameter(description = "Has the card been block requested") boolean isBlockRequested,
            @RequestParam(required = false) @Parameter(description = "Minimal card balance you looking for") Double minBalance,
            @RequestParam(required = false) @Parameter(description = "Maximal card balance you looking for") Double maxBalance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Earlier card expiry date you looking for") LocalDate expiryDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Latest card expiry date you looking for") LocalDate expiryDateTo
            ) {
        return ResponseEntity.ok(cardService.getFilteredCards(
                pageable,
                userId,
                number,
                status,
                owner,
                balance,
                expiryDate,
                isBlockRequested,
                minBalance,
                maxBalance,
                expiryDateFrom,
                expiryDateTo
        ));
    }

    @Operation(summary = "Block card", description = "ADMIN can block any card, USER can only request for block " +
            "their own card")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PutMapping("/{id}/block")
    public ResponseEntity<CardResponseDTO> blockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @Operation(summary = "Block card", description = "Only for ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card unblocked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @PutMapping("/{id}/unblock")
    public ResponseEntity<CardResponseDTO> unblockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.unblockCard(id));
    }

    @Operation(summary = "Delete card", description = "Only for ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised user"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
