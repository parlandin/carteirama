package com.parlandim.carteirama.account;

import com.parlandim.carteirama.common.email.EmailSender;
import com.parlandim.carteirama.common.email.EmailTemplates;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountMaintenanceService {

    private final AccountRepository accounts;
    private final AccountStateService accountState;
    private final EmailSender emailSender;
    private final EmailTemplates emailTemplates;
    private final JdbcTemplate jdbc;

    @Transactional
    public void autoSuspend(UUID accountId, String reason) {
        Account account = accounts.findById(accountId).orElseThrow();
        accountState.suspend(account, null, reason);
        emailSender.send(emailTemplates.accountSuspended(account.getUser().getEmail(), account.getUser().getName(),
                "trial_expired".equals(reason)));
    }

    @Transactional(readOnly = true)
    public void sendDueReminder(Invoice invoice) {
        Account account = accounts.findById(invoice.getAccount().getId()).orElseThrow();
        emailSender.send(emailTemplates.invoiceDue(account.getUser().getEmail(), account.getUser().getName(),
                invoice.getNumber(), invoice.getAmountCents(), invoice.getDueDate()));
    }

    @Transactional
    public void purge(UUID accountId) {
        UUID userId = jdbc.queryForObject("SELECT user_id FROM accounts WHERE id = ?", UUID.class, accountId);
        jdbc.update("DELETE FROM charges WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM client_extras WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM clients WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM groups WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM invoices WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM subscriptions WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM accounts WHERE id = ?", accountId);
        jdbc.update("DELETE FROM password_resets WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", userId.toString());
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }
}
