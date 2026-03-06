package com.easylink.easylink.vibe_service.web.dto;

import com.easylink.easylink.vibe_service.domain.model.SubscribeMode;
import com.easylink.easylink.vibe_service.domain.model.VibePrivacy;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class UpdateVibeRequest {
    private String name;
    private String description;
    private List<UUID> fieldIds;
    private String photo;
    private List<VibeFieldDTO> fieldsDTO;
    private VibePrivacy privacy;
    private SubscribeMode subscribeMode;
}
