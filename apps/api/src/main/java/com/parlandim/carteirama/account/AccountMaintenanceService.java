package com.parlandim.carteirama.account;

import com.parlandim.carteirama.common.email.EmailSender;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountMaintenanceService {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountRepository accounts;
    private final AccountStateService accountState;
    private final EmailSender emailSender;
    private final JdbcTemplate jdbc;

    @Transactional
    public void autoSuspend(UUID accountId, String reason) {
        Account account = accounts.findById(accountId).orElseThrow();
        accountState.suspend(account, null, reason);
        emailSender.send(account.getUser().getEmail(), "Sua conta no Carteirama foi suspensa",
                "Olá, " + account.getUser().getName() + ".\n\n"
                        + ("trial_expired".equals(reason)
                        ? "Seu período de teste terminou."
                        : "Há uma fatura em aberto há mais de alguns dias.")
                        + " Seus dados continuam disponíveis para consulta e exportação. Para voltar a editar, "
                        + "regularize a assinatura na tela Assinatura.");
    }

    @Transactional(readOnly = true)
    public void sendDueReminder(Invoice invoice) {
        Account account = accounts.findById(invoice.getAccount().getId()).orElseThrow();
        emailSender.send(account.getUser().getEmail(), "Sua fatura " + invoice.getNumber() + " vence em 3 dias",
                "Olá, " + account.getUser().getName() + ".\n\nA fatura " + invoice.getNumber() + " de R$ "
                        + BigDecimal.valueOf(invoice.getAmountCents(), 2).toPlainString().replace('.', ',')
                        + " vence em " + invoice.getDueDate().format(BR_DATE) + ".");
    }

    @Transactional
    public void purge(UUID accountId) {
        UUID userId = jdbc.queryForObject("SELECT user_id FROM accounts WHERE id = ?", UUID.class, accountId);
        jdbc.update("DELETE FROM charges WHERE account_id = ?", accountId);
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
