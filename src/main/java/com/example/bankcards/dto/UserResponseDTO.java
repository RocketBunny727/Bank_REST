package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private long id;
    private String username;
    private String name;
    private String surname;
}
