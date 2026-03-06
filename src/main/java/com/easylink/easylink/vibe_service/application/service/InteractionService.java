package com.easylink.easylink.vibe_service.application.service;

import com.easylink.easylink.services.NotificationService;
import com.easylink.easylink.vibe_service.application.dto.EarlyAccessRequestDTO;
import com.easylink.easylink.vibe_service.application.dto.InteractionWithOffersDTO;
import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.mapper.InteractionDtoMapper;
import com.easylink.easylink.vibe_service.application.mapper.VibeDtoMapper;
import com.easylink.easylink.vibe_service.application.port.in.interaction.*;
import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionStatus;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.EarlyAccessRequest;
import com.easylink.easylink.vibe_service.domain.model.SubscribeMode;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaEarlyAccessRequestAdapter;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaInteractionRepositoryAdapter;
import com.easylink.easylink.vibe_service.infrastructure.repository.SpringDataVibeRepository;
import com.easylink.easylink.vibe_service.web.dto.CreateInteractionRequest;
import com.easylink.easylink.vibe_service.web.dto.InteractionResponse;
import com.easylink.easylink.vibe_service.web.dto.PendingRequestResponse;
import com.easylink.easylink.vibe_service.web.mapper.InteractionResponseMapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractionService implements
        CreateInteractionUseCase,
        DeactivateInteractionUseCase,
        GetInteractionsByVibeUseCase,
        CreateEarlyAccessUseCase,
        EarlyAccessCheckable {

    private final SpringDataVibeRepository springDataVibeRepository;
    private final JpaInteractionRepositoryAdapter interactionRepositoryAdapter;
    private final JpaEarlyAccessRequestAdapter jpaEarlyAccessRequestAdapter;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;

    @Override
    public InteractionResponse createInteraction(CreateInteractionRequest req) {

        Vibe targetVibe = springDataVibeRepository
                .findById(req.getTargetVibeId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Target vibe not found"));

        Vibe myVibe = springDataVibeRepository
                .findById(req.getMyVibeId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscriber vibe not found"));

        if (targetVibe.getId().equals(myVibe.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot subscribe to your own vibe"
            );
        }

        InteractionType type = req.getInteractionType();
        if (type == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Interaction type is required"
            );
        }

        // =========================
        // SUBSCRIBE (upsert)
        // =========================
        if (type == InteractionType.SUBSCRIBE) {

            Interaction sub = interactionRepositoryAdapter
                    .findAnySubscription(myVibe, targetVibe)
                    .orElseGet(() -> {
                        Interaction i = new Interaction();
                        i.setTargetVibe(targetVibe);
                        i.setSubscriberVibe(myVibe);
                        i.setInteractionType(InteractionType.SUBSCRIBE);
                        return i;
                    });

            sub.setAnonymous(req.isAnonymous());
            sub.setUserEmail(req.getUserEmail());
            sub.setActive(true);

            sub.setAnonymous(req.isAnonymous());
            sub.setUserEmail(req.getUserEmail());
            sub.setActive(true);

            if (targetVibe.getSubscribeMode() == SubscribeMode.APPROVAL) {
                sub.setStatus(InteractionStatus.PENDING);
            } else {
                sub.setStatus(InteractionStatus.APPROVED);
            }

            Interaction saved = interactionRepositoryAdapter.save(sub);

            // Notification logic
            String ownerUserId = targetVibe.getVibeAccountId().toString();
            String subscriberName = (req.isAnonymous() ? "Someone" : myVibe.getName());

            String notificationType =
                    (sub.getStatus() == InteractionStatus.PENDING)
                            ? "ACCESS_REQUEST"
                            : "SUBSCRIBE";

            String title =
                    (sub.getStatus() == InteractionStatus.PENDING)
                            ? "New access request"
                            : "New subscriber";

            String message =
                    (sub.getStatus() == InteractionStatus.PENDING)
                            ? subscriberName + " requested access to your Vibe"
                            : subscriberName + " subscribed to your Vibe";

            notificationService.create(
                    ownerUserId,
                    notificationType,
                    title,
                    message,
                    "/view/" + myVibe.getId()
            );

            return InteractionResponseMapper.toInteractionResponse(
                    InteractionDtoMapper.toInteractionDto(saved)
            );
        }

        // =========================
        // UNSUBSCRIBE
        // =========================
        if (type == InteractionType.UNSUBSCRIBE) {

            Interaction sub = interactionRepositoryAdapter
                    .findAnySubscription(myVibe, targetVibe)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.CONFLICT, "Subscription not found"));

            sub.setActive(false);

            // STRICT model: leaving = revoke approval; next subscribe requires new request
            if (targetVibe.getSubscribeMode() == SubscribeMode.APPROVAL) {
                sub.setStatus(InteractionStatus.PENDING); // key change
            }

            Interaction saved = interactionRepositoryAdapter.save(sub);

            return InteractionResponseMapper.toInteractionResponse(
                    InteractionDtoMapper.toInteractionDto(saved)
            );
        }

        // =========================
        // OTHER INTERACTIONS
        // =========================
        Interaction interaction = new Interaction();
        interaction.setTargetVibe(targetVibe);
        interaction.setSubscriberVibe(myVibe);
        interaction.setAnonymous(req.isAnonymous());
        interaction.setUserEmail(req.getUserEmail());
        interaction.setInteractionType(type);
        interaction.setActive(req.isActive());

        Interaction saved = interactionRepositoryAdapter.save(interaction);

        return InteractionResponseMapper.toInteractionResponse(
                InteractionDtoMapper.toInteractionDto(saved)
        );
    }

    public List<VibeDto> getFollowing(UUID vibeId) {
        Vibe vibe = springDataVibeRepository.findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        List<Interaction> interactions = interactionRepositoryAdapter.getAllFollowings(vibe);
        List<Vibe> vibeList = interactions.stream().map(Interaction::getTargetVibe).collect(Collectors.toList());
        return vibeList.stream().map(VibeDtoMapper::toDto).toList();
    }

    public List<InteractionWithOffersDTO> getFollowingWithOffers(UUID vibeId) {
        Vibe vibe = springDataVibeRepository.findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        return interactionRepositoryAdapter.getAllFollowingsWithOffers(vibe);
    }

    public List<VibeDto> getSubscribers(UUID vibeId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        List<Interaction> interactions = interactionRepositoryAdapter.findApprovedSubscribersByTarget(targetVibe);

        return interactions.stream()
                .map(Interaction::getSubscriberVibe)
                .map(VibeDtoMapper::toDto)
                .toList();
    }

    public List<VibeDto> getSubscribeRequestsOwnedBy(UUID vibeId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        List<Interaction> interactions = interactionRepositoryAdapter.findPendingSubscribersByTarget(targetVibe);

        return interactions.stream()
                .map(Interaction::getSubscriberVibe)
                .map(VibeDtoMapper::toDto)
                .toList();
    }

    public List<VibeDto> getSubscribersOwnedBy(UUID vibeId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        List<Interaction> interactions = interactionRepositoryAdapter.findApprovedSubscribersByTarget(targetVibe);

        return interactions.stream()
                .map(Interaction::getSubscriberVibe)
                .map(VibeDtoMapper::toDto)
                .toList();
    }

    public InteractionResponse createInteractionFromJwt(
            CreateInteractionRequest req,
            String requesterUserId
    ) {
        Vibe myVibe = springDataVibeRepository
                .findById(req.getMyVibeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Subscriber vibe not found"));

        if (!myVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        return createInteraction(req);
    }

    public Boolean isSubscribed(UUID subscriberVibeId, UUID targetVibeId) {

        Vibe subscriberVibe = springDataVibeRepository.findById(subscriberVibeId)
                .orElseThrow(() -> new RuntimeException("Subscriber Vibe not found"));

        Vibe targetVibe = springDataVibeRepository.findById(targetVibeId)
                .orElseThrow(() -> new RuntimeException("Target Vibe not found"));

        return interactionRepositoryAdapter.isSubscribed(subscriberVibe, targetVibe);
    }

    @Override
    public EarlyAccessRequestDTO create(String email) {

        EarlyAccessRequest earlyAccessRequest = new EarlyAccessRequest();
        earlyAccessRequest.setCreatedAt(Instant.now());
        earlyAccessRequest.setApproved(true);
        earlyAccessRequest.setEmail(email);

        EarlyAccessRequest earlyAccessRequestSaved = jpaEarlyAccessRequestAdapter.save(earlyAccessRequest);

        return modelMapper.map(earlyAccessRequestSaved, EarlyAccessRequestDTO.class);
    }

    @Override
    public boolean isSubscribedEarlyAccess(String email) {
        return jpaEarlyAccessRequestAdapter.isSubscribedEarlyAccess(email);
    }

    // =========================================================================
    // REQUESTS (PENDING) for private vibe subscribe approval
    // =========================================================================

    public List<PendingRequestResponse> getPendingRequestsOwnedBy(UUID vibeId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        List<Interaction> interactions = interactionRepositoryAdapter.findPendingSubscribersByTarget(targetVibe);

        return interactions.stream()
                .map(i -> {
                    PendingRequestResponse dto = new PendingRequestResponse();
                    dto.setInteractionId(i.getId());

                    Vibe subscriberVibe = i.getSubscriberVibe();
                    if (subscriberVibe != null) {
                        dto.setSubscriberVibeId(subscriberVibe.getId());

                        if (i.isAnonymous()) {
                            dto.setSubscriberName("Someone");
                            dto.setSubscriberDescription("");
                            dto.setSubscriberType("");
                            dto.setSubscriberPhoto(null);
                        } else {
                            String name = subscriberVibe.getName();
                            if (name == null || name.isBlank()) {
                                name = "Someone";
                            }

                            dto.setSubscriberName(name);
                            dto.setSubscriberDescription(subscriberVibe.getDescription());
                            dto.setSubscriberType(
                                    subscriberVibe.getType() != null ? subscriberVibe.getType().name() : null
                            );
                            dto.setSubscriberPhoto(subscriberVibe.getPhoto());
                        }
                    }

                    dto.setAnonymous(i.isAnonymous());
                    return dto;
                })
                .toList();
    }

    public void approveRequest(UUID vibeId, UUID interactionId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        Interaction i = interactionRepositoryAdapter.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!i.getTargetVibe().getId().equals(vibeId) || i.getInteractionType() != InteractionType.SUBSCRIBE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
        }

        i.setStatus(InteractionStatus.APPROVED);
        i.setActive(true);
        interactionRepositoryAdapter.save(i);
    }

    public void rejectRequest(UUID vibeId, UUID interactionId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        Interaction i = interactionRepositoryAdapter.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!i.getTargetVibe().getId().equals(vibeId) || i.getInteractionType() != InteractionType.SUBSCRIBE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
        }

        i.setStatus(InteractionStatus.REJECTED);
        i.setActive(false);
        interactionRepositoryAdapter.save(i);
    }

    public void removeSubscriber(UUID targetVibeId, UUID subscriberVibeId, String requesterUserId) {
        Vibe targetVibe = springDataVibeRepository
                .findById(targetVibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        // only owner can remove
        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        Vibe subscriberVibe = springDataVibeRepository
                .findById(subscriberVibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscriber vibe not found"));

        Interaction sub = interactionRepositoryAdapter
                .findAnySubscription(subscriberVibe, targetVibe)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (sub.getInteractionType() != InteractionType.SUBSCRIBE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interaction type");
        }

        // STRICT model: revoke access + require approval next time
        sub.setActive(false);
        sub.setStatus(InteractionStatus.PENDING);

        interactionRepositoryAdapter.save(sub);
    }
}