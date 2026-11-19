package com.parlandim.carteirama.account;

import com.parlandim.carteirama.model.AuditLog;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.AuditLogRepository;
import com.parlandim.carteirama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final AccountRepository accounts;
    private final UserRepository users;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID accountId, UUID actorUserId, String action, Map<String, Object> meta) {
        AuditLog log = new AuditLog();
        log.setAccount(accountId == null ? null : accounts.getReferenceById(accountId));
        log.setActorUser(actorUserId == null ? null : users.getReferenceById(actorUserId));
        log.setAction(action);
        log.setMeta(meta == null ? new HashMap<>() : new HashMap<>(meta));
        auditLogs.save(log);
    }
}
