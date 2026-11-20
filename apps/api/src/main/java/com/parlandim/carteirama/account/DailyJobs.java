package com.parlandim.carteirama.account;

import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.InvoiceRepository;
import com.parlandim.carteirama.repository.PasswordResetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyJobs {

    private final AccountRepository accounts;
    private final InvoiceRepository invoices;
    private final PasswordResetRepository passwordResets;
    private final AccountMaintenanceService maintenance;
    private final BusinessClock clock;
    private final CarteiramaProperties properties;

    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    public void runAll() {
        suspendExpiredTrials();
        suspendOverdueAccounts();
        sendDueReminders();
        cleanup();
    }

    public void suspendExpiredTrials() {
        List<UUID> ids = accounts.findAllByStatusAndTrialEndsAtBefore(AccountStatus.trial, clock.now())
                .stream().map(a -> a.getId()).toList();
        forEach("suspender trial vencido", ids, id -> maintenance.autoSuspend(id, "trial_expired"));
    }

    public void suspendOverdueAccounts() {
        LocalDate cutoff = clock.today().minusDays(properties.suspendAfterDays());
        List<UUID> ids = invoices.findAccountIdsWithOverduePayableInvoices(AccountStatus.active, InvoiceStatus.pending,
                cutoff, SubscriptionStatus.cancelled);
        forEach("suspender por fatura vencida", ids, id -> maintenance.autoSuspend(id, "invoice_overdue"));
    }

    public void sendDueReminders() {
        List<Invoice> due = invoices.findPayableDueOn(clock.today().plusDays(3));
        for (Invoice invoice : due) {
            try {
                maintenance.sendDueReminder(invoice);
            } catch (RuntimeException e) {
                log.error("Falha ao avisar vencimento da fatura {}", invoice.getId(), e);
            }
        }
    }

    public void cleanup() {
        int tokens = passwordResets.deleteExpired(clock.now());
        log.info("Job diário: {} tokens de senha expirados apagados", tokens);
        Instant limit = clock.now().minus(Duration.ofDays(properties.retentionDays()));
        List<UUID> ids = accounts.findAllByStatusAndCancelledAtBefore(AccountStatus.cancelled, limit)
                .stream().map(a -> a.getId()).toList();
        forEach("apagar conta cancelada", ids, maintenance::purge);
    }

    private void forEach(String what, List<UUID> accountIds, Consumer<UUID> action) {
        int ok = 0;
        for (UUID id : accountIds) {
            try {
                action.accept(id);
                ok++;
            } catch (RuntimeException e) {
                log.error("Job diário: falha ao {} (conta {})", what, id, e);
            }
        }
        log.info("Job diário: {}: {} de {} contas", what, ok, accountIds.size());
    }
}
