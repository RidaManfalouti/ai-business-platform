package com.aibi.auth_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Map<String, String> getCurrentUser(
            Authentication authentication) {

        return Map.of(
                "email", authentication.getName(),
                "message", "Authentification JWT réussie"
        );
    }
}
