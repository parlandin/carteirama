package com.parlandim.carteirama.admin;

import com.parlandim.carteirama.account.AccountStateService;
import com.parlandim.carteirama.account.AuditService;
import com.parlandim.carteirama.admin.dto.AdminDtos;
import com.parlandim.carteirama.billing.PaymentConfirmationService;
import com.parlandim.carteirama.billing.dto.InvoiceResponse;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Plan;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.PaymentMethod;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.AuditLogRepository;
import com.parlandim.carteirama.repository.InvoiceRepository;
import com.parlandim.carteirama.repository.PlanRepository;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    public enum AccountFilter { trial, active, suspended, cancelled, late }

    public enum InvoiceFilter { pending, paid, cancelled, late }

    private final AccountRepository accounts;
    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final PlanRepository plans;
    private final AuditLogRepository auditLogs;
    private final AccountStateService accountState;
    private final AuditService audit;
    private final PaymentConfirmationService paymentConfirmation;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public AdminDtos.Summary summary() {
        LocalDate today = clock.today();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (AccountStatus s : AccountStatus.values()) {
            byStatus.put(s.name(), accounts.countByStatus(s));
        }
        byStatus.put("late", (long) lateAccountIds().size());

        YearMonth month = clock.currentMonth();
        long paid = 0, pending = 0, late = 0;
        for (Object[] row : invoices.figuresBetween(month.atDay(1), month.atEndOfMonth())) {
            InvoiceStatus status = (InvoiceStatus) row[0];
            LocalDate due = (LocalDate) row[1];
            int amount = (Integer) row[2];
            if (status == InvoiceStatus.paid) {
                paid += amount;
            } else if (status == InvoiceStatus.pending) {
                if (due.isBefore(today)) {
                    late += amount;
                } else {
                    pending += amount;
                }
            }
        }

        YearMonth first = month.minusMonths(11);
        Map<YearMonth, Long> billed = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            billed.put(first.plusMonths(i), 0L);
        }
        for (Object[] row : invoices.billedByMonth(first.atDay(1), month.atEndOfMonth())) {
            YearMonth ym = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            billed.computeIfPresent(ym, (k, v) -> v + ((Number) row[2]).longValue());
        }
        List<AdminDtos.MonthValue> mrrByMonth = billed.entrySet().stream()
                .map(e -> new AdminDtos.MonthValue(e.getKey().toString(), e.getValue())).toList();

        return new AdminDtos.Summary(subscriptions.sumMrrCents(), byStatus,
                new AdminDtos.InvoicesMonth(month.toString(), paid, pending, late), mrrByMonth);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AccountItem> accounts(AccountFilter status, String q, Pageable page) {
        LocalDate today = clock.today();
        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status == AccountFilter.late) {
                p.add(cb.equal(root.get("status"), AccountStatus.active));
                Subquery<UUID> overdue = query.subquery(UUID.class);
                var inv = overdue.from(Invoice.class);
                overdue.select(inv.get("account").get("id")).where(
                        cb.equal(inv.get("account").get("id"), root.get("id")),
                        cb.equal(inv.get("status"), InvoiceStatus.pending),
                        cb.lessThan(inv.get("dueDate"), today));
                p.add(cb.exists(overdue));
            } else if (status != null) {
                p.add(cb.equal(root.get("status"), AccountStatus.valueOf(status.name())));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.strip().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                var user = root.join("user");
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("legalName")), like, '\\'));
                any.add(cb.like(cb.lower(root.get("companyName")), like, '\\'));
                any.add(cb.like(cb.lower(user.get("email")), like, '\\'));
                String digits = q.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    any.add(cb.like(root.get("document"), "%" + digits + "%"));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Page<Account> result = accounts.findAll(spec, page);
        Set<UUID> late = result.isEmpty() ? Set.of() : new HashSet<>(invoices.findAccountIdsWithInvoiceDueBefore(
                result.map(Account::getId).getContent(), InvoiceStatus.pending, today));
        return PageResponse.of(result, a -> AdminDtos.AccountItem.of(a, late.contains(a.getId())));
    }

    @Transactional(readOnly = true)
    public AdminDtos.AccountDetail account(UUID id) {
        Account a = find(id);
        LocalDate today = clock.today();
        boolean late = !invoices.findAccountIdsWithInvoiceDueBefore(List.of(id), InvoiceStatus.pending, today).isEmpty();
        return new AdminDtos.AccountDetail(AdminDtos.AccountItem.of(a, late), a.getPhone(), a.getZip(), a.getAddress(),
                a.getCity(), a.getState(),
                AdminDtos.SubscriptionView.of(subscriptions.findByAccountId(id).orElse(null)),
                invoices.findTop24ByAccountIdOrderByDueDateDesc(id).stream().map(i -> InvoiceResponse.of(i, today)).toList(),
                auditLogs.findTop50ByAccountIdOrderByCreatedAtDesc(id).stream().map(AdminDtos.AuditEntry::of).toList());
    }

    @Transactional
    public AdminDtos.AccountDetail suspend(UUID adminUserId, UUID accountId, String reason) {
        Account a = find(accountId);
        if (a.getUser().getId().equals(adminUserId)) {
            throw ApiException.conflict("invalid_account_state", "Você não pode suspender a própria conta.");
        }
        accountState.suspend(a, adminUserId, reason);
        accounts.flush();
        return account(accountId);
    }

    @Transactional
    public AdminDtos.AccountDetail reactivate(UUID adminUserId, UUID accountId) {
        accountState.reactivate(find(accountId), adminUserId);
        accounts.flush();
        return account(accountId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AdminInvoiceItem> invoices(InvoiceFilter status, YearMonth month, Pageable page) {
        LocalDate today = clock.today();
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (month != null) {
                p.add(cb.between(root.get("dueDate"), month.atDay(1), month.atEndOfMonth()));
            }
            if (status == InvoiceFilter.late) {
                p.add(cb.equal(root.get("status"), InvoiceStatus.pending));
                p.add(cb.lessThan(root.get("dueDate"), today));
            } else if (status != null) {
                p.add(cb.equal(root.get("status"), InvoiceStatus.valueOf(status.name())));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(invoices.findAll(spec, page), i -> new AdminDtos.AdminInvoiceItem(
                InvoiceResponse.of(i, today), i.getAccount().getId(), i.getAccount().getLegalName()));
    }

    @Transactional
    public InvoiceResponse markPaid(UUID adminUserId, UUID invoiceId, LocalDate paidAt, PaymentMethod method) {
        Invoice invoice = invoices.findById(invoiceId).orElseThrow(ApiException::notFound);
        if (invoice.getStatus() != InvoiceStatus.pending) {
            throw ApiException.conflict("invoice_not_payable", "Esta fatura já está paga ou cancelada.");
        }
        LocalDate today = clock.today();
        if (paidAt.isAfter(today)) {
            throw ValidationException.of("paid_at", "out_of_range", "A data de pagamento não pode ser futura.");
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("invoice_id", invoice.getId().toString());
        meta.put("number", invoice.getNumber());
        meta.put("paid_at", paidAt.toString());
        meta.put("method", method.name());
        audit.record(invoice.getAccount().getId(), adminUserId, "invoice.mark_paid", meta);
        paymentConfirmation.confirm(invoice, paidAt.atStartOfDay(BusinessClock.ZONE).toInstant(), method);
        return InvoiceResponse.of(invoice, today);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PlanView> plans() {
        return plans.findAll().stream().map(AdminDtos.PlanView::of).toList();
    }

    @Transactional
    public AdminDtos.PlanView updatePlanPrice(UUID adminUserId, String code, int priceCents) {
        Plan plan = plans.findById(code).orElseThrow(ApiException::notFound);
        int from = plan.getPriceCents();
        plan.setPriceCents(priceCents);
        audit.record(null, adminUserId, "plan.price_change", Map.of("plan", code, "from", from, "to", priceCents));
        return AdminDtos.PlanView.of(plan);
    }

    private Account find(UUID id) {
        return accounts.findById(id).orElseThrow(ApiException::notFound);
    }

    private List<UUID> lateAccountIds() {
        return invoices.findAccountIdsWithOverdueInvoices(AccountStatus.active, InvoiceStatus.pending,
                clock.today().minusDays(1));
    }
}
