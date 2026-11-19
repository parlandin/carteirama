package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.web.RequestIdFilter;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AccountAccessInterceptor implements HandlerInterceptor {

    private final AccountRepository accounts;
    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return true;
        }
        if (user.accountId() != null) {
            request.setAttribute(RequestIdFilter.ACCOUNT_ID_ATTR, user.accountId());
        }
        rateLimiter.hit(RateLimiter.USER_REQUESTS, user.userId().toString());

        if (handler instanceof HandlerMethod hm && hm.hasMethodAnnotation(WriteAccess.class)) {
            AccountStatus status = accounts.findStatusById(user.requireAccountId())
                    .orElseThrow(ApiException::forbidden);
            if (status == AccountStatus.suspended || status == AccountStatus.cancelled) {
                throw ApiException.accountSuspended();
            }
        }
        return true;
    }
}
