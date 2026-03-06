package com.easylink.easylink.vibe_service.web.controller;

import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.service.InteractionService;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionStatus;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.SubscribeMode;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import com.easylink.easylink.vibe_service.infrastructure.repository.SpringDataInteraction;
import com.easylink.easylink.vibe_service.infrastructure.repository.SpringDataVibeRepository;
import com.easylink.easylink.vibe_service.web.dto.PendingRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v3/vibes")
@RequiredArgsConstructor
public class VibeAccessController {

    private final InteractionService interactionService;
    private final SpringDataVibeRepository springDataVibeRepository;
    private final SpringDataInteraction springDataInteraction;

    @GetMapping("/{id}/subscribers")
    public ResponseEntity<List<VibeDto>> subscribers(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Vibe targetVibe = springDataVibeRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        // PUBLIC vibe (без approval) → видно всем
        if (targetVibe.getSubscribeMode() != SubscribeMode.APPROVAL) {
            return ResponseEntity.ok(interactionService.getSubscribers(id));
        }

        // PRIVATE (APPROVAL) → owner или approved subscriber
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String requesterUserId = jwt.getSubject();

        // owner
        if (targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            return ResponseEntity.ok(interactionService.getSubscribers(id));
        }

        // subscriber: проверяем, что у текущего аккаунта есть APPROVED подписка на этот target vibe
        UUID accountId;
        try {
            accountId = UUID.fromString(requesterUserId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject");
        }

        List<UUID> myVibeIds = springDataVibeRepository.findAliveIdsByVibeAccountId(accountId);
        if (myVibeIds == null || myVibeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access");
        }

        boolean approved = springDataInteraction.existsSubscriptionByStatus(
                id,
                InteractionType.SUBSCRIBE,
                InteractionStatus.APPROVED,
                myVibeIds
        );

        if (!approved) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access");
        }

        return ResponseEntity.ok(interactionService.getSubscribers(id));
    }

    // остальное можешь оставить как было (requests/approve/reject/remove)
    @GetMapping("/{id}/requests")
    public ResponseEntity<List<PendingRequestResponse>> requests(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        return ResponseEntity.ok(interactionService.getPendingRequestsOwnedBy(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/requests/{interactionId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable UUID id,
            @PathVariable UUID interactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        interactionService.approveRequest(id, interactionId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/requests/{interactionId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID id,
            @PathVariable UUID interactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        interactionService.rejectRequest(id, interactionId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/subscribers/{subscriberVibeId}")
    public ResponseEntity<Void> removeSubscriber(
            @PathVariable UUID id,
            @PathVariable UUID subscriberVibeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        interactionService.removeSubscriber(id, subscriberVibeId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}