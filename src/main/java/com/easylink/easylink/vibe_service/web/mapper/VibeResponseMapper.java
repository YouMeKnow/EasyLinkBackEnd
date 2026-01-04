package com.easylink.easylink.vibe_service.web.mapper;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.web.dto.MiniVibeResponse;
import com.easylink.easylink.vibe_service.web.dto.VibeResponse;

public class VibeResponseMapper {
    public static VibeResponse toResponse(VibeDto vibeDto){
        VibeResponse response = new VibeResponse();
        response.setId(vibeDto.getId());
        response.setDescription(vibeDto.getDescription());
        response.setName(vibeDto.getName());
        response.setVisible(vibeDto.getVisible());
        response.setPublicCode(vibeDto.getPublicCode());
        response.setType(vibeDto.getType());
        response.setFieldsDTO(vibeDto.getFieldsDTO());
        response.setPhoto(vibeDto.getPhoto());
        response.setSubscriberCount(vibeDto.getSubscriberCount());

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
        return response;

    }
}
