package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.VerifyOtpRequest;
import com.example.auth.entity.User;
import com.example.auth.entity.VerificationToken;
import com.example.auth.exception.ApiException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final String baseUrl;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       VerificationTokenService tokenService,
                       EmailService emailService,
                       JwtUtil jwtUtil,
                       @Value("${app.base-url}") String baseUrl) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email already registered");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setEnabled(false);
        user = userRepo.save(user);

        VerificationToken token = tokenService.createActivationToken(user);
        String link = baseUrl + "/api/auth/activate?token=" + token.getToken();
        emailService.sendActivationEmail(user.getEmail(), link);
    }

    @Transactional
    public void activate(String token) {
        VerificationToken vt = tokenService.consumeActivationToken(token);
        User user = vt.getUser();
        user.setEnabled(true);
    }

    @Transactional
    public void login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> ApiException.unauthorized("BAD_CREDENTIALS", "Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw ApiException.unauthorized("BAD_CREDENTIALS", "Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("ACCOUNT_DISABLED", "Account not activated. Please check your email");
        }

        VerificationToken otp = tokenService.createLoginOtp(user);
        emailService.sendOtpEmail(user.getEmail(), otp.getToken());
    }

    @Transactional
    public LoginResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> ApiException.badRequest("OTP_NOT_FOUND", "No active OTP"));

        tokenService.verifyOtp(user, req.otp());

        user.setPreviousLoginAt(user.getLastLoginAt());
        user.setLastLoginAt(Instant.now());

        String jwt = jwtUtil.generate(user.getId().toString(), user.getEmail());
        return new LoginResponse(jwt, jwtUtil.getExpirationSeconds());
    }
}
