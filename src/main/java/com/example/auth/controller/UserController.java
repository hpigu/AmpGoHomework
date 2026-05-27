package com.example.auth.controller;

import com.example.auth.dto.LastLoginResponse;
import com.example.auth.entity.User;
import com.example.auth.exception.ApiException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "User self-service endpoints")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/me/last-login")
    @Operation(summary = "Get the caller's previous login time (the one before the current session).")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<LastLoginResponse> getLastLogin(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication required");
        }
        User user = userRepo.findById(UUID.fromString(principal.userId()))
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (user.getPreviousLoginAt() == null) {
            return ResponseEntity.ok(new LastLoginResponse(null, "首次登入"));
        }
        return ResponseEntity.ok(new LastLoginResponse(user.getPreviousLoginAt(), null));
    }
}
