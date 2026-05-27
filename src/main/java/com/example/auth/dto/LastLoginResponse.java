package com.example.auth.dto;

import java.time.Instant;

public record LastLoginResponse(Instant lastLoginAt, String message) {}
