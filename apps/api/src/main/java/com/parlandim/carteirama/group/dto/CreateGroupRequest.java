package com.parlandim.carteirama.group.dto;

import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank @Size(max = 60) String name,
        @Pattern(regexp = Patterns.HEX_COLOR) String color,
        @Size(max = 2000) String notes
) {
    public CreateGroupRequest {
        name = Text.trim(name);
        color = Text.trimToNull(color);
        notes = Text.trimToNull(notes);
    }
}
