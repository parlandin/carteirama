package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.auth.dto.ChangePasswordRequest;
import com.parlandim.carteirama.auth.dto.MeResponse;
import com.parlandim.carteirama.auth.dto.UpdateMeRequest;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.User;
import com.parlandim.carteirama.model.enums.PersonType;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public MeResponse get(AuthUser auth) {
        User user = users.findById(auth.userId()).orElseThrow(ApiException::notFound);
        Account account = auth.accountId() == null ? null : accounts.findById(auth.accountId()).orElse(null);
        return MeResponse.of(user, account);
    }

    @Transactional
    public MeResponse update(AuthUser auth, UpdateMeRequest req) {
        User user = users.findById(auth.userId()).orElseThrow(ApiException::notFound);
        Account account = auth.accountId() == null ? null : accounts.findById(auth.accountId()).orElse(null);

        if (req.getName() != null) {
            user.setName(req.getName());
        }
        if (account != null) {
            if (req.getPhone() != null) {
                account.setPhone(req.getPhone());
            }
            if (req.getLegalName() != null) {
                account.setLegalName(req.getLegalName());
            }
            if (req.getCompanyName() != null && account.getPersonType() == PersonType.pj) {
                var errors = new ValidationException.Collector();
                AccountRules.checkCompanyName(errors, PersonType.pj, req.getCompanyName().orElse(null));
                errors.throwIfAny();
                account.setCompanyName(req.getCompanyName().get());
            }
            if (req.getZip() != null) {
                account.setZip(req.getZip().orElse(null));
            }
            if (req.getAddress() != null) {
                account.setAddress(req.getAddress().orElse(null));
            }
            if (req.getCity() != null) {
                account.setCity(req.getCity().orElse(null));
            }
            if (req.getState() != null) {
                account.setState(req.getState().orElse(null));
            }
        }
        return MeResponse.of(user, account);
    }

    @Transactional
    public void changePassword(AuthUser auth, ChangePasswordRequest req, String currentSessionId) {
        User user = users.findById(auth.userId()).orElseThrow(ApiException::notFound);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ValidationException.of("current_password", "incorrect_password");
        }
        PasswordPolicy.requireDifferentFromEmail("new_password", req.newPassword(), user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        sessionService.closeAllOf(user.getId(), currentSessionId);
    }
}
