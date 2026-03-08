package com.easylink.easylink.vibe_service.application.port.in.vibe;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;

import java.util.UUID;

public interface GetVibeByIdUseCase {

    // owner page (/vibes/{id})
    VibeDto getOwnedVibeById(UUID id, UUID accountId);

    // public page (/view/{id})
    VibeDto getPublicVibeById(UUID id, String viewerAccountId);

}