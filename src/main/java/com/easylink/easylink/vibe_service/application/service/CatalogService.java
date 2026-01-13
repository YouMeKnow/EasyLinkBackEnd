package com.easylink.easylink.vibe_service.application.service;

import com.easylink.easylink.exceptions.NotFoundException;
import com.easylink.easylink.vibe_service.application.dto.ItemDTO;
import com.easylink.easylink.vibe_service.application.port.out.CatalogUpdateRepositoryPort;
import com.easylink.easylink.vibe_service.domain.model.Item;
import com.easylink.easylink.vibe_service.infrastructure.repository.JpaCatalogRepositoryAdapter;
import com.easylink.easylink.vibe_service.web.dto.UpdateItemRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.easylink.easylink.vibe_service.application.port.out.CatalogRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CatalogService implements CatalogRepositoryPort, CatalogUpdateRepositoryPort {

    private final JpaCatalogRepositoryAdapter jpaCatalogRepositoryAdapter;
    private final ModelMapper modelMapper;

    @Override
    public List<ItemDTO> getAllItemsByVibeId(UUID vibeId) {
        return jpaCatalogRepositoryAdapter.getAllItemsByVibeId(vibeId)
                .stream()
                .map(item -> modelMapper.map(item, ItemDTO.class))
                .toList();
    }

    public ItemDTO getById(UUID id) {
        Item item = jpaCatalogRepositoryAdapter.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        return modelMapper.map(item, ItemDTO.class);
    }

    @Override
    public ItemDTO updateItem(UUID id, UpdateItemRequest req) {
        Item item = jpaCatalogRepositoryAdapter.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        // ---- TITLE ----
        if (req.getTitle() != null) {
            String title = req.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Title cannot be blank");
            }
            item.setTitle(title);
        }

        // ---- DESCRIPTION ----
        if (req.getDescription() != null) {
            String description = req.getDescription().trim();
            item.setDescription(description);
        }

        // ---- PRICE ----
        if (req.getPrice() != null) {
            item.setPrice(req.getPrice());
        }

        // ---- IMAGE ----
        if (req.getImageUrl() != null) {
            item.setImageUrl(req.getImageUrl().trim());
        }

        return modelMapper.map(
                jpaCatalogRepositoryAdapter.save(item),
                ItemDTO.class
        );
    }

    public void deleteItem(UUID id) {
        Item item = jpaCatalogRepositoryAdapter.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        jpaCatalogRepositoryAdapter.delete(item);
    }

}
