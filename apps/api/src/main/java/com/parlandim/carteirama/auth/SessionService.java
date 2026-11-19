package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    static final Duration REMEMBER_ME = Duration.ofDays(30);
    static final Duration DEFAULT = Duration.ofHours(12);

    private final SecurityContextRepository contextRepository;
    private final FindByIndexNameSessionRepository<?> sessions;

    public void open(HttpServletRequest request, HttpServletResponse response, AuthUser user, boolean remember) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval((int) (remember ? REMEMBER_ME : DEFAULT).toSeconds());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.admin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    public void close(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public void closeAllOf(UUID userId, String exceptSessionId) {
        for (String id : sessions.findByPrincipalName(userId.toString()).keySet()) {
            if (!id.equals(exceptSessionId)) {
                sessions.deleteById(id);
            }
        }
    }
}
