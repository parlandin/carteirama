package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.billing.dto.InvoiceResponse;
import com.parlandim.carteirama.billing.dto.SubscriptionResponse;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.common.web.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public record CheckoutResponse(String url) {
    }

    @GetMapping
    public SubscriptionResponse get(@AuthenticationPrincipal AuthUser user) {
        return subscriptionService.get(user.requireAccountId());
    }

    @GetMapping("/invoices")
    public PageResponse<InvoiceResponse> invoices(@AuthenticationPrincipal AuthUser user,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer limit) {
        return subscriptionService.invoices(user.requireAccountId(), Paging.of(page, limit, Sort.unsorted()));
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthUser user) {
        return new CheckoutResponse(subscriptionService.checkout(user.requireAccountId()));
    }

    @PostMapping("/invoices/{id}/payment-link")
    public PaymentGateway.PaymentLink paymentLink(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return subscriptionService.paymentLink(user.requireAccountId(), id);
    }

    @PostMapping("/cancel")
    public SubscriptionResponse cancel(@AuthenticationPrincipal AuthUser user) {
        return subscriptionService.cancel(user.requireAccountId());
    }
}
