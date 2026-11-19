package com.parlandim.carteirama.common.email;

import com.parlandim.carteirama.common.CarteiramaProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    private final EmailTemplates templates = new EmailTemplates(new CarteiramaProperties("https://app.exemplo.com/", 5, 90));

    @Test
    void redefinicaoDeSenhaTemBotaoLinkELogo() {
        String link = "https://app.exemplo.com/reset-password?token=abc_123";
        EmailMessage m = templates.passwordReset("ana@exemplo.com", "Ana Souza", link);

        assertThat(m.to()).isEqualTo("ana@exemplo.com");
        assertThat(m.subject()).isEqualTo("Redefinir sua senha no Carteirama");
        assertThat(m.html())
                .startsWith("<!doctype html>")
                .contains("Olá, Ana.")
                .contains("href=\"" + link + "\"")
                .contains("Criar nova senha")
                .contains("src=\"https://app.exemplo.com/email/logo.png\"")
                .contains("href=\"https://app.exemplo.com/termos\"");
        assertThat(m.text())
                .startsWith("Redefinir sua senha")
                .contains("Criar nova senha: " + link)
                .doesNotContain("<");
    }

    @Test
    void escapaHtmlDosDadosDoUsuario() {
        EmailMessage m = templates.passwordReset("x@exemplo.com", "<script>alert(1)</script> & cia", "https://a/?t=1&u=2");
        assertThat(m.html())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("href=\"https://a/?t=1&amp;u=2\"");
    }

    @Test
    void faturaMostraValorEDataNoFormatoBrasileiro() {
        EmailMessage m = templates.invoiceDue("ana@exemplo.com", "Ana", "CRT-2026-0042", 123456, LocalDate.of(2026, 12, 5));
        assertThat(m.subject()).isEqualTo("Sua fatura CRT-2026-0042 vence em 3 dias");
        assertThat(m.html()).contains("R$ 1.234,56").contains("05/12/2026").contains("https://app.exemplo.com/app/assinatura");
        assertThat(m.text()).contains("Valor: R$ 1.234,56").contains("Vencimento: 05/12/2026");
        assertThat(EmailTemplates.money(2499)).isEqualTo("R$ 24,99");
        assertThat(EmailTemplates.money(99_999_999)).isEqualTo("R$ 999.999,99");
    }

    @Test
    void contaSuspensaExplicaOMotivo() {
        assertThat(templates.accountSuspended("a@b.com", "Ana", true).text()).contains("Seu período de teste terminou.");
        assertThat(templates.accountSuspended("a@b.com", "Ana", false).text()).contains("fatura da sua assinatura em aberto");
    }
}
