package com.parlandim.carteirama.repository;

import com.parlandim.carteirama.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByGatewayEventId(String gatewayEventId);

    boolean existsByGatewayEventId(String gatewayEventId);
}
