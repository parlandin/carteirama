package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.group.dto.GroupListItem;
import com.parlandim.carteirama.model.Group;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import com.parlandim.carteirama.model.enums.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByIdAndAccountId(UUID id, UUID accountId);

    List<Group> findAllByAccountIdAndArchivedAtIsNullOrderByNameAsc(UUID accountId);

    List<Group> findAllByAccountIdOrderByNameAsc(UUID accountId);

    boolean existsByAccountIdAndNameIgnoreCaseAndArchivedAtIsNull(UUID accountId, String name);

    boolean existsByAccountIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(UUID accountId, String name, UUID id);

    @Query("""
            select new com.parlandim.carteirama.group.dto.GroupListItem(
                g.id, g.name, g.color, g.notes,
                (select count(c) from Client c
                  where c.group = g and c.status <> :archived),
                (select coalesce(sum(ch.amountCents), 0L) from Charge ch
                  where ch.client.group = g and ch.referenceMonth = :month
                    and ch.status <> :cancelled))
            from Group g
            where g.account.id = :accountId and g.archivedAt is null
            order by lower(g.name)
            """)
    List<GroupListItem> listWithTotals(UUID accountId, LocalDate month, ClientStatus archived, ChargeStatus cancelled);

    default List<GroupListItem> listWithTotals(UUID accountId, LocalDate month) {
        return listWithTotals(accountId, month, ClientStatus.archived, ChargeStatus.cancelled);
    }
}
