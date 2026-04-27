package com.example.trellite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Boards this user OWNS
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private Set<Board> ownedBoards = new HashSet<>();

    // Boards this user is a MEMBER of
    @ManyToMany(mappedBy = "members")
    private Set<Board> memberBoards = new HashSet<>();

    // Tasks assigned to this user
    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL)
    private Set<Task> tasks = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

enum Role {
    USER,
    ADMIN
}
