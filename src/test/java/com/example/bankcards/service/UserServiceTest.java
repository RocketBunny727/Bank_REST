package com.example.bankcards.service;

import com.example.bankcards.dto.UserCreateDTO;
import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DataNotFilledException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded_password")
                .name("John")
                .surname("Doe")
                .role(Role.USER)
                .build();
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createUser_success() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("john_doe")
                .password("password")
                .name("John")
                .surname("Doe")
                .build();
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO response = userService.createUser(dto);

        assertEquals(1L, response.getId());
        assertEquals("john_doe", response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_usernameExists_throwsUsernameAlreadyExistsException() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("john_doe")
                .password("password")
                .name("John")
                .surname("Doe")
                .build();
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.createUser(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_missingFields_throwsDataNotFilledException() {
        UserCreateDTO dto = UserCreateDTO.builder()
                .username("john_doe")
                .password(null)
                .name("John")
                .surname("Doe")
                .build();

        assertThrows(DataNotFilledException.class, () -> userService.createUser(dto));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    void getUserById_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getById(1L);

        assertEquals(1L, response.getId());
        assertEquals("john_doe", response.getUsername());
    }

    @Test
    void getUserByUsername_success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("john_doe");

        assertEquals("john_doe", result.getUsername());
    }

    @Test
    void getUserByUsername_notFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("john_doe"));
    }

    @Test
    void loadUserByUsername_success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("john_doe");

        assertEquals("john_doe", userDetails.getUsername());
        assertEquals("encoded_password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("john_doe"));
    }
}
