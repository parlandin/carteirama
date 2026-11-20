package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByIdAndAccountId(UUID id, UUID accountId);

    Page<Invoice> findAllByAccountIdOrderByDueDateDesc(UUID accountId, Pageable pageable);

    List<Invoice> findTop24ByAccountIdOrderByDueDateDesc(UUID accountId);

    Optional<Invoice> findByGatewayChargeId(String gatewayChargeId);

    @Query("""
            select i from Invoice i
            where i.status = :status and i.dueDate = :dueDate
              and not exists (select 1 from Subscription s
                              where s.account = i.account and (s.cancelAtPeriodEnd = true or s.status = :ended))
            """)
    List<Invoice> findPayableByStatusAndDueDate(InvoiceStatus status, LocalDate dueDate, SubscriptionStatus ended);

    default List<Invoice> findPayableDueOn(LocalDate dueDate) {
        return findPayableByStatusAndDueDate(InvoiceStatus.pending, dueDate, SubscriptionStatus.cancelled);
    }

    @Query("""
            select distinct i.account.id from Invoice i
            where i.account.status = :accountStatus and i.status = :invoiceStatus and i.dueDate <= :cutoff
              and not exists (select 1 from Subscription s
                              where s.account = i.account and (s.cancelAtPeriodEnd = true or s.status = :ended))
            """)
    List<UUID> findAccountIdsWithOverduePayableInvoices(AccountStatus accountStatus, InvoiceStatus invoiceStatus,
                                                        LocalDate cutoff, SubscriptionStatus ended);

    @Modifying
    @Query("update Invoice i set i.status = :cancelled where i.account.id = :accountId and i.status = :pending")
    int cancelPendingByAccount(UUID accountId, InvoiceStatus pending, InvoiceStatus cancelled);

    @Query("""
            select distinct i.account.id from Invoice i
            where i.account.status = :accountStatus and i.status = :invoiceStatus and i.dueDate <= :cutoff
            """)
    List<UUID> findAccountIdsWithOverdueInvoices(AccountStatus accountStatus, InvoiceStatus invoiceStatus, LocalDate cutoff);

    @Query("""
            select distinct i.account.id from Invoice i
            where i.account.id in :accountIds and i.status = :status and i.dueDate < :today
            """)
    List<UUID> findAccountIdsWithInvoiceDueBefore(Collection<UUID> accountIds, InvoiceStatus status, LocalDate today);

    @Query("select i.status, i.dueDate, i.amountCents from Invoice i where i.dueDate between :start and :end")
    List<Object[]> figuresBetween(LocalDate start, LocalDate end);

    @Query(value = """
            SELECT CAST(extract(year FROM due_date) AS int),
                   CAST(extract(month FROM due_date) AS int),
                   CAST(sum(amount_cents) AS bigint)
            FROM invoices
            WHERE status <> 'cancelled' AND due_date BETWEEN :start AND :end
            GROUP BY 1, 2
            """, nativeQuery = true)
    List<Object[]> billedByMonth(LocalDate start, LocalDate end);
}
