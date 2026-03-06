package com.easylink.easylink.vibe_service.web.mapper;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.web.dto.MiniVibeResponse;
import com.easylink.easylink.vibe_service.web.dto.VibeResponse;

public class VibeResponseMapper {
    public static VibeResponse toResponse(VibeDto vibeDto) {
        VibeResponse response = new VibeResponse();
        response.setId(vibeDto.getId());
        response.setDescription(vibeDto.getDescription());
        response.setName(vibeDto.getName());
        response.setVisible(vibeDto.getVisible());
        response.setPublicCode(vibeDto.getPublicCode());
        response.setType(vibeDto.getType());
        response.setPhoto(vibeDto.getPhoto());

        response.setSubscriberCount(vibeDto.getSubscriberCount());
        response.setFollowingCount(vibeDto.getFollowingCount());
        response.setOwner(vibeDto.isOwner());

        response.setPrivacy(vibeDto.getPrivacy());
        response.setSubscribeMode(vibeDto.getSubscribeMode());
        response.setHasAccess(vibeDto.isHasAccess());
        response.setMySubscriptionStatus(vibeDto.getMySubscriptionStatus());

        boolean canSeePrivateContent =
                vibeDto.isOwner() || vibeDto.isHasAccess(); // owner OR approved subscriber

        if (canSeePrivateContent) {
            response.setFieldsDTO(vibeDto.getFieldsDTO());
            if (vibeDto.getSubscriberVibes() != null) {
                response.setSubscriberVibes(
                        vibeDto.getSubscriberVibes().stream().map(v -> {
                            MiniVibeResponse r = new MiniVibeResponse();
                            r.setId(v.getId());
                            r.setName(v.getName());
                            r.setType(v.getType());
                            r.setPhoto(v.getPhoto());
                            return r;
                        }).toList()
                );
            }
        } else {
            response.setFieldsDTO(null);        // или List.of()
            response.setSubscriberVibes(null);  // чтобы не палить followers
        }

        return response;
    }
}
