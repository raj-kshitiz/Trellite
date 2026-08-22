package com.example.trellite.service;

import com.example.trellite.exception.InvalidRefreshTokenException;
import com.example.trellite.model.RefreshToken;
import com.example.trellite.model.User;
import com.example.trellite.repository.AuthRepo;
import com.example.trellite.repository.RefreshTokenRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock AuthRepo authRepo;
    @Mock RefreshTokenRepo refreshTokenRepo;

    RefreshTokenService service;

    User user = User.builder().id(1L).username("alice").build();

    /** Stands in for the refresh_tokens table. */
    Map<String, RefreshToken> stored = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(authRepo, refreshTokenRepo);
        service.refreshExpirationDays = 7;
        service.refreshGraceSeconds = 30;

        lenient().when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(call -> {
            RefreshToken saved = call.getArgument(0);
            stored.put(saved.getToken(), saved);
            return saved;
        });
        lenient().when(refreshTokenRepo.findByToken(anyString()))
                .thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0, String.class))));
    }

    private RefreshToken liveToken() {
        return service.createRefreshToken(user);
    }

    @Test
    void rotationIssuesANewTokenAndRetiresTheOld() {
        RefreshToken original = liveToken();

        RefreshToken rotated = service.rotateRefreshToken(original.getToken());

        assertThat(rotated.getToken()).isNotEqualTo(original.getToken());
        assertThat(original.isRotated()).isTrue();
        assertThat(original.getReplacedByToken()).isEqualTo(rotated.getToken());
    }

    @Test
    void twoSimultaneousRefreshesWithTheSameTokenBothSucceed() {
        // Two browser tabs restoring a session at the same moment. Deleting the old row on
        // first use made the second call a 401, which logged the user out of both.
        RefreshToken original = liveToken();

        RefreshToken first = service.rotateRefreshToken(original.getToken());
        RefreshToken second = service.rotateRefreshToken(original.getToken());

        assertThat(second.getToken()).isEqualTo(first.getToken());
        verify(refreshTokenRepo, never()).deleteByUser(any(User.class));
    }

    @Test
    void replayingARotatedTokenAfterTheGraceWindowRevokesEverySession() {
        RefreshToken original = liveToken();
        service.rotateRefreshToken(original.getToken());
        // Push the rotation outside the window.
        original.setRotatedAt(LocalDateTime.now().minusSeconds(31));

        assertThatThrownBy(() -> service.rotateRefreshToken(original.getToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // A token that surfaces this late is assumed leaked: end the attacker's session
        // and the victim's alike.
        verify(refreshTokenRepo).deleteByUser(user);
    }

    @Test
    void anExpiredTokenIsRejectedAndRemoved() {
        RefreshToken original = liveToken();
        original.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> service.rotateRefreshToken(original.getToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepo).delete(original);
    }

    @Test
    void anUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.rotateRefreshToken("never-issued"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("sign in again");
    }

    @Test
    void aRotatedTokenIsNoLongerAcceptedByVerify() {
        RefreshToken original = liveToken();
        service.rotateRefreshToken(original.getToken());

        assertThatThrownBy(() -> service.verifyRefreshToken(original.getToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
