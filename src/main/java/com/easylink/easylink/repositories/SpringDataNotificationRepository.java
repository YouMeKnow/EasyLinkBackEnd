package com.easylink.easylink.repositories;

import com.easylink.easylink.entities.NotificationEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, PageRequest page);
    long countByUserIdAndReadFalse(String userId);
}
