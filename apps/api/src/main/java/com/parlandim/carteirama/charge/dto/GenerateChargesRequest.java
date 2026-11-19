package com.parlandim.carteirama.charge.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateChargesRequest(@NotBlank String month) {
}
