package com.parlandim.carteirama.admin.dto;

import com.parlandim.carteirama.billing.dto.InvoiceResponse;
import com.parlandim.carteirama.common.validation.Text;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.AuditLog;
import com.parlandim.carteirama.model.Plan;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.PaymentMethod;
import com.parlandim.carteirama.model.enums.PersonType;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AccountItem(UUID id, PersonType personType, String document, String legalName, String companyName,
                              String email, String userName, AccountStatus status, String displayStatus,
                              Instant trialEndsAt, Instant suspendedAt, Instant cancelledAt, Instant createdAt) {
        public static AccountItem of(Account a, boolean late) {
            return new AccountItem(a.getId(), a.getPersonType(), a.getDocument(), a.getLegalName(), a.getCompanyName(),
                    a.getUser().getEmail(), a.getUser().getName(), a.getStatus(),
                    late && a.getStatus() == AccountStatus.active ? "late" : a.getStatus().name(),
                    a.getTrialEndsAt(), a.getSuspendedAt(), a.getCancelledAt(), a.getCreatedAt());
        }
    }

    public record SubscriptionView(String planCode, SubscriptionStatus status, int priceCents, LocalDate nextDueDate,
                                   boolean cancelAtPeriodEnd) {
        public static SubscriptionView of(Subscription s) {
            return s == null ? null : new SubscriptionView(s.getPlan().getCode(), s.getStatus(), s.getPriceCents(),
                    s.getNextDueDate(), s.isCancelAtPeriodEnd());
        }
    }

    public record AuditEntry(UUID id, String action, UUID actorUserId, String actorName, Map<String, Object> meta,
                             Instant createdAt) {
        public static AuditEntry of(AuditLog l) {
            return new AuditEntry(l.getId(), l.getAction(),
                    l.getActorUser() == null ? null : l.getActorUser().getId(),
                    l.getActorUser() == null ? null : l.getActorUser().getName(),
                    l.getMeta(), l.getCreatedAt());
        }
    }

    public record AccountDetail(AccountItem account, String phone, String zip, String address, String city,
                                String state, SubscriptionView subscription, List<InvoiceResponse> invoices,
                                List<AuditEntry> auditLog) {
    }

    public record AdminInvoiceItem(InvoiceResponse invoice, UUID accountId, String accountName) {
    }

    public record PlanView(String code, String name, int priceCents, int trialDays, boolean active) {
        public static PlanView of(Plan p) {
            return new PlanView(p.getCode(), p.getName(), p.getPriceCents(), p.getTrialDays(), p.isActive());
        }
    }

    public record Summary(long mrrCents, Map<String, Long> accountsByStatus, InvoicesMonth invoicesMonth,
                          List<MonthValue> mrrByMonth) {
    }

    public record InvoicesMonth(String month, long paidCents, long pendingCents, long lateCents) {
    }

    public record MonthValue(String month, long cents) {
    }

    public record SuspendRequest(@NotBlank @Size(min = 3, max = 200) String reason) {
        public SuspendRequest {
            reason = Text.trim(reason);
        }
    }

    public record MarkPaidRequest(@NotNull LocalDate paidAt, @NotNull PaymentMethod method) {
    }

    public record UpdatePlanRequest(@NotNull @Min(100) @Max(999_999) Integer priceCents) {
    }
}
