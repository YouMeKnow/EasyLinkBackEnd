package com.easylink.easylink.vibe_service.web.dto;

import com.easylink.easylink.vibe_service.domain.model.VibeType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MiniVibeResponse {
    private UUID id;
    private String name;
    private VibeType type;
    private String photo;
}
