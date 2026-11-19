package com.parlandim.carteirama.auth.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Optional;

@Getter
public class UpdateMeRequest {

    @Size(min = 2, max = 120)
    @JsonSetter(nulls = Nulls.FAIL)
    private String name;
    @Pattern(regexp = Patterns.PHONE)
    @JsonSetter(nulls = Nulls.FAIL)
    private String phone;
    @Size(min = 2, max = 120)
    @JsonSetter(nulls = Nulls.FAIL)
    private String legalName;
    private Optional<@Size(min = 2, max = 120) String> companyName;
    private Optional<@Pattern(regexp = Patterns.ZIP) String> zip;
    private Optional<@Size(max = 200) String> address;
    private Optional<@Size(max = 120) String> city;
    private Optional<@Pattern(regexp = Patterns.UF) String> state;

    public void setName(String name) {
        this.name = Text.trim(name);
    }

    public void setPhone(String phone) {
        this.phone = Text.digits(phone);
    }

    public void setLegalName(String legalName) {
        this.legalName = Text.trim(legalName);
    }

    public void setCompanyName(Optional<String> companyName) {
        this.companyName = Text.patch(companyName, Text::trimToNull);
    }

    public void setZip(Optional<String> zip) {
        this.zip = Text.patch(zip, Text::digits);
    }

    public void setAddress(Optional<String> address) {
        this.address = Text.patch(address, Text::trimToNull);
    }

    public void setCity(Optional<String> city) {
        this.city = Text.patch(city, Text::trimToNull);
    }

    public void setState(Optional<String> state) {
        this.state = Text.patch(state, Text::upper);
    }
}
