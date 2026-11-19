package com.parlandim.carteirama.client.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import com.parlandim.carteirama.model.enums.ClientStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.UUID;

@Getter
public class UpdateClientRequest {

    @Size(min = 2, max = 120)
    @JsonSetter(nulls = Nulls.FAIL)
    private String name;
    @Setter
    @JsonSetter(nulls = Nulls.FAIL)
    private UUID groupId;
    private Optional<String> document;
    private Optional<@Size(max = 254) @Pattern(regexp = Patterns.EMAIL) String> email;
    private Optional<@Pattern(regexp = Patterns.PHONE) String> phone;
    @Setter
    @Min(1)
    @Max(99_999_999)
    @JsonSetter(nulls = Nulls.FAIL)
    private Integer amountCents;
    @Setter
    @Min(1)
    @Max(28)
    @JsonSetter(nulls = Nulls.FAIL)
    private Integer dueDay;
    @Setter
    @JsonSetter(nulls = Nulls.FAIL)
    private ClientStatus status;
    private Optional<@Size(max = 2000) String> notes;

    public void setName(String name) {
        this.name = Text.trim(name);
    }

    public void setDocument(Optional<String> document) {
        this.document = Text.patch(document, Text::digits);
    }

    public void setEmail(Optional<String> email) {
        this.email = Text.patch(email, v -> Text.trimToNull(Text.email(v)));
    }

    public void setPhone(Optional<String> phone) {
        this.phone = Text.patch(phone, Text::digits);
    }

    public void setNotes(Optional<String> notes) {
        this.notes = Text.patch(notes, Text::trimToNull);
    }
}
