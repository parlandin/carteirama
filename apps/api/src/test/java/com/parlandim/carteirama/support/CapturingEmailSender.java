package com.parlandim.carteirama.support;

import com.parlandim.carteirama.common.email.EmailMessage;
import com.parlandim.carteirama.common.email.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Primary
@Component
public class CapturingEmailSender implements EmailSender {

    public record Email(String to, String subject, String body, String html) {
    }

    private final List<Email> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(EmailMessage message) {
        sent.add(new Email(message.to(), message.subject(), message.text(), message.html()));
    }

    public List<Email> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
