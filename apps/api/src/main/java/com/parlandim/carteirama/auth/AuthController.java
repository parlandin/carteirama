package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.auth.dto.ForgotPasswordRequest;
import com.parlandim.carteirama.auth.dto.LoginRequest;
import com.parlandim.carteirama.auth.dto.MeResponse;
import com.parlandim.carteirama.auth.dto.RegisterRequest;
import com.parlandim.carteirama.auth.dto.ResetPasswordRequest;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MeService meService;
    private final SessionService sessionService;
    private final RateLimiter rateLimiter;

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request,
                               HttpServletResponse response) {
        rateLimiter.hit(RateLimiter.REGISTER, request.getRemoteAddr());
        AuthUser user = authService.register(body);
        sessionService.open(request, response, user, false);
        return meService.get(user);
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                            HttpServletResponse response) {
        AuthUser user = authService.login(body);
        sessionService.open(request, response, user, Boolean.TRUE.equals(body.remember()));
        return meService.get(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        sessionService.close(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest request) {
        rateLimiter.hit(RateLimiter.FORGOT_PASSWORD, request.getRemoteAddr());
        authService.forgotPassword(body.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        authService.resetPassword(body.token(), body.password());
    }
}
