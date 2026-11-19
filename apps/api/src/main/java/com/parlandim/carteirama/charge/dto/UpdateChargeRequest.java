package com.parlandim.carteirama.charge.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Optional;

@Getter
public class UpdateChargeRequest {

    @Setter
    @Min(1)
    @Max(99_999_999)
    @JsonSetter(nulls = Nulls.FAIL)
    private Integer amountCents;
    @Setter
    @JsonSetter(nulls = Nulls.FAIL)
    private LocalDate dueDate;
    private Optional<@Size(max = 500) String> note;

    public void setNote(Optional<String> note) {
        this.note = Text.patch(note, Text::trimToNull);
    }
}
