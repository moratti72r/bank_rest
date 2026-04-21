package com.example.bankcards.repository;

import com.example.bankcards.entity.RevokedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedTokenEntity, UUID> {
    Optional<RevokedTokenEntity> findByToken(String token);

    void deleteByExpiresAtBefore(Instant instant);
}