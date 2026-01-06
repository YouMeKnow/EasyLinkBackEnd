package com.easylink.easylink.vibe_service.infrastructure.repository;

import com.easylink.easylink.vibe_service.application.dto.InteractionWithOffersDTO;
import com.easylink.easylink.vibe_service.application.port.out.InteractionRepositoryPort;
import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaInteractionRepositoryAdapter implements InteractionRepositoryPort {

    private final SpringDataInteraction delegateRepository;

    public JpaInteractionRepositoryAdapter(SpringDataInteraction springDataInteraction) {
        this.delegateRepository = springDataInteraction;
    }

    @Override
    public Interaction save(Interaction interaction) {
        return delegateRepository.save(interaction);
    }

    @Override
    public List<Interaction> getAllFollowings(Vibe subscriberVibe) {
        return delegateRepository.findAllBySubscriberVibe(subscriberVibe);
    }

    @Override
    public List<InteractionWithOffersDTO> getAllFollowingsWithOffers(Vibe subscriberVibe) {
        List<Object[]> rows = delegateRepository.findAllBySubscriberVibeWithOffers(subscriberVibe);

        return rows.stream()
                .map(arr -> new InteractionWithOffersDTO((Interaction) arr[0], ((Long) arr[1]).intValue()))
                .toList();
    }

    // SUBSCRIBE + active=true
    public boolean isSubscribed(Vibe subscriberVibe, Vibe targetVibe) {
        return delegateRepository.existsBySubscriberVibeAndTargetVibeAndInteractionTypeAndActiveTrue(
                subscriberVibe,
                targetVibe,
                InteractionType.SUBSCRIBE
        );
    }

    public Optional<Interaction> findAnySubscription(Vibe subscriberVibe, Vibe targetVibe) {
        return delegateRepository.findFirstBySubscriberVibeAndTargetVibeAndInteractionTypeOrderByIdDesc(
                subscriberVibe, targetVibe, InteractionType.SUBSCRIBE
        );
    }

    public Optional<Interaction> findActiveSubscriptionOpt(Vibe subscriberVibe, Vibe targetVibe) {
        return delegateRepository.findFirstBySubscriberVibeAndTargetVibeAndInteractionTypeAndActiveTrueOrderByIdDesc(
                subscriberVibe, targetVibe, InteractionType.SUBSCRIBE
        );
    }

    public Interaction findActiveSubscription(Vibe subscriberVibe, Vibe targetVibe) {
        return delegateRepository
                .findFirstBySubscriberVibeAndTargetVibeAndInteractionTypeAndActiveTrueOrderByIdDesc(
                        subscriberVibe,
                        targetVibe,
                        InteractionType.SUBSCRIBE
                )
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }

    public List<Interaction> findActiveSubscribersByTarget(Vibe targetVibe) {
        return delegateRepository.findByTargetVibeAndInteractionTypeAndActiveTrue(targetVibe, InteractionType.SUBSCRIBE);
    }

    @Override
    public long countActiveByTarget(UUID targetVibeId, InteractionType type) {
        if (targetVibeId == null || type == null) return 0L;
        return delegateRepository.countActiveByTarget(targetVibeId, type);
    }

    @Override
    public List<UUID> findActiveSubscriberVibeIdsForTargetAndSubscriberIn(
            UUID targetVibeId,
            InteractionType type,
            List<UUID> subscriberVibeIds
    ) {
        if (targetVibeId == null || type == null) return List.of();
        if (subscriberVibeIds == null || subscriberVibeIds.isEmpty()) return List.of();
        return delegateRepository.findActiveSubscriberVibeIdsForTargetAndSubscriberIn(
                targetVibeId, type, subscriberVibeIds
        );
    }
}
