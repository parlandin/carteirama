package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.billing.dto.InvoiceResponse;
import com.parlandim.carteirama.billing.dto.SubscriptionResponse;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import com.parlandim.carteirama.repository.ClientRepository;
import com.parlandim.carteirama.repository.InvoiceRepository;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final ClientRepository clients;
    private final PaymentGateway gateway;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public SubscriptionResponse get(UUID accountId) {
        return toResponse(find(accountId), accountId);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> invoices(UUID accountId, Pageable page) {
        var today = clock.today();
        return PageResponse.of(invoices.findAllByAccountIdOrderByDueDateDesc(accountId, page),
                i -> InvoiceResponse.of(i, today));
    }

    @Transactional
    public String checkout(UUID accountId) {
        Subscription subscription = find(accountId);
        requirePayable(subscription);
        return gateway.createCheckout(subscription.getAccount(), subscription);
    }

    @Transactional(readOnly = true)
    public PaymentGateway.PaymentLink paymentLink(UUID accountId, UUID invoiceId) {
        Invoice invoice = invoices.findByIdAndAccountId(invoiceId, accountId).orElseThrow(ApiException::notFound);
        if (invoice.getStatus() != InvoiceStatus.pending) {
            throw ApiException.conflict("invoice_not_payable", "Esta fatura não está em aberto.");
        }
        requirePayable(find(accountId));
        return gateway.paymentLink(invoice);
    }

    @Transactional
    public SubscriptionResponse cancel(UUID accountId) {
        Subscription subscription = find(accountId);
        if (subscription.getStatus() == SubscriptionStatus.cancelled) {
            throw ApiException.conflict("invalid_account_state", "A assinatura já está cancelada.");
        }
        if (!subscription.isCancelAtPeriodEnd()) {
            gateway.cancelAtPeriodEnd(subscription);
            subscription.setCancelAtPeriodEnd(true);
        }
        return toResponse(subscription, accountId);
    }

    @Transactional
    public SubscriptionResponse resume(UUID accountId) {
        Subscription subscription = find(accountId);
        if (subscription.getStatus() == SubscriptionStatus.cancelled) {
            throw ApiException.conflict("invalid_account_state", "A assinatura já está cancelada.");
        }
        if (subscription.isCancelAtPeriodEnd()) {
            gateway.resume(subscription);
            subscription.setCancelAtPeriodEnd(false);
        }
        return toResponse(subscription, accountId);
    }

    private static void requirePayable(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.cancelled) {
            throw ApiException.conflict("subscription_cancelled", "A assinatura está cancelada.");
        }
        if (subscription.isCancelAtPeriodEnd()) {
            throw ApiException.conflict("subscription_cancel_scheduled",
                    "O cancelamento da assinatura está agendado. Mantenha a assinatura para voltar a pagar.");
        }
    }

    private Subscription find(UUID accountId) {
        return subscriptions.findByAccountId(accountId).orElseThrow(ApiException::notFound);
    }

    private SubscriptionResponse toResponse(Subscription s, UUID accountId) {
        Account account = s.getAccount();
        return new SubscriptionResponse(
                new SubscriptionResponse.PlanView(s.getPlan().getCode(), s.getPlan().getName()),
                s.getStatus(), s.getPriceCents(), s.getNextDueDate(), s.isCancelAtPeriodEnd(),
                account.getStatus(), account.getTrialEndsAt(),
                new SubscriptionResponse.Usage(clients.countByAccountIdAndStatusNot(accountId, ClientStatus.archived), 1));
    }
}
