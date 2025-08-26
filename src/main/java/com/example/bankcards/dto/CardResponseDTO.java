package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CardResponseDTO {
    private Long id;
    private String maskedNumber;
    private String owner;
    private LocalDate expiryDate;
    private CardStatus status;
    private double balance;
    private boolean isBlockRequested;
}
