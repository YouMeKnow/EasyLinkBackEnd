package com.easylink.easylink.vibe_service.application.port.in.vibe;

import com.easylink.easylink.vibe_service.application.dto.MiniVibeDto;
import java.util.List;
import java.util.UUID;

public interface GetFollowingUseCase {
    List<MiniVibeDto> getFollowing(UUID vibeId);
}