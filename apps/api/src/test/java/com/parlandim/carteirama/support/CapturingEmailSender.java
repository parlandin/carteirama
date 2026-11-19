package com.parlandim.carteirama.support;

import com.parlandim.carteirama.common.email.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Primary
@Component
public class CapturingEmailSender implements EmailSender {

    public record Email(String to, String subject, String body) {
    }

    private final List<Email> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new Email(to, subject, body));
    }

    public List<Email> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
