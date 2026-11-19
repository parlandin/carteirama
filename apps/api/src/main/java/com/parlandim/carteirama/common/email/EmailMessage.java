package com.parlandim.carteirama.common.email;

/** E-mail pronto para envio: a versão em texto vai junto para clientes que não mostram HTML. */
public record EmailMessage(String to, String subject, String text, String html) {
}
