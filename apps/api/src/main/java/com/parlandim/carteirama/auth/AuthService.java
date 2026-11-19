package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.auth.dto.LoginRequest;
import com.parlandim.carteirama.auth.dto.RegisterRequest;
import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.email.EmailSender;
import com.parlandim.carteirama.common.email.EmailTemplates;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.security.RateLimiter;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.PasswordReset;
import com.parlandim.carteirama.model.Plan;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.User;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.PersonType;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.PasswordResetRepository;
import com.parlandim.carteirama.repository.PlanRepository;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import com.parlandim.carteirama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String PLAN_CODE = "cloud";
    static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final PasswordResetRepository passwordResets;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final EmailSender emailSender;
    private final EmailTemplates emailTemplates;
    private final SessionService sessionService;
    private final BusinessClock clock;
    private final CarteiramaProperties properties;

    private volatile String dummyHash;

    @Transactional
    public AuthUser register(RegisterRequest req) {
        var errors = new ValidationException.Collector();
        AccountRules.checkDocument(errors, req.personType(), req.document());
        AccountRules.checkCompanyName(errors, req.personType(), req.companyName());
        errors.throwIfAny();
        PasswordPolicy.requireDifferentFromEmail("password", req.password(), req.email());

        if (users.existsByEmail(req.email())) {
            throw ApiException.conflict("email_taken", "Este e-mail já está em uso.");
        }
        if (accounts.existsByDocument(req.document())) {
            throw ApiException.conflict("document_taken", "Este CPF/CNPJ já está cadastrado.");
        }

        User user = new User();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(user);

        Plan plan = plans.findById(PLAN_CODE).orElseThrow(() -> new IllegalStateException("Plano cloud não existe"));

        Account account = new Account();
        account.setUser(user);
        account.setPersonType(req.personType());
        account.setDocument(req.document());
        account.setLegalName(req.legalName() != null ? req.legalName() : req.name());
        account.setCompanyName(req.personType() == PersonType.pj ? req.companyName() : null);
        account.setPhone(req.phone());
        account.setZip(req.zip());
        account.setAddress(req.address());
        account.setCity(req.city());
        account.setState(req.state());
        account.setStatus(AccountStatus.trial);
        account.setTrialEndsAt(clock.now().plus(plan.getTrialDays(), ChronoUnit.DAYS));
        accounts.save(account);

        Subscription subscription = new Subscription();
        subscription.setAccount(account);
        subscription.setPlan(plan);
        subscription.setPriceCents(plan.getPriceCents());
        subscription.setStatus(SubscriptionStatus.trialing);
        subscriptions.save(subscription);

        return new AuthUser(user.getId(), account.getId(), false);
    }

    @Transactional(readOnly = true)
    public AuthUser login(LoginRequest req) {
        rateLimiter.check(RateLimiter.LOGIN_FAILURES, req.email());
        User user = users.findByEmail(req.email()).orElse(null);
        boolean ok;
        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash());
            ok = false;
        } else {
            ok = passwordEncoder.matches(req.password(), user.getPasswordHash());
        }
        if (!ok) {
            rateLimiter.record(RateLimiter.LOGIN_FAILURES, req.email());
            throw ApiException.invalidCredentials();
        }
        rateLimiter.reset(RateLimiter.LOGIN_FAILURES, req.email());
        UUID accountId = accounts.findByUserId(user.getId()).map(Account::getId).orElse(null);
        return new AuthUser(user.getId(), accountId, user.isAdmin());
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(email).ifPresent(user -> {
            String token = Tokens.random();
            PasswordReset reset = new PasswordReset();
            reset.setUser(user);
            reset.setTokenHash(Tokens.sha256(token));
            reset.setExpiresAt(clock.now().plus(RESET_TTL));
            passwordResets.save(reset);
            String link = properties.frontendUrl() + "/reset-password?token=" + token;
            emailSender.send(emailTemplates.passwordReset(user.getEmail(), user.getName(), link));
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordReset reset = passwordResets.findByTokenHash(Tokens.sha256(token))
                .filter(r -> r.getUsedAt() == null && r.getExpiresAt().isAfter(clock.now()))
                .orElseThrow(() -> ValidationException.of("token", "invalid_token"));
        User user = reset.getUser();
        PasswordPolicy.requireDifferentFromEmail("password", newPassword, user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        reset.setUsedAt(clock.now());
        sessionService.closeAllOf(user.getId(), null);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }
}
