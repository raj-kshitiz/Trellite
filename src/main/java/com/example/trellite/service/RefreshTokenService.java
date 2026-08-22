package com.example.trellite.service;

import com.example.trellite.exception.InvalidRefreshTokenException;
import com.example.trellite.exception.ResourceNotFoundException;
import com.example.trellite.model.RefreshToken;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.RefreshTokenRepo;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final AuthRepo authRepo;
    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${jwt.refresh-expiration-days}")
    long refreshExpirationDays;

    /**
     * How long a just-rotated token still answers, so simultaneous refreshes from the same
     * client do not log the user out. Defaulted here rather than required in properties.
     */
    @Value("${jwt.refresh-grace-seconds:30}")
    long refreshGraceSeconds;

    public RefreshTokenService(AuthRepo authRepo, RefreshTokenRepo refreshTokenRepo) {
        this.authRepo = authRepo;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    public RefreshToken createRefreshToken(String username) {
        User user = authRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return createRefreshToken(user);
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .user(user)
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Please sign in again"));

        if (refreshToken.isRotated()) {
            throw new InvalidRefreshTokenException("Please sign in again");
        }
        if (refreshToken.isExpired()) {
            refreshTokenRepo.delete(refreshToken);
            throw new InvalidRefreshTokenException("Your session has expired — please sign in again");
        }
        return refreshToken;
    }

    /**
     * Verifies a refresh token and replaces it. Without rotation a single leaked refresh
     * token stays usable for its whole seven days, however many times it is replayed.
     *
     * <p>The old row is marked rather than deleted, which matters for two reasons:
     * <ul>
     *   <li><b>Concurrent refreshes.</b> Two browser tabs restoring a session at the same
     *       moment both send the stored token. Deleting on first use meant the second call
     *       got a 401 and logged the user out of both. Inside the grace window the replay
     *       is answered with the same replacement token, so it is idempotent.</li>
     *   <li><b>Theft detection.</b> A rotated token presented *after* the grace window
     *       should not happen for a well-behaved client, so it is treated as a leaked
     *       token: every refresh token for that user is revoked, ending both the attacker's
     *       session and the victim's, which is the point.</li>
     * </ul>
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String token) {
        RefreshToken current = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Please sign in again"));

        if (current.isRotated()) {
            return replayRotation(current);
        }
        if (current.isExpired()) {
            refreshTokenRepo.delete(current);
            throw new InvalidRefreshTokenException("Your session has expired — please sign in again");
        }

        RefreshToken replacement = createRefreshToken(current.getUser());
        current.setRotatedAt(LocalDateTime.now());
        current.setReplacedByToken(replacement.getToken());
        refreshTokenRepo.save(current);
        return replacement;
    }

    private RefreshToken replayRotation(RefreshToken rotated) {
        boolean withinGrace = rotated.getRotatedAt()
                .isAfter(LocalDateTime.now().minusSeconds(refreshGraceSeconds));

        if (withinGrace && rotated.getReplacedByToken() != null) {
            Optional<RefreshToken> replacement = refreshTokenRepo.findByToken(rotated.getReplacedByToken());
            if (replacement.isPresent() && !replacement.get().isExpired() && !replacement.get().isRotated()) {
                return replacement.get();
            }
        }

        // Outside the window, or the replacement is gone: assume the token leaked and end
        // every session for this user rather than let a thief keep refreshing.
        log.warn("Rotated refresh token presented again outside the grace window for user id {} — revoking all sessions",
                rotated.getUser().getId());
        refreshTokenRepo.deleteByUser(rotated.getUser());
        throw new InvalidRefreshTokenException("Please sign in again");
    }

    @Transactional
    public void deleteTokensForUser(String username) {
        User user = authRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        refreshTokenRepo.deleteByUser(user);
    }
}
