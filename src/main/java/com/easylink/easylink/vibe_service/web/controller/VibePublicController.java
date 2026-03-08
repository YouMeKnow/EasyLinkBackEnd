package com.easylink.easylink.vibe_service.web.controller;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.port.in.vibe.GetVibeByIdUseCase;
import com.easylink.easylink.vibe_service.web.dto.VibeResponse;
import com.easylink.easylink.vibe_service.web.mapper.VibeResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v3/vibes/public")
@RequiredArgsConstructor
public class VibePublicController {

    private final GetVibeByIdUseCase getVibeByIdUseCase;

    @Operation(summary = "Get public vibe", description = "Get public Vibe profile using ID")
    @GetMapping("/{id}")
    public ResponseEntity<VibeResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String viewerAccountId = (jwt != null ? jwt.getSubject() : null);

        VibeDto vibeDto = getVibeByIdUseCase.getPublicVibeById(id, viewerAccountId);

        return ResponseEntity.ok(VibeResponseMapper.toResponse(vibeDto));
    }
}