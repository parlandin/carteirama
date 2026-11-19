package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.enums.ClientStatus;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(UUID id, UUID groupId, String groupName, String groupColor, String name,
                             String document, String email, String phone, int amountCents, int dueDay,
                             ClientStatus status, String notes, Instant createdAt) {
    public static ClientResponse of(Client c) {
        return new ClientResponse(c.getId(), c.getGroup().getId(), c.getGroup().getName(), c.getGroup().getColor(),
                c.getName(), c.getDocument(), c.getEmail(), c.getPhone(), c.getAmountCents(), c.getDueDay(),
                c.getStatus(), c.getNotes(), c.getCreatedAt());
    }
}
