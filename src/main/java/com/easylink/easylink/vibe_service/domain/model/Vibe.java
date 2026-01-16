package com.easylink.easylink.vibe_service.domain.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@SQLDelete(sql = "UPDATE vibe SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Setter
@Getter
public class Vibe {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID vibeAccountId;

    private String description;

    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VibeType type;

    @Column(name = "photo")
    private String photo;

    @Column(nullable = true)
    private Boolean visible;

    @OneToMany(mappedBy = "vibe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    private String publicCode;

    private LocalDateTime codeGeneratedAt;

    @OneToMany(mappedBy = "vibe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VibeField> fields = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
