package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private Role role;
}
