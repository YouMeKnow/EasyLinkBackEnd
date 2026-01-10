package com.easylink.easylink.vibe_service.infrastructure.repository;

import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInteraction extends JpaRepository<Interaction, UUID> {

    List<Interaction> findAllBySubscriberVibe(Vibe subscriberVibe);

    Optional<Interaction> findBySubscriberVibeAndTargetVibeAndInteractionType(
            Vibe subscriberVibe,
            Vibe targetVibe,
            InteractionType interactionType
    );

    boolean existsBySubscriberVibeAndTargetVibeAndInteractionTypeAndActiveTrue(
            Vibe subscriberVibe,
            Vibe targetVibe,
            InteractionType interactionType
    );

    @Query("""
        SELECT i, COUNT(o)
        FROM Interaction i
        join i.targetVibe tv
        LEFT JOIN Offer o ON o.vibe = tv AND o.endTime >= CURRENT_TIMESTAMP
        WHERE i.subscriberVibe = :subscriberVibe
          and i.active = true
          and i.interactionType = com.easylink.easylink.vibe_service.domain.interaction.InteractionType.SUBSCRIBE
          and tv.deletedAt is null
        GROUP BY i
    """)
    List<Object[]> findAllBySubscriberVibeWithOffers(@Param("subscriberVibe") Vibe subscriberVibe);

    @Query("""
        select count(i)
        from Interaction i
        join i.targetVibe tv
        where i.active = true
          and i.interactionType = :type
          and tv.id = :targetId
          and tv.deletedAt is null
    """)

    long countActiveByTarget(@Param("targetId") UUID targetId, @Param("type") InteractionType type);

    @Query("""
        select i.subscriberVibe.id
        from Interaction i
        join i.targetVibe tv
        join i.subscriberVibe sv
        where i.active = true
          and i.interactionType = :type
          and tv.id = :targetId
          and tv.deletedAt is null
          and sv.deletedAt is null
          and sv.id in :subscriberIds
    """)
    List<UUID> findActiveSubscriberVibeIdsForTargetAndSubscriberIn(
            @Param("targetId") UUID targetId,
            @Param("type") InteractionType type,
            @Param("subscriberIds") List<UUID> subscriberIds
    );

    Optional<Interaction> findFirstBySubscriberVibeAndTargetVibeAndInteractionTypeOrderByIdDesc(
            Vibe subscriberVibe,
            Vibe targetVibe,
            InteractionType interactionType
    );

    Optional<Interaction> findFirstBySubscriberVibeAndTargetVibeAndInteractionTypeAndActiveTrueOrderByIdDesc(
            Vibe subscriberVibe,
            Vibe targetVibe,
            InteractionType interactionType
    );

    List<Interaction> findByTargetVibeAndInteractionTypeAndActiveTrue(Vibe targetVibe, InteractionType type);

    @Query("""
        select i
        from Interaction i
        join i.targetVibe tv
        where i.subscriberVibe = :subscriberVibe
          and i.active = true
          and i.interactionType = com.easylink.easylink.vibe_service.domain.interaction.InteractionType.SUBSCRIBE
          and tv.deletedAt is null
    """)
    List<Interaction> findActiveFollowingsAlive(@Param("subscriberVibe") Vibe subscriberVibe);

    @Query("""
        select i
        from Interaction i
        join i.subscriberVibe sv
        where i.targetVibe = :targetVibe
          and i.active = true
          and i.interactionType = com.easylink.easylink.vibe_service.domain.interaction.InteractionType.SUBSCRIBE
          and sv.deletedAt is null
    """)
    List<Interaction> findActiveSubscribersAlive(@Param("targetVibe") Vibe targetVibe);
}
