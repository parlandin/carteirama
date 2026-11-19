package com.parlandim.carteirama.common.email;

import com.parlandim.carteirama.common.CarteiramaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Os e-mails que a Carteirama envia, todos no mesmo layout (ver {@link EmailLayout}). */
@Component
@RequiredArgsConstructor
public class EmailTemplates {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CarteiramaProperties properties;

    public EmailMessage passwordReset(String to, String name, String link) {
        return layout()
                .preheader("Use o link para criar uma nova senha. Ele vale por 1 hora.")
                .eyebrow("// Conta · nova senha")
                .title("Redefinir sua senha")
                .paragraph("Olá, " + firstName(name) + ". Recebemos um pedido para criar uma nova senha para a sua conta na Carteirama.")
                .button("Criar nova senha", link)
                .note("O link vale por 1 hora e só pode ser usado uma vez. Se não foi você que pediu, ignore este e-mail: sua senha continua a mesma.")
                .build(to, "Redefinir sua senha no Carteirama");
    }

    public EmailMessage invoiceDue(String to, String name, String number, int amountCents, LocalDate dueDate) {
        String amount = money(amountCents);
        String due = dueDate.format(BR_DATE);
        return layout()
                .preheader("Fatura " + number + " de " + amount + ", vencimento em " + due + ".")
                .eyebrow("// Assinatura · fatura")
                .title("Sua fatura vence em 3 dias")
                .paragraph("Olá, " + firstName(name) + ". Este é um lembrete da próxima fatura da sua assinatura da Carteirama.")
                .details("Fatura", number, "Valor", amount, "Vencimento", due)
                .button("Ver fatura e pagar", url("/app/assinatura"))
                .note("Pode pagar por Pix, boleto ou cartão. Se você já pagou, desconsidere este aviso.")
                .build(to, "Sua fatura " + number + " vence em 3 dias");
    }

    public EmailMessage accountSuspended(String to, String name, boolean trialExpired) {
        return layout()
                .preheader("Seus dados continuam guardados. Regularize a assinatura para voltar a editar.")
                .eyebrow("// Assinatura · conta suspensa")
                .title("Sua conta foi suspensa")
                .paragraph("Olá, " + firstName(name) + ". "
                        + (trialExpired
                        ? "Seu período de teste terminou."
                        : "Há uma fatura da sua assinatura em aberto há mais de alguns dias."))
                .callout("Seus dados continuam disponíveis para consulta e exportação. Só não é possível cadastrar nem alterar nada até a assinatura ser regularizada.")
                .button("Regularizar assinatura", url("/app/assinatura"))
                .build(to, "Sua conta no Carteirama foi suspensa");
    }

    private EmailLayout layout() {
        return EmailLayout.create(properties.frontendUrl());
    }

    private String url(String path) {
        String base = properties.frontendUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    private static String firstName(String name) {
        String trimmed = name == null ? "" : name.strip();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    static String money(int cents) {
        String value = BigDecimal.valueOf(cents, 2).toPlainString();
        String[] parts = value.split("\\.");
        StringBuilder reais = new StringBuilder(parts[0]);
        for (int i = reais.length() - 3; i > 0; i -= 3) {
            reais.insert(i, '.');
        }
        return "R$ " + reais + "," + parts[1];
    }
}
