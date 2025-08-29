package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
}
