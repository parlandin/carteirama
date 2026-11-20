package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.common.time.BusinessClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class AbacatePayBillingJobs {

    private record Due(String subscriptionId, int priceCents, LocalDate dueDate) {
    }

    private final JdbcTemplate jdbc;
    private final AbacatePayInvoices abacatePayInvoices;
    private final AbacatePayProperties properties;
    private final BusinessClock clock;

    public AbacatePayBillingJobs(JdbcTemplate jdbc, AbacatePayInvoices abacatePayInvoices,
                                 AbacatePayProperties properties, BusinessClock clock) {
        this.jdbc = jdbc;
        this.abacatePayInvoices = abacatePayInvoices;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Sao_Paulo")
    public void runAll() {
        issueUpcomingInvoices();
        endCancelledSubscriptions();
    }

    public void issueUpcomingInvoices() {
        LocalDate limit = clock.today().plusDays(properties.invoiceDaysBefore());
        List<Due> due = jdbc.query("""
                SELECT s.gateway_subscription_id, s.price_cents, s.next_due_date
                FROM subscriptions s
                JOIN accounts a ON a.id = s.account_id
                WHERE s.status = 'active' AND NOT s.cancel_at_period_end AND a.status = 'active'
                  AND s.gateway_subscription_id LIKE 'abacatepay:%' AND s.next_due_date <= ?
                  AND NOT EXISTS (SELECT 1 FROM invoices i
                                  WHERE i.account_id = s.account_id
                                    AND (i.status = 'pending' OR i.due_date = s.next_due_date))
                """, (rs, i) -> new Due(rs.getString(1), rs.getInt(2), rs.getObject(3, LocalDate.class)), limit);
        int ok = 0;
        for (Due d : due) {
            try {
                abacatePayInvoices.issue(d.subscriptionId(), d.priceCents(), d.dueDate());
                ok++;
            } catch (RuntimeException e) {
                log.error("AbacatePay: falha ao gerar a fatura de {}", d.subscriptionId(), e);
            }
        }
        log.info("AbacatePay: {} de {} faturas geradas", ok, due.size());
    }

    public void endCancelledSubscriptions() {
        List<String> ended = jdbc.queryForList("""
                SELECT s.gateway_subscription_id
                FROM subscriptions s
                JOIN accounts a ON a.id = s.account_id
                WHERE s.cancel_at_period_end AND s.status <> 'cancelled'
                  AND s.gateway_subscription_id LIKE 'abacatepay:%'
                  AND (s.next_due_date <= ? OR (s.next_due_date IS NULL AND a.trial_ends_at <= ?))
                """, String.class, clock.today(), Timestamp.from(clock.now()));
        int ok = 0;
        for (String id : ended) {
            try {
                abacatePayInvoices.endSubscription(id);
                ok++;
            } catch (RuntimeException e) {
                log.error("AbacatePay: falha ao encerrar a assinatura {}", id, e);
            }
        }
        log.info("AbacatePay: {} de {} assinaturas encerradas", ok, ended.size());
    }
}
