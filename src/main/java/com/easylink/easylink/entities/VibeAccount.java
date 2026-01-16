package com.easylink.easylink.entities;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
public class VibeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    @OneToMany(mappedBy = "vibeAccount", cascade = CascadeType.ALL, orphanRemoval = true,  fetch = FetchType.LAZY)
    private List<AssociativeEntry> associativeEntries;

    private String passwordHash; // nullable
    private LocalDateTime created;
    private LocalDateTime lastLogin;
    private int failedAttempts;
    private Instant lockTime;
    private Boolean isEmailVerified = false;
    private String emailVerificationToken;
    private LocalDateTime tokenExpiry;
    private Instant lastFailedAt;
}
