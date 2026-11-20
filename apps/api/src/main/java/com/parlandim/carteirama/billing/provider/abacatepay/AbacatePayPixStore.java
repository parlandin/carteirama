package com.parlandim.carteirama.billing.provider.abacatepay;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class AbacatePayPixStore {

    public record PixInvoice(UUID invoiceId, String chargeId, int amountCents, LocalDate dueDate,
                             String subscriptionId, LocalDate nextDueDate) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate newTransaction;

    public AbacatePayPixStore(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Optional<String> reusableBrCode(UUID invoiceId, int amountCents, Instant validUntil) {
        return jdbc.query("""
                        SELECT br_code FROM abacatepay_pix_charges
                        WHERE invoice_id = ? AND amount_cents = ? AND status = 'PENDING' AND expires_at > ?
                        ORDER BY created_at DESC LIMIT 1
                        """, (rs, i) -> rs.getString(1), invoiceId, amountCents, Timestamp.from(validUntil))
                .stream().findFirst();
    }

    public void save(UUID invoiceId, int amountCents, AbacatePayClient.Pix pix) {
        newTransaction.executeWithoutResult(s -> jdbc.update("""
                        INSERT INTO abacatepay_pix_charges (id, invoice_id, amount_cents, br_code, status, expires_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, pix.id(), invoiceId, amountCents, pix.brCode(), pix.status(),
                Timestamp.from(pix.expiresAt())));
    }

    public void markStatus(String pixId, String status) {
        newTransaction.executeWithoutResult(s ->
                jdbc.update("UPDATE abacatepay_pix_charges SET status = ? WHERE id = ?", status, pixId));
    }

    public Optional<PixInvoice> findInvoice(String pixId) {
        return jdbc.query("""
                        SELECT i.id, i.gateway_charge_id, i.amount_cents, i.due_date,
                               s.gateway_subscription_id, s.next_due_date
                        FROM abacatepay_pix_charges p
                        JOIN invoices i ON i.id = p.invoice_id
                        JOIN subscriptions s ON s.account_id = i.account_id
                        WHERE p.id = ?
                        """, (rs, i) -> new PixInvoice(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3),
                        rs.getObject(4, LocalDate.class), rs.getString(5), rs.getObject(6, LocalDate.class)), pixId)
                .stream().findFirst();
    }
}
