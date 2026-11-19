package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.auth.dto.ChangePasswordRequest;
import com.parlandim.carteirama.auth.dto.MeResponse;
import com.parlandim.carteirama.auth.dto.UpdateMeRequest;
import com.parlandim.carteirama.common.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public MeResponse get(@AuthenticationPrincipal AuthUser user) {
        return meService.get(user);
    }

    @PatchMapping
    public MeResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateMeRequest body) {
        return meService.update(user, body);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest body,
                               HttpServletRequest request) {
        meService.changePassword(user, body, request.getSession().getId());
    }
}
