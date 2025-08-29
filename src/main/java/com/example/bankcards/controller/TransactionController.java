package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransactionResponseDTO;
import com.example.bankcards.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transaction management", description = "API for making transaction between bank cards")
@RestController
@RequestMapping("/api/transact")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Make transaction", description = "USER can make transaction between their own cards")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success transaction"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Card for transaction not found")
    })
    @PutMapping
    public ResponseEntity<TransactionResponseDTO> makeTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(transactionService.transfer(transactionDTO));
    }
}
