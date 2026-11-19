package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    @EntityGraph(attributePaths = "actorUser")
    List<AuditLog> findTop50ByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Optional<AuditLog> findFirstByAccountIdAndActionInOrderByCreatedAtDesc(UUID accountId, Collection<String> actions);
}
