package com.easylink.easylink.services;

import com.easylink.easylink.dtos.NotificationDto;
import com.easylink.easylink.dtos.UnreadDeltaDto;
import com.easylink.easylink.entities.NotificationEntity;
import com.easylink.easylink.notifications.NotificationSseHub;
import com.easylink.easylink.repositories.SpringDataNotificationRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SpringDataNotificationRepository repo;
    private final NotificationSseHub hub;

    @Transactional(readOnly = true)
    public List<NotificationDto> list(String userId, int limit) {
        var page = PageRequest.of(0, Math.max(1, Math.min(limit, 50)));
        return repo.findByUserIdOrderByCreatedAtDesc(userId, page)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markRead(String userId, UUID id) {
        var n = repo.findById(id).orElseThrow();

        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not your notification");
        }

        if (!n.isRead()) {
            n.setRead(true);
            repo.save(n);
        }
    }

    @Transactional
    public void markAllRead(String userId) {
        var all = repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        for (var n : all) {
            if (!n.isRead()) n.setRead(true);
        }
        repo.saveAll(all);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto create(String userId, String type, String title, String body, String link) {
        var entity = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .read(false)
                .createdAt(OffsetDateTime.now())
                .build();

        repo.save(entity);

        var dto = toDto(entity);

        hub.emitToUser(userId, "notification.created", dto);
        hub.emitToUser(userId, "notification.unread_changed", new UnreadDeltaDto(1));

        return dto;
    }

    private NotificationDto toDto(NotificationEntity e) {
        return NotificationDto.builder()
                .id(e.getId())
                .type(e.getType())
                .title(e.getTitle())
                .body(e.getBody())
                .link(e.getLink())
                .read(e.isRead())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
