package com.parlandim.carteirama.admin;

import com.parlandim.carteirama.admin.dto.AdminDtos;
import com.parlandim.carteirama.billing.dto.InvoiceResponse;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.common.web.Paging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final BusinessClock clock;

    public record PlanList(List<AdminDtos.PlanView> data) {
    }

    @GetMapping("/summary")
    public AdminDtos.Summary summary() {
        return adminService.summary();
    }

    @GetMapping("/accounts")
    public PageResponse<AdminDtos.AccountItem> accounts(@RequestParam(required = false) AdminService.AccountFilter status,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer limit) {
        return adminService.accounts(status, q, Paging.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/accounts/{id}")
    public AdminDtos.AccountDetail account(@PathVariable UUID id) {
        return adminService.account(id);
    }

    @PostMapping("/accounts/{id}/suspend")
    public AdminDtos.AccountDetail suspend(@AuthenticationPrincipal AuthUser admin, @PathVariable UUID id,
                                           @Valid @RequestBody AdminDtos.SuspendRequest body) {
        return adminService.suspend(admin.userId(), id, body.reason());
    }

    @PostMapping("/accounts/{id}/reactivate")
    public AdminDtos.AccountDetail reactivate(@AuthenticationPrincipal AuthUser admin, @PathVariable UUID id) {
        return adminService.reactivate(admin.userId(), id);
    }

    @GetMapping("/invoices")
    public PageResponse<AdminDtos.AdminInvoiceItem> invoices(@RequestParam(required = false) AdminService.InvoiceFilter status,
                                                             @RequestParam(required = false) String month,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer limit) {
        var ym = month == null ? null : clock.parseMonth("month", month);
        return adminService.invoices(status, ym, Paging.of(page, limit, Sort.by(Sort.Direction.DESC, "dueDate")));
    }

    @PostMapping("/invoices/{id}/mark-paid")
    public InvoiceResponse markPaid(@AuthenticationPrincipal AuthUser admin, @PathVariable UUID id,
                                    @Valid @RequestBody AdminDtos.MarkPaidRequest body) {
        return adminService.markPaid(admin.userId(), id, body.paidAt(), body.method());
    }

    @GetMapping("/plans")
    public PlanList plans() {
        return new PlanList(adminService.plans());
    }

    @PatchMapping("/plans/{code}")
    public AdminDtos.PlanView updatePlan(@AuthenticationPrincipal AuthUser admin, @PathVariable String code,
                                         @Valid @RequestBody AdminDtos.UpdatePlanRequest body) {
        return adminService.updatePlanPrice(admin.userId(), code, body.priceCents());
    }
}
