package com.parlandim.carteirama.auth.dto;

import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.User;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.PersonType;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(UserView user, AccountView account) {
    public record UserView(UUID id, String name, String email, boolean isAdmin) {
    }

    public record AccountView(UUID id, PersonType personType, String document, String legalName, String companyName,
                              String phone, String zip, String address, String city, String state,
                              AccountStatus status, Instant trialEndsAt, Instant suspendedAt, Instant cancelledAt) {
    }

    public static MeResponse of(User user, Account account) {
        UserView u = new UserView(user.getId(), user.getName(), user.getEmail(), user.isAdmin());
        AccountView a = account == null ? null : new AccountView(account.getId(), account.getPersonType(),
                account.getDocument(), account.getLegalName(), account.getCompanyName(), account.getPhone(),
                account.getZip(), account.getAddress(), account.getCity(), account.getState(), account.getStatus(),
                account.getTrialEndsAt(), account.getSuspendedAt(), account.getCancelledAt());
        return new MeResponse(u, a);
    }
}
