package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID>, JpaSpecificationExecutor<Charge> {

    @EntityGraph(attributePaths = {"client", "client.group"})
    Optional<Charge> findByIdAndAccountId(UUID id, UUID accountId);

    @Override
    @EntityGraph(attributePaths = {"client", "client.group"})
    Page<Charge> findAll(Specification<Charge> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "client.group"})
    List<Charge> findAllByAccountIdAndClientIdOrderByReferenceMonthDesc(UUID accountId, UUID clientId);

    boolean existsByClientIdAndReferenceMonth(UUID clientId, LocalDate referenceMonth);

    @Query("""
            select c.status, c.dueDate, c.amountCents from Charge c
            where c.account.id = :accountId and c.referenceMonth = :month and c.status <> :cancelled
            """)
    List<Object[]> monthFigures(UUID accountId, LocalDate month, ChargeStatus cancelled);

    @Modifying
    @Query(value = """
            INSERT INTO charges (id, account_id, client_id, reference_month, amount_cents, due_date, status)
            SELECT gen_random_uuid(), c.account_id, c.id, CAST(:month AS date), c.amount_cents,
                   CAST(:month AS date) + (c.due_day - 1), 'pending'
            FROM clients c
            WHERE c.account_id = :accountId AND c.status = 'active'
            ON CONFLICT (client_id, reference_month) DO NOTHING
            """, nativeQuery = true)
    int generateMonth(UUID accountId, LocalDate month);

    @Modifying
    @Query("""
            update Charge c set c.status = :cancelled
            where c.account.id = :accountId and c.client.id = :clientId and c.status = :pending
            """)
    int cancelPendingByClient(UUID accountId, UUID clientId, ChargeStatus pending, ChargeStatus cancelled);

    default int cancelPendingByClient(UUID accountId, UUID clientId) {
        return cancelPendingByClient(accountId, clientId, ChargeStatus.pending, ChargeStatus.cancelled);
    }
}
