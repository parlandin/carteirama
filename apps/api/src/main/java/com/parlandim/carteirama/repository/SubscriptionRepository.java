package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByAccountId(UUID accountId);

    Optional<Subscription> findByGatewaySubscriptionId(String gatewaySubscriptionId);

    @Query("""
            select coalesce(sum(s.priceCents), 0)
            from Subscription s
            where s.status = :subscriptionStatus and s.account.status = :accountStatus
            """)
    long sumMrrCents(SubscriptionStatus subscriptionStatus, AccountStatus accountStatus);

    default long sumMrrCents() {
        return sumMrrCents(SubscriptionStatus.active, AccountStatus.active);
    }
}
