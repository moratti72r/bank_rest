package com.example.bankcards.util;

import com.example.bankcards.entity.RevokedTokenEntity;
import com.example.bankcards.repository.RevokedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${app.jwt-secret:myVeryStrongSecretKeyThatIsLongEnoughForHS512AndMeetsTheRFC7518Standard!}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds:86400000}")
    private long jwtExpirationMs;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getEmailFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            if (isTokenRevoked(authToken)) {
                return false;
            }

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            revokeIfPresent(authToken);
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    public void blacklistToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            Instant expiresAt = expiration.toInstant();

            if (expiresAt.isAfter(Instant.now())) {
                RevokedTokenEntity revokedToken = RevokedTokenEntity.builder()
                        .token(token)
                        .expiresAt(expiresAt)
                        .build();
                revokedTokenRepository.save(revokedToken);
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid JWT Token");
        }
    }

    private boolean isTokenRevoked(String token) {
        return revokedTokenRepository.findByToken(token).isPresent();
    }

    private void revokeIfPresent(String token) {
        revokedTokenRepository.findByToken(token)
                .ifPresent(revoked -> revokedTokenRepository.delete(revoked));
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }


}
