package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CardResponseDTO {
    private Long id;
    private String maskedNumber;
    private String owner;
    private String expiryDate;
    private CardStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double balance;

    private boolean isBlockRequested;
}
