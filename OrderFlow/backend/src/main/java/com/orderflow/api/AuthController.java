package com.orderflow.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflow.api.dto.AuthDtos;
import com.orderflow.domain.Organization;
import com.orderflow.domain.User;
import com.orderflow.repository.OrganizationRepository;
import com.orderflow.repository.UserRepository;
import com.orderflow.security.JwtPrincipal;
import com.orderflow.security.JwtService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, OrganizationRepository organizationRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@RequestBody AuthDtos.LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }
        return userRepository.findByEmail(request.email().trim().toLowerCase())
            .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
            .map(this::authResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@RequestBody AuthDtos.RegisterRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password()) || isBlank(request.fullName())
                || isBlank(request.organizationName()) || isBlank(request.organizationSlug()) || request.password().length() < 8) {
            return ResponseEntity.badRequest().build();
        }
        String email = request.email().trim().toLowerCase();
        String slug = request.organizationSlug().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent() || organizationRepository.findBySlug(slug).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(request.organizationName().trim());
        organization.setSlug(slug);
        organization.setCreatedAt(Instant.now());
        organizationRepository.save(organization);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganizationId(organization.getId());
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole("ADMIN");
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(userRepository.save(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return userRepository.findById(UUID.fromString(principal.userId()))
                .map(this::userResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private AuthDtos.AuthResponse authResponse(User user) {
        return new AuthDtos.AuthResponse(
            jwtService.generateToken(user.getId().toString(), user.getRole(), user.getOrganizationId().toString()),
            userResponse(user)
        );
    }

    private AuthDtos.UserResponse userResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getOrganizationId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
