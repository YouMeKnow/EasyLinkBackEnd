package com.easylink.easylink.vibe_service.application.service;

import com.easylink.easylink.services.NotificationService;
import com.easylink.easylink.vibe_service.application.dto.EarlyAccessRequestDTO;
import com.easylink.easylink.vibe_service.application.dto.InteractionWithOffersDTO;
import com.easylink.easylink.vibe_service.application.dto.VibeDto;
import com.easylink.easylink.vibe_service.application.mapper.InteractionDtoMapper;
import com.easylink.easylink.vibe_service.application.mapper.VibeDtoMapper;
import com.easylink.easylink.vibe_service.application.port.in.interaction.*;
import com.easylink.easylink.vibe_service.domain.interaction.Interaction;
import com.easylink.easylink.vibe_service.domain.interaction.InteractionType;
import com.easylink.easylink.vibe_service.domain.model.EarlyAccessRequest;
import com.easylink.easylink.vibe_service.domain.model.Vibe;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaEarlyAccessRequestAdapter;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaInteractionRepositoryAdapter;
import com.easylink.easylink.vibe_service.infrastructure.repository.SpringDataVibeRepository;
import com.easylink.easylink.vibe_service.web.dto.CreateInteractionRequest;
import com.easylink.easylink.vibe_service.web.dto.InteractionResponse;
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
public class InteractionService implements CreateInteractionUseCase, DeactivateInteractionUseCase, GetInteractionsByVibeUseCase, CreateEarlyAccessUseCase, EarlyAccessCheckable {

    private final SpringDataVibeRepository springDataVibeRepository;
    private final JpaInteractionRepositoryAdapter interactionRepositoryAdapter;
    private final JpaEarlyAccessRequestAdapter jpaEarlyAccessRequestAdapter;
    private final ModelMapper modelMapper;

    private final NotificationService notificationService;

    @Override
    public InteractionResponse createInteraction(CreateInteractionRequest req) {

        Vibe targetVibe = springDataVibeRepository
                .findById(req.getTargetVibeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target vibe not found"));

        Vibe myVibe = springDataVibeRepository
                .findById(req.getMyVibeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscriber vibe not found"));

        if (targetVibe.getId().equals(myVibe.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot subscribe to your own vibe");
        }

        InteractionType type = req.getInteractionType();
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interaction type is required");
        }

        // SUBSCRIBE = upsert (one row per pair)
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
            Interaction saved = interactionRepositoryAdapter.save(sub);

            // notify owner of target vibe
            String ownerUserId = targetVibe.getVibeAccountId().toString();
            String subscriberName = (req.isAnonymous() ? "Someone" : myVibe.getName());

            notificationService.create(
                    ownerUserId,
                    "SUBSCRIBE",
                    "New subscriber",
                    subscriberName + " subscribed to your Vibe",
                    "/view/" + myVibe.getId()
            );
            return InteractionResponseMapper.toInteractionResponse(
                    InteractionDtoMapper.toInteractionDto(saved)
            );
        }

        // UNSUBSCRIBE = toggle active=false
        if (type == InteractionType.UNSUBSCRIBE) {
            Interaction sub = interactionRepositoryAdapter
                    .findAnySubscription(myVibe, targetVibe)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Subscription not found"));

            sub.setActive(false);
            Interaction saved = interactionRepositoryAdapter.save(sub);

            return InteractionResponseMapper.toInteractionResponse(
                    InteractionDtoMapper.toInteractionDto(saved)
            );
        }

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

    public List<VibeDto> getFollowing(UUID vibeId){

        Optional<Vibe> vibe = springDataVibeRepository.findById(vibeId);

        List<Interaction> interactions = interactionRepositoryAdapter.getAllFollowings(vibe.get());

        List<Vibe> vibeList = interactions.stream().map(Interaction::getTargetVibe).collect(Collectors.toList());
        List<VibeDto> vibeDtoList = vibeList.stream().map(VibeDtoMapper::toDto).toList();

        return vibeDtoList;
    }
    public List<InteractionWithOffersDTO> getFollowingWithOffers(UUID vibeId){

        Optional<Vibe> vibe = springDataVibeRepository.findById(vibeId);

        List<InteractionWithOffersDTO>  interactions = interactionRepositoryAdapter.getAllFollowingsWithOffers(vibe.get());

        return interactions;
    }

    public List<VibeDto> getSubscribers(UUID vibeId) {

        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        List<Interaction> interactions = interactionRepositoryAdapter.findActiveSubscribersByTarget(targetVibe);

        return interactions.stream()
                .map(Interaction::getSubscriberVibe)
                .map(VibeDtoMapper::toDto)
                .toList();
    }

    public List<VibeDto> getSubscribersOwnedBy(UUID vibeId, String requesterUserId) {

        Vibe targetVibe = springDataVibeRepository
                .findById(vibeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vibe not found"));

        // owner check
        if (!targetVibe.getVibeAccountId().toString().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vibe");
        }

        List<Interaction> interactions = interactionRepositoryAdapter.findActiveSubscribersByTarget(targetVibe);

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


    public Boolean isSubscribed(UUID subscriberVibeId,UUID targetVibeId){

        Vibe subscriberVibe = springDataVibeRepository.findById(subscriberVibeId).orElseThrow(()->new RuntimeException("Subscriber Vibe not found"));
        Vibe targetVibe = springDataVibeRepository.findById(targetVibeId).orElseThrow(()->new RuntimeException("Target Vibe not found"));

        boolean isSubscribed = interactionRepositoryAdapter.isSubscribed(subscriberVibe,targetVibe);

        return isSubscribed;
    }

    @Override
    public EarlyAccessRequestDTO create(String email) {

        EarlyAccessRequest earlyAccessRequest = new EarlyAccessRequest();
        earlyAccessRequest.setCreatedAt(Instant.now());
        earlyAccessRequest.setApproved(true);
        earlyAccessRequest.setEmail(email);

        EarlyAccessRequest earlyAccessRequestSaved = jpaEarlyAccessRequestAdapter.save(earlyAccessRequest);

        EarlyAccessRequestDTO earlyAccessRequestDTO = modelMapper.map(earlyAccessRequestSaved,EarlyAccessRequestDTO.class);

        return earlyAccessRequestDTO;
    }

    @Override
    public boolean isSubscribedEarlyAccess(String email) {

        return jpaEarlyAccessRequestAdapter.isSubscribedEarlyAccess(email);

    }
}
