package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CardCreateDTO {
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{4} \\d{4} \\d{4} \\d{4}", message = "Invalid card number format")
    private String cardNumber;

    @NotNull(message = "Expiry date is required")
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Invalid expiry date format")
    private String expiryDate;

    @NotNull(message = "User is required")
    private Long userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double balance;
}
