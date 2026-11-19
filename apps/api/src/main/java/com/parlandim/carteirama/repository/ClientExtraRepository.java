package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.ClientExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientExtraRepository extends JpaRepository<ClientExtra, UUID> {

    Optional<ClientExtra> findByIdAndAccountIdAndClientId(UUID id, UUID accountId, UUID clientId);

    /** Recorrentes em vigor e os de um mês a partir de {@code since}. */
    @Query("""
            select e from ClientExtra e
            where e.account.id = :accountId and e.client.id = :clientId
              and ((e.recurring = true and e.endMonth is null) or (e.recurring = false and e.startMonth >= :since))
            order by e.recurring desc, e.startMonth desc, e.createdAt
            """)
    List<ClientExtra> listVisible(UUID accountId, UUID clientId, LocalDate since);
}
