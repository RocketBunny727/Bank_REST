package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponseDTO {
    private long sourceId;
    private long destinationId;
    private String maskedSourceCardNumber;
    private String maskedDestinationCardNumber;
    private double sourceBalance;
    private double destinationBalance;
    private String owner;
}
