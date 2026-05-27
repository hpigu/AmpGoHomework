package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, activation, login, OTP verification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new account. Sends an activation email.")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful, please check your email to activate."));
    }

    @GetMapping("/activate")
    @Operation(summary = "Activate an account using the link emailed to the user.")
    public ResponseEntity<Map<String, String>> activate(@RequestParam String token) {
        authService.activate(token);
        return ResponseEntity.ok(Map.of("message", "Account activated. You can now log in."));
    }

    @PostMapping("/login")
    @Operation(summary = "Verify password and send an OTP to the user's email.")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        authService.login(req);
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and return a JWT.")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(authService.verifyOtp(req));
    }
}
