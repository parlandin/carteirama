package com.parlandim.carteirama.group.dto;

import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.Group;
import com.parlandim.carteirama.model.enums.ClientStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupDetailResponse(UUID id, String name, String color, String notes, Instant archivedAt,
                                  Instant createdAt, List<ClientItem> clients) {
    public record ClientItem(UUID id, String name, String email, int amountCents, int dueDay, ClientStatus status) {
    }

    public static GroupDetailResponse of(Group g, List<Client> clients) {
        return new GroupDetailResponse(g.getId(), g.getName(), g.getColor(), g.getNotes(), g.getArchivedAt(),
                g.getCreatedAt(), clients.stream()
                .map(c -> new ClientItem(c.getId(), c.getName(), c.getEmail(), c.getAmountCents(), c.getDueDay(), c.getStatus()))
                .toList());
    }
}
