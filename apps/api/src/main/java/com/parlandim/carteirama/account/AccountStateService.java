package com.parlandim.carteirama.account;

import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.AuditLog;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountStateService {

    public static final String SUSPEND = "account.suspend";
    public static final String REACTIVATE = "account.reactivate";

    private final AuditService audit;
    private final AuditLogRepository auditLogs;
    private final BusinessClock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void suspend(Account account, UUID actorUserId, String reason) {
        if (account.getStatus() != AccountStatus.trial && account.getStatus() != AccountStatus.active) {
            throw invalidState();
        }
        AccountStatus from = account.getStatus();
        account.setStatus(AccountStatus.suspended);
        account.setSuspendedAt(clock.now());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("reason", reason);
        meta.put("from", from.name());
        meta.put("automatic", actorUserId == null);
        audit.record(account.getId(), actorUserId, SUSPEND, meta);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reactivate(Account account, UUID actorUserId) {
        if (account.getStatus() != AccountStatus.suspended) {
            throw invalidState();
        }
        account.setStatus(AccountStatus.active);
        account.setSuspendedAt(null);
        audit.record(account.getId(), actorUserId, REACTIVATE, Map.of("automatic", actorUserId == null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onPaymentConfirmed(Account account) {
        if (account.getStatus() == AccountStatus.trial) {
            account.setStatus(AccountStatus.active);
        } else if (account.getStatus() == AccountStatus.suspended && !isManuallySuspended(account)) {
            reactivate(account, null);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(Account account) {
        if (account.getStatus() == AccountStatus.cancelled) {
            return;
        }
        account.setStatus(AccountStatus.cancelled);
        account.setCancelledAt(clock.now());
    }

    public boolean isManuallySuspended(Account account) {
        return auditLogs.findFirstByAccountIdAndActionInOrderByCreatedAtDesc(account.getId(), List.of(SUSPEND, REACTIVATE))
                .filter(log -> SUSPEND.equals(log.getAction()))
                .map(AuditLog::getActorUser)
                .isPresent();
    }

    private static ApiException invalidState() {
        return ApiException.conflict("invalid_account_state", "A conta não está num estado que permita esta ação.");
    }
}
