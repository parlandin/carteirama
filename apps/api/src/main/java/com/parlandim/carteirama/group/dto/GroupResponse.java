package com.parlandim.carteirama.group.dto;

import com.parlandim.carteirama.model.Group;

import java.time.Instant;
import java.util.UUID;

public record GroupResponse(UUID id, String name, String color, String notes, Instant archivedAt, Instant createdAt) {
    public static GroupResponse of(Group g) {
        return new GroupResponse(g.getId(), g.getName(), g.getColor(), g.getNotes(), g.getArchivedAt(), g.getCreatedAt());
    }
}
