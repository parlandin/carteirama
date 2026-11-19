package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    @EntityGraph(attributePaths = "group")
    Optional<Client> findByIdAndAccountId(UUID id, UUID accountId);

    @Override
    @EntityGraph(attributePaths = "group")
    Page<Client> findAll(Specification<Client> spec, Pageable pageable);

    List<Client> findAllByAccountIdOrderByNameAsc(UUID accountId);

    List<Client> findAllByAccountIdAndStatus(UUID accountId, ClientStatus status);

    List<Client> findAllByAccountIdAndGroupIdAndStatusNotOrderByNameAsc(UUID accountId, UUID groupId, ClientStatus status);

    long countByAccountIdAndStatusNot(UUID accountId, ClientStatus status);

    boolean existsByAccountIdAndGroupIdAndStatus(UUID accountId, UUID groupId, ClientStatus status);
}
