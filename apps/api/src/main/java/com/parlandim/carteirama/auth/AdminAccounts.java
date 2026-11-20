package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.model.User;
import com.parlandim.carteirama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccounts {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public static boolean validEmail(String email) {
        return email != null && email.length() <= 254 && email.matches(Patterns.EMAIL);
    }

    public static boolean strongPassword(String password, String email) {
        return password != null && password.length() >= 8 && password.length() <= 72
                && password.matches(Patterns.PASSWORD_STRENGTH) && !password.equalsIgnoreCase(email);
    }

    public boolean exists(String email) {
        return users.existsByEmail(email);
    }

    public User create(String email, String name, String password) {
        User user = new User();
        user.setEmail(email);
        user.setName(name != null ? name : "Administrador");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAdmin(true);
        return users.saveAndFlush(user);
    }
}
