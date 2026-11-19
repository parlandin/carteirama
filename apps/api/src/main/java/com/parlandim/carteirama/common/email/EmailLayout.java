package com.parlandim.carteirama.common.email;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta o e-mail no visual do front (cores, cartão com borda grossa e "sombra" chapada, rótulos em
 * fonte mono) e, ao mesmo tempo, a versão em texto puro.
 * <p>
 * HTML de e-mail: só tabelas e estilos inline, porque Gmail e Outlook ignoram boa parte do CSS.
 * A sombra do front é imitada com bordas mais grossas à direita e embaixo; Figtree e JetBrains Mono
 * aparecem onde o cliente carrega fontes (Apple Mail, iOS), e nos outros caem em Arial e Courier.
 */
public final class EmailLayout {

    // Tokens de apps/web/src/index.css
    private static final String NAVY = "#111b35";
    private static final String MUTED = "#55617a";
    private static final String PAPER = "#edf2fa";
    private static final String PAPER_2 = "#f5f8fd";
    private static final String BLUE = "#2553e6";
    private static final String AMBER = "#fbaf24";
    private static final String AMBER_SOFT = "#fdecc4";

    private static final String SANS = "Figtree,'Segoe UI',Helvetica,Arial,sans-serif";
    private static final String MONO = "'JetBrains Mono',SFMono-Regular,Menlo,Consolas,'Courier New',monospace";

    private final String baseUrl;
    private String preheader = "";
    private String eyebrow;
    private String title;
    private final List<String> html = new ArrayList<>();
    private final List<String> text = new ArrayList<>();

    private EmailLayout(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** @param baseUrl endereço do front, de onde vêm o logo e os links do rodapé. */
    public static EmailLayout create(String baseUrl) {
        return new EmailLayout(baseUrl);
    }

    /** Texto que aparece ao lado do assunto na caixa de entrada. */
    public EmailLayout preheader(String value) {
        this.preheader = value;
        return this;
    }

    /** Rótulo pequeno acima do título, no estilo "// Conta · senha". */
    public EmailLayout eyebrow(String value) {
        this.eyebrow = value;
        return this;
    }

    public EmailLayout title(String value) {
        this.title = value;
        return this;
    }

    public EmailLayout paragraph(String value) {
        html.add("<p style=\"margin:0 0 16px;font-family:" + SANS + ";font-size:16px;line-height:1.6;color:" + NAVY + ";\">"
                + esc(value) + "</p>");
        text.add(value);
        return this;
    }

    /** Botão principal, com o link por extenso logo abaixo para quem não consegue clicar. */
    public EmailLayout button(String label, String url) {
        html.add("""
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:8px 0 12px;">
                  <tr><td style="background:%s;border:2px solid %s;border-right-width:4px;border-bottom-width:4px;border-radius:10px;">
                    <a href="%s" target="_blank" style="display:inline-block;padding:13px 24px;font-family:%s;font-size:16px;font-weight:700;color:#ffffff;text-decoration:none;">%s &rarr;</a>
                  </td></tr>
                </table>
                <p style="margin:0 0 20px;font-family:%s;font-size:12px;line-height:1.5;color:%s;word-break:break-all;">Se o botão não abrir, copie este endereço no navegador:<br><a href="%s" target="_blank" style="color:%s;">%s</a></p>"""
                .formatted(BLUE, NAVY, attr(url), SANS, esc(label), SANS, MUTED, attr(url), BLUE, esc(url)));
        text.add(label + ": " + url);
        return this;
    }

    /** Quadro de dados (rótulo em mono à esquerda, valor em negrito à direita). */
    public EmailLayout details(String... labelValuePairs) {
        StringBuilder rows = new StringBuilder();
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < labelValuePairs.length; i += 2) {
            String border = i + 2 < labelValuePairs.length ? "border-bottom:1px solid #d6deeb;" : "";
            rows.append("""
                    <tr>
                      <td style="padding:12px 16px;%sfont-family:%s;font-size:11px;letter-spacing:1.5px;text-transform:uppercase;color:%s;">%s</td>
                      <td align="right" style="padding:12px 16px;%sfont-family:%s;font-size:16px;font-weight:800;color:%s;">%s</td>
                    </tr>""".formatted(border, MONO, MUTED, esc(labelValuePairs[i]), border, SANS, NAVY,
                    esc(labelValuePairs[i + 1])));
            plain.append(labelValuePairs[i]).append(": ").append(labelValuePairs[i + 1]).append('\n');
        }
        html.add("""
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:4px 0 20px;background:%s;border:2px solid %s;border-radius:12px;border-collapse:separate;">
                %s
                </table>""".formatted(PAPER_2, NAVY, rows));
        text.add(plain.toString().strip());
        return this;
    }

    /** Destaque em âmbar, como os avisos do front. */
    public EmailLayout callout(String value) {
        html.add("""
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:4px 0 20px;">
                  <tr><td style="background:%s;border:2px solid %s;border-left-width:6px;border-left-color:%s;border-radius:10px;padding:14px 16px;font-family:%s;font-size:15px;line-height:1.5;color:%s;">%s</td></tr>
                </table>""".formatted(AMBER_SOFT, NAVY, AMBER, SANS, NAVY, esc(value)));
        text.add(value);
        return this;
    }

    /** Observação em letra menor, no fim do cartão. */
    public EmailLayout note(String value) {
        html.add("<p style=\"margin:8px 0 0;font-family:" + SANS + ";font-size:14px;line-height:1.5;color:" + MUTED + ";\">"
                + esc(value) + "</p>");
        text.add(value);
        return this;
    }

    public EmailMessage build(String to, String subject) {
        return new EmailMessage(to, subject, renderText(), renderHtml(subject));
    }

    private String renderText() {
        StringBuilder sb = new StringBuilder();
        if (title != null) {
            sb.append(title).append("\n\n");
        }
        sb.append(String.join("\n\n", text));
        sb.append("\n\n—\nCarteirama · um sistema da suíte Parlandim TI\n").append(baseUrl);
        return sb.toString();
    }

    private String renderHtml(String subject) {
        String head = eyebrow == null ? "" : """
                <p style="margin:0 0 8px;font-family:%s;font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:%s;">%s</p>"""
                .formatted(MONO, BLUE, esc(eyebrow));
        String h1 = title == null ? "" : """
                <h1 style="margin:0 0 20px;font-family:%s;font-size:28px;line-height:1.15;font-weight:800;letter-spacing:-0.5px;color:%s;">%s</h1>"""
                .formatted(SANS, NAVY, esc(title));
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
                <title>%1$s</title>
                <link href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;700;800&family=JetBrains+Mono:wght@400;700&display=swap" rel="stylesheet">
                </head>
                <body style="margin:0;padding:0;background:%2$s;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%3$s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%2$s;">
                  <tr><td align="center" style="padding:24px 12px 40px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:600px;">
                      <tr><td style="background:%4$s;border-radius:16px 16px 0 0;padding:18px 24px;">
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                          <td style="padding-right:12px;"><img src="%5$s/email/logo.png" width="40" height="40" alt="" style="display:block;border:0;border-radius:10px;"></td>
                          <td>
                            <div style="font-family:%6$s;font-size:21px;font-weight:800;letter-spacing:-0.3px;color:#ffffff;line-height:1;">Carteirama</div>
                            <div style="font-family:%7$s;font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:#b7c0d4;padding-top:5px;">Suíte Parlandim TI</div>
                          </td>
                        </tr></table>
                      </td></tr>
                      <tr><td style="background:#ffffff;border:2px solid %4$s;border-top:0;border-right-width:6px;border-bottom-width:6px;border-radius:0 0 16px 16px;padding:32px 28px 28px;">
                        %8$s%9$s%10$s
                      </td></tr>
                      <tr><td style="padding:24px 8px 0;font-family:%7$s;font-size:11px;line-height:1.7;letter-spacing:1px;text-transform:uppercase;color:%11$s;">
                        Carteirama · um sistema da suíte Parlandim TI<br>
                        <a href="%5$s/termos" target="_blank" style="color:%11$s;">[Termos de uso]</a> &nbsp; <a href="%5$s/privacidade" target="_blank" style="color:%11$s;">[Privacidade]</a>
                      </td></tr>
                      <tr><td style="padding:10px 8px 0;font-family:%6$s;font-size:12px;line-height:1.5;color:%11$s;">
                        Você recebeu este e-mail porque tem uma conta na Carteirama. Este endereço não recebe respostas.
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(esc(subject), PAPER, esc(preheader), NAVY, attr(baseUrl), SANS, MONO, head, h1,
                String.join("\n", html), MUTED);
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private static String attr(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
