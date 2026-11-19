package com.parlandim.carteirama.billing.dto;

import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.PaymentMethod;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(UUID id, String number, int amountCents, LocalDate dueDate, InvoiceStatus status,
                              String displayStatus, Instant paidAt, PaymentMethod method) {
    public static InvoiceResponse of(Invoice i, LocalDate today) {
        boolean late = i.getStatus() == InvoiceStatus.pending && i.getDueDate().isBefore(today);
        return new InvoiceResponse(i.getId(), i.getNumber(), i.getAmountCents(), i.getDueDate(), i.getStatus(),
                late ? "late" : i.getStatus().name(), i.getPaidAt(), i.getMethod());
    }
}
