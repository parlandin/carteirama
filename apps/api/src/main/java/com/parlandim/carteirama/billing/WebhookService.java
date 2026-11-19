package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.account.AccountStateService;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.WebhookEvent;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.InvoiceRepository;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import com.parlandim.carteirama.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEventRepository events;
    private final SubscriptionRepository subscriptions;
    private final AccountRepository accounts;
    private final InvoiceRepository invoices;
    private final AccountStateService accountState;
    private final PaymentConfirmationService paymentConfirmation;
    private final BusinessClock clock;

    @Transactional
    public boolean process(GatewayEvent event) {
        if (events.existsByGatewayEventId(event.eventId())) {
            return false;
        }
        WebhookEvent stored = new WebhookEvent();
        stored.setGatewayEventId(event.eventId());
        stored.setType(event.rawType());
        stored.setPayload(event.payload());
        events.saveAndFlush(stored);

        Optional<Subscription> subscription = findSubscription(event);
        if (subscription.isEmpty() && event.type() != GatewayEvent.Type.IGNORED) {
            log.warn("Evento {} ({}) sem assinatura correspondente", event.eventId(), event.rawType());
        }
        subscription.ifPresent(s -> apply(event, s));
        stored.setProcessedAt(clock.now());
        return true;
    }

    private void apply(GatewayEvent event, Subscription subscription) {
        Account account = subscription.getAccount();
        switch (event.type()) {
            case INVOICE_CREATED, PAYMENT_OVERDUE -> upsertInvoice(event, account);
            case PAYMENT_CONFIRMED -> {
                Invoice invoice = upsertInvoice(event, account);
                paymentConfirmation.confirm(invoice, event.paidAt() != null ? event.paidAt() : clock.now(),
                        event.method());
                if (event.nextDueDate() != null) {
                    subscription.setNextDueDate(event.nextDueDate());
                }
            }
            case SUBSCRIPTION_CANCELLED -> {
                subscription.setStatus(SubscriptionStatus.cancelled);
                accountState.cancel(account);
            }
            case IGNORED -> {
            }
        }
    }

    private Invoice upsertInvoice(GatewayEvent event, Account account) {
        return invoices.findByGatewayChargeId(event.chargeId()).orElseGet(() -> {
            Invoice invoice = new Invoice();
            invoice.setAccount(account);
            invoice.setGatewayChargeId(event.chargeId());
            invoice.setAmountCents(event.amountCents());
            invoice.setDueDate(event.dueDate());
            invoice.setStatus(InvoiceStatus.pending);
            return invoices.saveAndFlush(invoice);
        });
    }

    private Optional<Subscription> findSubscription(GatewayEvent event) {
        if (event.subscriptionId() != null) {
            Optional<Subscription> s = subscriptions.findByGatewaySubscriptionId(event.subscriptionId());
            if (s.isPresent()) {
                return s;
            }
        }
        if (event.customerId() != null) {
            return accounts.findByGatewayCustomerId(event.customerId())
                    .flatMap(a -> subscriptions.findByAccountId(a.getId()));
        }
        return Optional.empty();
    }
}
