package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.entity.VerificationToken;
import com.example.auth.entity.VerificationToken.Type;
import com.example.auth.exception.ApiException;
import com.example.auth.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class VerificationTokenService {

    private static final Duration ACTIVATION_TTL = Duration.ofHours(24);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final VerificationTokenRepository repo;
    private final SecureRandom random = new SecureRandom();

    public VerificationTokenService(VerificationTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public VerificationToken createActivationToken(User user) {
        VerificationToken t = new VerificationToken();
        t.setUser(user);
        t.setToken(UUID.randomUUID().toString());
        t.setType(Type.ACTIVATION);
        t.setExpiresAt(Instant.now().plus(ACTIVATION_TTL));
        return repo.save(t);
    }

    @Transactional
    public VerificationToken createLoginOtp(User user) {
        VerificationToken t = new VerificationToken();
        t.setUser(user);
        t.setToken(String.format("%06d", random.nextInt(1_000_000)));
        t.setType(Type.LOGIN_OTP);
        t.setExpiresAt(Instant.now().plus(OTP_TTL));
        return repo.save(t);
    }

    @Transactional
    public VerificationToken consumeActivationToken(String token) {
        VerificationToken vt = repo.findByTokenAndType(token, Type.ACTIVATION)
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Activation token invalid"));
        if (vt.isUsed()) throw ApiException.badRequest("TOKEN_USED", "Activation token already used");
        if (vt.isExpired()) throw ApiException.badRequest("TOKEN_EXPIRED", "Activation token expired");
        vt.setUsed(true);
        return vt;
    }

    @Transactional
    public VerificationToken verifyOtp(User user, String otp) {
        VerificationToken vt = repo
                .findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, Type.LOGIN_OTP)
                .orElseThrow(() -> ApiException.badRequest("OTP_NOT_FOUND", "No active OTP. Please login again"));

        if (vt.isExpired()) {
            vt.setUsed(true);
            throw ApiException.badRequest("OTP_EXPIRED", "OTP has expired");
        }
        if (vt.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            vt.setUsed(true);
            throw ApiException.badRequest("OTP_MAX_ATTEMPTS", "Too many attempts. Please login again");
        }

        if (!vt.getToken().equals(otp)) {
            vt.setAttemptCount(vt.getAttemptCount() + 1);
            if (vt.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
                vt.setUsed(true);
            }
            throw ApiException.badRequest("OTP_INVALID", "Incorrect OTP");
        }

        vt.setUsed(true);
        return vt;
    }
}
