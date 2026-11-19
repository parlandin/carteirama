package com.parlandim.carteirama.charge.dto;

public record SummaryResponse(String month, long totalCents, long paidCents, long pendingCents, long lateCents,
                              int totalCount, int paidCount, int pendingCount, int lateCount,
                              long costCents, long netCents) {
}
