package com.easylink.easylink.vibe_service.application.mapper;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import com.easylink.easylink.vibe_service.domain.model.VibeField;
import com.easylink.easylink.vibe_service.web.dto.VibeFieldDTO;

import java.util.List;

public class VibeDtoMapper {

    public static VibeDto toDto(Vibe vibe) {
        VibeDto vibeDto = new VibeDto();

        vibeDto.setId(vibe.getId());
        vibeDto.setName(vibe.getName());
        vibeDto.setDescription(vibe.getDescription());
        vibeDto.setType(vibe.getType());

        vibeDto.setVisible(vibe.getVisible());
        vibeDto.setPublicCode(vibe.getPublicCode());
        vibeDto.setPhoto(vibe.getPhoto());

        // privacy fields
        vibeDto.setPrivacy(vibe.getPrivacy());
        vibeDto.setSubscribeMode(vibe.getSubscribeMode());

        List<VibeFieldDTO> vibeFieldDTOS = vibe.getFields().stream()
                .map(val -> new VibeFieldDTO(
                        val.getId(),
                        val.getType(),
                        val.getValue(),
                        val.getLabel(),
                        val.getVibe() != null ? val.getVibe().getId() : null
                ))
                .toList();

        vibeDto.setFieldsDTO(vibeFieldDTOS);

        return vibeDto;
    }

    private static VibeFieldDTO toDto(VibeField entity) {
        return new VibeFieldDTO(
                entity.getId(),
                entity.getType(),
                entity.getValue(),
                entity.getLabel(),
                entity.getVibe() != null ? entity.getVibe().getId() : null
        );
    }
}