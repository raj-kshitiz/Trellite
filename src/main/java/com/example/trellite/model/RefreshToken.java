package com.example.trellite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @ToString.Exclude
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // The user this refresh token belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /**
     * When this token was rotated away, or null while it is still the live one. The row is
     * kept for a short grace period after rotation rather than deleted outright, so that a
     * client which fires two refreshes with the same token — two browser tabs restoring a
     * session at the same moment, or a retried request — gets the replacement instead of
     * being logged out. After the grace window, presenting a rotated token is treated as
     * a stolen-token signal.
     */
    private LocalDateTime rotatedAt;

    /** The token issued in this one's place, so a replay inside the grace window can be answered. */
    @ToString.Exclude
    private String replacedByToken;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }
}
