package com.easylink.easylink.vibe_service.application.port.out;

import com.easylink.easylink.vibe_service.application.dto.InteractionWithOffersDTO;
import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import java.util.List;
import java.util.UUID;

import java.util.List;

public interface InteractionRepositoryPort {
    Interaction save(Interaction interaction);
    List<Interaction> getAllFollowings(Vibe subscriberVibe);
    List<InteractionWithOffersDTO> getAllFollowingsWithOffers(Vibe subscriberVibe);

    long countActiveByTarget(UUID targetVibeId, InteractionType type);

    List<UUID> findActiveSubscriberVibeIdsForTargetAndSubscriberIn(
            UUID targetVibeId,
            InteractionType type,
            List<UUID> subscriberVibeIds
    );
}
