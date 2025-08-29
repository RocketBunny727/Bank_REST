package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.dto.LoginResponseDTO;
import com.example.bankcards.dto.UserCreateDTO;
import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "API for login and signup")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "Create a new user", description = "Only for USER, ADMIN creating manually")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(@Valid @RequestBody UserCreateDTO body) {
        UserResponseDTO newUser = userService.createUser(body);
        return ResponseEntity.ok(newUser);
    }

    @Operation(summary = "Login in system", description = "USER and ADMIN takes auth tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logged successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO body) {
        return ResponseEntity.ok(authService.login(body));
    }
}
