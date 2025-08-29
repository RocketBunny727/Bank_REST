package com.example.bankcards.controller;

import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User viewing", description = "API for viewing users information (Only for ADMIN)")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Find user info by ID", description = "Returns one user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Find users info by filters", description = "Returns page of users by request params")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved by filter successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No user found")
    })
    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> filterUsers(
            @PageableDefault(sort = "id") Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "User ID") Long id,
            @RequestParam(required = false) @Parameter(description = "User username") String username,
            @RequestParam(required = false) @Parameter(description = "User name") String name,
            @RequestParam(required = false) @Parameter(description = "User surname") String surname
            ) {
        return ResponseEntity.ok(userService.getUsersByFilter(pageable, id, username, name, surname));
    }
}
