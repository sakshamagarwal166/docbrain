package com.docbrain.service;

import com.docbrain.dto.AuthResponse;
import com.docbrain.dto.LoginRequest;
import com.docbrain.dto.RegisterRequest;
import com.docbrain.exception.BadRequestException;
import com.docbrain.model.User;
import com.docbrain.repository.UserRepository;
import com.docbrain.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        tokenProvider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac!!", 86400000);
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = authService.register(request);

        assertNotNull(response.getToken());
        assertEquals("test@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));
        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertNotNull(response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void login_wrongPassword_throws() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void register_tokenIsValid() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = authService.register(request);

        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals("test@example.com", tokenProvider.getEmailFromToken(response.getToken()));
    }
}
