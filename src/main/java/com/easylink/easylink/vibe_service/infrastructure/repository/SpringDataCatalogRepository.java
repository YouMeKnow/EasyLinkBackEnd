package com.easylink.easylink.vibe_service.infrastructure.repository;

import com.easylink.easylink.vibe_service.domain.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataCatalogRepository extends JpaRepository<Item, UUID> {

    List<Item> findByVibeId(UUID vibeId);
}
