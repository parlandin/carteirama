package com.parlandim.carteirama.group.dto;

import java.util.UUID;

public record GroupListItem(UUID id, String name, String color, String notes, long clientCount, long monthTotalCents) {
}
