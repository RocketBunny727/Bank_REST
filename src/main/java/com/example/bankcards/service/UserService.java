package com.example.bankcards.service;

import com.example.bankcards.dto.UserCreateDTO;
import com.example.bankcards.dto.UserResponseDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.DataNotFilledException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.IUserRepository;
import com.example.bankcards.repository.UserSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public UserResponseDTO createUser(UserCreateDTO dto) {
        logger.info("Attempting to create user: username={}", dto.getUsername());

        if (dto.getUsername() == null || dto.getUsername().isEmpty()
                || dto.getPassword() == null || dto.getPassword().isEmpty()
                || dto.getName() == null || dto.getName().isEmpty()
                || dto.getSurname() == null || dto.getSurname().isEmpty()) {
            logger.error("Signup fields are missing");
            throw new DataNotFilledException("Not all fields are filled in");
        }

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            logger.error("Username already exists: username={}", dto.getUsername());
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .name(dto.getName())
                .surname(dto.getSurname())
                .build();

        user = userRepository.save(user);
        logger.info("User created successfully: userId={}, username={}", user.getId(), user.getUsername());

        return toResponseDTO(user);
    }

    @Transactional
    public User getUserById(Long id) {
        logger.info("Fetching user by id: id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found: id={}", id);
                    return new UserNotFoundException("User with id '" + id + "' not found");
                });
        logger.info("User retrieved successfully: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Transactional
    public UserResponseDTO getById(Long id) {
        logger.info("Fetching user by id (tried to find from controller): id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found: id={} (tried to find from controller)", id);
                    return new UserNotFoundException("User with id '" + id + "' not found");
                });
        logger.info("User retrieved successfully: userId={}, username={} (tried to find from controller)", user.getId(), user.getUsername());
        return toResponseDTO(user);
    }

    @Transactional
    public User getUserByUsername(String username) {
        logger.info("Fetching user by username: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: username={}", username);
                    return new UserNotFoundException("User with username: '" + username + "' not found");
                });
        logger.info("User retrieved successfully (by username): userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Loading user details for authentication: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found for authentication: username={}", username);
                    return new UsernameNotFoundException("User with username: '" + username + "' not found");
                });
        logger.debug("User details loaded: username={}, role={}", user.getUsername(), user.getRole());
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
                .build();
    }

    @Transactional
    public Page<UserResponseDTO> getUsersByFilter(
            Pageable pageable,
            Long id,
            String username,
            String name,
            String surname
    ) {
        logger.info("Fetching users with filter: id={}, username={}, name={}, surname={}, pageable={}",
                id, username, name, surname, pageable);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = getUserByUsername(auth.getName());
        logger.debug("Authenticated user: username={}, role={}", currentUser.getUsername(), currentUser.getRole());

        if (!Objects.equals(currentUser.getRole(), Role.ADMIN)) {
            logger.error("Access denied: user={} is not ADMIN", currentUser.getUsername());
            throw new AccessDeniedException("Only ADMIN can view the users information");
        }

        Page<User> users = userRepository.findAll(UserSpecifications.withFilters(id, username, name, surname), pageable);
        logger.info("Retrieved {} users with filter", users.getTotalElements());
        return users.map(this::toResponseDTO);
    }

    private UserResponseDTO toResponseDTO(User user) {
        logger.debug("Converting user to response DTO: userId={}, username={}", user.getId(), user.getUsername());
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .surname(user.getSurname())
                .role(user.getRole())
                .build();
    }
}
