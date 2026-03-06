package com.easylink.easylink.vibe_service.domain.interaction;

import com.easylink.easylink.vibe_service.domain.model.Vibe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Interaction {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="target_vibe_id")
    private Vibe targetVibe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="subscriber_vibe_id")
    private Vibe subscriberVibe;

    private String userEmail;

    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;

    private LocalDateTime createdAt;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private InteractionStatus status;
}
