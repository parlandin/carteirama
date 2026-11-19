package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.charge.dto.ChargeResponse;

import java.util.List;

public record ClientDetailResponse(ClientResponse client, List<ChargeResponse> charges, List<ExtraResponse> extras) {
}
