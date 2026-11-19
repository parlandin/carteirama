package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.account.AccountStateService;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.PaymentMethod;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentConfirmationService {

    private final SubscriptionRepository subscriptions;
    private final AccountStateService accountState;

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(Invoice invoice, Instant paidAt, PaymentMethod method) {
        if (invoice.getStatus() != InvoiceStatus.paid) {
            invoice.setStatus(InvoiceStatus.paid);
            invoice.setPaidAt(paidAt);
            invoice.setMethod(method);
        }
        subscriptions.findByAccountId(invoice.getAccount().getId())
                .filter(s -> s.getStatus() != SubscriptionStatus.cancelled)
                .ifPresent(s -> s.setStatus(SubscriptionStatus.active));
        accountState.onPaymentConfirmed(invoice.getAccount());
    }
}
