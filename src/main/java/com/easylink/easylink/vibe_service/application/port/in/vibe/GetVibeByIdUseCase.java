package com.easylink.easylink.vibe_service.application.port.in.vibe;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;

import java.util.UUID;

public interface GetVibeByIdUseCase {
    VibeDto getVibeById(UUID id, String viewerAccountId);
    default VibeDto getVibeById(UUID id) {
        return getVibeById(id, null);
    }
}