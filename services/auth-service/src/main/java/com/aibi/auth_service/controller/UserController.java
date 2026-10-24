package com.aibi.auth_service.controller;

import com.aibi.auth_service.dto.UpdateUserRoleRequest;
import com.aibi.auth_service.dto.UserSummaryResponse;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Map<String, String> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "Authentification JWT réussie");

        userRepository.findByEmail(email).ifPresent(user -> {
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            response.put("userId", String.valueOf(user.getId()));
        });

        return response;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<UserSummaryResponse> users = userRepository.findAll().stream()
                .map(u -> new UserSummaryResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        User user = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        String requestedRole = request.role().trim().toUpperCase();
        if (requestedRole.startsWith("ROLE_")) {
            requestedRole = requestedRole.substring(5);
        }

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(requestedRole);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rôle invalide. Rôles autorisés : USER, ADMIN");
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);

        return ResponseEntity.ok(new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole().name(),
                saved.getCreatedAt()
        ));
    }
}
