package com.easylink.easylink.vibe_service.infrastructure.repository;

import com.easylink.easylink.vibe_service.domain.model.Vibe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataVibeRepository extends JpaRepository<Vibe, UUID> {
    @EntityGraph(attributePaths = "fields")
    List<Vibe> findAllByVibeAccountId(UUID id);
    @EntityGraph(attributePaths = "fields")
    List<Vibe> findAllByVibeAccountIdAndDeletedAtIsNull(UUID id);
    List<Vibe> findAllById(UUID id);
    long countByVibeAccountId(UUID id);
    long countByVibeAccountIdAndDeletedAtIsNull(UUID id);
    List<Vibe> findAllByDeletedAtIsNull();
    Optional<Vibe> findByPublicCodeAndVisibleTrueAndDeletedAtIsNull(String publicCode);
    Optional<Vibe> findByIdAndDeletedAtIsNull(UUID id);
    // SpringDataVibeRepository.java

    @Query("""
        select v.id
        from Vibe v
        where v.deletedAt is null
          and v.vibeAccountId = :accountId
        """)
    List<UUID> findAliveIdsByVibeAccountId(@Param("accountId") UUID accountId);
}
