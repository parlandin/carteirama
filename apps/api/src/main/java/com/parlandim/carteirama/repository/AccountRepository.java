package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Account> findAll(Specification<Account> spec, Pageable pageable);

    Optional<Account> findByUserId(UUID userId);

    @Query("select a.status from Account a where a.id = :id")
    Optional<AccountStatus> findStatusById(UUID id);

    boolean existsByDocument(String document);

    Optional<Account> findByGatewayCustomerId(String gatewayCustomerId);

    long countByStatus(AccountStatus status);

    List<Account> findAllByStatusAndTrialEndsAtBefore(AccountStatus status, Instant now);

    List<Account> findAllByStatusAndCancelledAtBefore(AccountStatus status, Instant limit);
}
