package com.parlandim.carteirama.group.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Optional;

@Getter
public class UpdateGroupRequest {

    @Pattern(regexp = "^.+$", message = "required")
    @Size(max = 60)
    @JsonSetter(nulls = Nulls.FAIL)
    private String name;
    private Optional<@Pattern(regexp = Patterns.HEX_COLOR) String> color;
    private Optional<@Size(max = 2000) String> notes;

    public void setName(String name) {
        this.name = Text.trim(name);
    }

    public void setColor(Optional<String> color) {
        this.color = Text.patch(color, Text::trimToNull);
    }

    public void setNotes(Optional<String> notes) {
        this.notes = Text.patch(notes, Text::trimToNull);
    }
}
