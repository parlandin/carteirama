package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.charge.dto.ChargeResponse;

import java.util.List;

/** {@code extras}: bônus e custos recorrentes em vigor e os de um mês dos últimos 12 meses em diante. */
public record ClientDetailResponse(ClientResponse client, List<ChargeResponse> charges, List<ExtraResponse> extras) {
}
