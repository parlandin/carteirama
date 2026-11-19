package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.PasswordReset;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<PasswordReset> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("delete from PasswordReset p where p.expiresAt < :now")
    int deleteExpired(Instant now);
}
