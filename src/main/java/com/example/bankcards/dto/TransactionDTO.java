package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionDTO {
    @NotNull(message = "Source card ID is required")
    private Long fromCardId;

    @NotNull(message = "Destination card ID is required")
    private Long toCardId;

    @Positive(message = "Amount must be positive")
    private double amount;
}
