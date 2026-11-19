package com.parlandim.carteirama.charge;

import com.parlandim.carteirama.charge.dto.ChargeResponse;
import com.parlandim.carteirama.charge.dto.CreateChargeRequest;
import com.parlandim.carteirama.charge.dto.GenerateChargesRequest;
import com.parlandim.carteirama.charge.dto.PayChargeRequest;
import com.parlandim.carteirama.charge.dto.SummaryResponse;
import com.parlandim.carteirama.charge.dto.UpdateChargeRequest;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.security.WriteAccess;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.common.web.Paging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final BusinessClock clock;

    public record GenerateResponse(String month, int created) {
    }

    @GetMapping("/charges")
    public PageResponse<ChargeResponse> list(@AuthenticationPrincipal AuthUser user,
                                             @RequestParam(required = false) String month,
                                             @RequestParam(required = false) ChargeService.StatusFilter status,
                                             @RequestParam(name = "group_id", required = false) UUID groupId,
                                             @RequestParam(name = "client_id", required = false) UUID clientId,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer limit) {
        var ym = clock.parseMonth("month", month);
        var pageable = Paging.of(page, limit, Sort.by("dueDate", "id"));
        return chargeService.list(user.requireAccountId(), ym, status, groupId, clientId, pageable);
    }

    @PostMapping("/charges/generate")
    @WriteAccess
    public GenerateResponse generate(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody GenerateChargesRequest body) {
        var ym = clock.parseMonth("month", body.month());
        return new GenerateResponse(ym.toString(), chargeService.generate(user.requireAccountId(), ym));
    }

    @PostMapping("/charges")
    @WriteAccess
    @ResponseStatus(HttpStatus.CREATED)
    public ChargeResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateChargeRequest body) {
        return chargeService.create(user.requireAccountId(), body);
    }

    @PatchMapping("/charges/{id}")
    @WriteAccess
    public ChargeResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateChargeRequest body) {
        return chargeService.update(user.requireAccountId(), id, body);
    }

    @PostMapping("/charges/{id}/pay")
    @WriteAccess
    public ChargeResponse pay(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                              @RequestBody(required = false) PayChargeRequest body) {
        return chargeService.pay(user.requireAccountId(), id, body == null ? null : body.paidAt());
    }

    @PostMapping("/charges/{id}/unpay")
    @WriteAccess
    public ChargeResponse unpay(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return chargeService.unpay(user.requireAccountId(), id);
    }

    @DeleteMapping("/charges/{id}")
    @WriteAccess
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        chargeService.cancel(user.requireAccountId(), id);
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@AuthenticationPrincipal AuthUser user, @RequestParam(required = false) String month) {
        return chargeService.summary(user.requireAccountId(), clock.parseMonth("month", month));
    }
}
