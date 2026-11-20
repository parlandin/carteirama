package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.validation.Text;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.Arrays;

@Slf4j
@Component
@ConditionalOnProperty("create-admin")
@RequiredArgsConstructor
public class CreateAdminCommand implements ApplicationRunner {

    private final AdminAccounts admins;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int code = 1;
        try {
            code = createAdmin(args);
        } finally {
            int exitCode = code;
            System.exit(SpringApplication.exit(context, () -> exitCode));
        }
    }

    private int createAdmin(ApplicationArguments args) {
        String email = Text.email(first(args, "create-admin"));
        String name = Text.trimToNull(first(args, "name"));
        if (!AdminAccounts.validEmail(email)) {
            log.error("Informe um e-mail válido: --create-admin=email@exemplo.com");
            return 1;
        }
        if (admins.exists(email)) {
            log.error("Já existe um usuário com o e-mail {}", email);
            return 1;
        }
        Console console = System.console();
        if (console == null) {
            log.error("Sem terminal interativo para ler a senha (no Docker, use 'docker compose run' com TTY)");
            return 1;
        }
        char[] password = console.readPassword("Senha do admin (8 a 72 caracteres, letras e números): ");
        char[] confirm = console.readPassword("Repita a senha: ");
        try {
            String pwd = new String(password);
            if (!Arrays.equals(password, confirm)) {
                log.error("As senhas não conferem");
                return 1;
            }
            if (!AdminAccounts.strongPassword(pwd, email)) {
                log.error("Senha fraca: use 8 a 72 caracteres, com letras e números, diferente do e-mail");
                return 1;
            }
            admins.create(email, name, pwd);
            log.info("Admin {} criado", email);
            return 0;
        } finally {
            Arrays.fill(password, ' ');
            Arrays.fill(confirm, ' ');
        }
    }

    private static String first(ApplicationArguments args, String option) {
        var values = args.getOptionValues(option);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
