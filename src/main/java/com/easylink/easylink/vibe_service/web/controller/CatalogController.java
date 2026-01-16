package com.easylink.easylink.vibe_service.web.controller;


import com.easylink.easylink.vibe_service.application.dto.CreateItemCommand;
import com.easylink.easylink.vibe_service.application.dto.ItemDTO;
import com.easylink.easylink.vibe_service.application.port.in.catalog.CreateItemUseCase;
import com.easylink.easylink.vibe_service.application.port.out.CatalogRepositoryPort;
import com.easylink.easylink.vibe_service.application.service.CatalogService;
import com.easylink.easylink.vibe_service.web.dto.CreateItemRequest;
import com.easylink.easylink.vibe_service.web.dto.ItemResponse;
import com.easylink.easylink.vibe_service.web.dto.UpdateItemRequest;
import io.jsonwebtoken.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v3/catalog")
@Tag(name="Catalog API", description = "Manage Catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CreateItemUseCase createItemUseCase;
    private final CatalogService catalogService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Create item", description = "Create new item")
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(
            @RequestBody @Valid CreateItemRequest createItemRequest,
            @AuthenticationPrincipal Jwt jwt
    ){
        ItemDTO itemDTO = createItemUseCase.saveItem(
                modelMapper.map(createItemRequest, CreateItemCommand.class)
        );
        return ResponseEntity.ok(modelMapper.map(itemDTO, ItemResponse.class));
    }

    @Operation(summary = "Get all items", description = "Get all items by VibeId")
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItemsByVibeId(@RequestParam UUID vibeId){
        List<ItemDTO> itemDTOList = catalogService.getAllItemsByVibeId(vibeId);
        List<ItemResponse> itemResponseList = itemDTOList.stream()
                .map(item -> modelMapper.map(item, ItemResponse.class))
                .toList();
        return ResponseEntity.ok(itemResponseList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable UUID id){
        ItemDTO itemDTO = catalogService.getById(id);
        return ResponseEntity.ok(modelMapper.map(itemDTO, ItemResponse.class));
    }

    @Operation(summary="Update item", description = "Partially update an item")
    @PatchMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateItemRequest updateItemRequest
    ){
        ItemDTO updatedItem = catalogService.updateItem(id, updateItemRequest);
        return ResponseEntity.ok(modelMapper.map(updatedItem, ItemResponse.class));
    }

    @Operation(summary="Delete item", description="Delete item by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        catalogService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
