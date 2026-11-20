package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.validation.Text;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AdminAccounts admins;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(AdminAccounts admins,
                          @Value("${carteirama.admin.email:}") String email,
                          @Value("${carteirama.admin.password:}") String password,
                          @Value("${carteirama.admin.name:}") String name) {
        this.admins = admins;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdmin(email, password, name);
    }

    public boolean ensureAdmin(String rawEmail, String rawPassword, String rawName) {
        String adminEmail = Text.email(Text.trimToNull(rawEmail));
        if (adminEmail == null && (rawPassword == null || rawPassword.isEmpty())) {
            return false;
        }
        if (!AdminAccounts.validEmail(adminEmail)) {
            throw new IllegalStateException("ADMIN_EMAIL inválido");
        }
        if (admins.exists(adminEmail)) {
            log.info("Usuário {} já existe; ADMIN_EMAIL e ADMIN_PASSWORD foram ignorados", adminEmail);
            return false;
        }
        if (!AdminAccounts.strongPassword(rawPassword, adminEmail)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD fraca: use 8 a 72 caracteres, com letras e números, diferente do e-mail");
        }
        try {
            admins.create(adminEmail, Text.trimToNull(rawName), rawPassword);
        } catch (DataIntegrityViolationException e) {
            log.info("Usuário {} foi criado por outra instância", adminEmail);
            return false;
        }
        log.info("Admin {} criado a partir de ADMIN_EMAIL", adminEmail);
        return true;
    }
}
