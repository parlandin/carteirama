package com.parlandim.carteirama.charge.dto;

/** Valores cobrados (serviço + bônus). {@code costCents}: custos do mês; {@code netCents}: total − custos. */
public record SummaryResponse(String month, long totalCents, long paidCents, long pendingCents, long lateCents,
                              int totalCount, int paidCount, int pendingCount, int lateCount,
                              long costCents, long netCents) {
}
