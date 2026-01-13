package com.easylink.easylink.vibe_service.infrastructure.repository;

import com.easylink.easylink.vibe_service.application.port.out.CatalogSaveItemRepositoryPort;
import com.easylink.easylink.vibe_service.domain.model.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaCatalogRepositoryAdapter implements CatalogSaveItemRepositoryPort {

    private final SpringDataCatalogRepository springDataCatalogRepository;

    @Override
    public Item save(Item item) {
        return springDataCatalogRepository.save(item);
    }

    public List<Item> getAllItemsByVibeId(UUID vibeId) {
        return springDataCatalogRepository.findByVibeId(vibeId);
    }

    public Optional<Item> findById(UUID id) {
        return springDataCatalogRepository.findById(id);
    }

    public void deleteById(UUID id) {
        springDataCatalogRepository.deleteById(id);
    }

    public void delete(Item item) {
        springDataCatalogRepository.delete(item);
    }
}
