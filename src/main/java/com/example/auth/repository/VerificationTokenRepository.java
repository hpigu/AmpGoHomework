package com.example.auth.repository;

import com.example.auth.entity.User;
import com.example.auth.entity.VerificationToken;
import com.example.auth.entity.VerificationToken.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenAndType(String token, Type type);

    Optional<VerificationToken> findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(User user, Type type);
}
