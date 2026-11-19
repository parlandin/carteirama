package com.parlandim.carteirama.charge;

import com.parlandim.carteirama.charge.dto.ChargeResponse;
import com.parlandim.carteirama.charge.dto.CreateChargeRequest;
import com.parlandim.carteirama.charge.dto.SummaryResponse;
import com.parlandim.carteirama.charge.dto.UpdateChargeRequest;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.enums.ChargeKind;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.ChargeRepository;
import com.parlandim.carteirama.repository.ClientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChargeService {

    public enum StatusFilter { pending, paid, cancelled, late }

    private final ChargeRepository charges;
    private final ClientRepository clients;
    private final AccountRepository accounts;
    private final BusinessClock clock;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<ChargeResponse> list(UUID accountId, YearMonth month, StatusFilter status, UUID groupId,
                                             UUID clientId, Pageable page) {
        LocalDate today = clock.today();
        Specification<Charge> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("account").get("id"), accountId));
            p.add(cb.equal(root.get("referenceMonth"), month.atDay(1)));
            if (clientId != null) {
                p.add(cb.equal(root.get("client").get("id"), clientId));
            }
            if (groupId != null) {
                p.add(cb.equal(root.get("client").get("group").get("id"), groupId));
            }
            if (status != null) {
                switch (status) {
                    case paid -> p.add(cb.equal(root.get("status"), ChargeStatus.paid));
                    case cancelled -> p.add(cb.equal(root.get("status"), ChargeStatus.cancelled));
                    case pending -> {
                        p.add(cb.equal(root.get("status"), ChargeStatus.pending));
                        p.add(cb.greaterThanOrEqualTo(root.get("dueDate"), today));
                    }
                    case late -> {
                        p.add(cb.equal(root.get("status"), ChargeStatus.pending));
                        p.add(cb.lessThan(root.get("dueDate"), today));
                    }
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(charges.findAll(spec, page), c -> ChargeResponse.of(c, today));
    }

    @Transactional
    public int generate(UUID accountId, YearMonth month) {
        return charges.generateMonth(accountId, month.atDay(1));
    }

    @Transactional
    public ChargeResponse create(UUID accountId, CreateChargeRequest req) {
        Client client = clients.findByIdAndAccountId(req.clientId(), accountId).orElseThrow(ApiException::notFound);
        if (client.getStatus() != ClientStatus.active) {
            throw ApiException.conflict("client_not_active", "O cliente não está ativo.");
        }
        boolean oneOff = req.kind() == ChargeKind.one_off;
        if (oneOff && req.description() == null) {
            throw ValidationException.of("description", "required", "Descreva a cobrança avulsa.");
        }
        LocalDate referenceMonth = req.dueDate().withDayOfMonth(1);
        if (!oneOff && charges.existsByClientIdAndReferenceMonthAndKind(client.getId(), referenceMonth,
                ChargeKind.recurring)) {
            throw ApiException.conflict("charge_already_exists", "Este cliente já tem cobrança neste mês.");
        }
        Charge charge = new Charge();
        charge.setAccount(accounts.getReferenceById(accountId));
        charge.setClient(client);
        charge.setReferenceMonth(referenceMonth);
        charge.setAmountCents(req.amountCents());
        charge.setDueDate(req.dueDate());
        charge.setStatus(ChargeStatus.pending);
        charge.setNote(req.note());
        charge.setKind(req.kind());
        charge.setDescription(oneOff ? req.description() : null);
        charges.saveAndFlush(charge);
        entityManager.refresh(charge);
        return ChargeResponse.of(charge, clock.today());
    }

    @Transactional
    public ChargeResponse update(UUID accountId, UUID id, UpdateChargeRequest req) {
        Charge charge = find(accountId, id);
        requirePending(charge);
        if (req.getAmountCents() != null) {
            charge.setAmountCents(req.getAmountCents());
        }
        if (req.getDueDate() != null) {
            charge.setDueDate(req.getDueDate());
        }
        if (req.getNote() != null) {
            charge.setNote(req.getNote().orElse(null));
        }
        if (req.getDescription() != null) {
            if (charge.getKind() != ChargeKind.one_off) {
                throw ValidationException.of("description", "not_allowed", "Só cobranças avulsas têm descrição.");
            }
            if (req.getDescription().isEmpty()) {
                throw ValidationException.of("description", "required", "Descreva a cobrança avulsa.");
            }
            charge.setDescription(req.getDescription());
        }
        return ChargeResponse.of(charges.saveAndFlush(charge), clock.today());
    }

    @Transactional
    public ChargeResponse pay(UUID accountId, UUID id, LocalDate paidAt) {
        Charge charge = find(accountId, id);
        if (charge.getStatus() == ChargeStatus.paid) {
            throw ApiException.conflict("charge_already_paid", "Esta cobrança já está paga.");
        }
        requirePending(charge);
        LocalDate today = clock.today();
        LocalDate date = paidAt != null ? paidAt : today;
        if (date.isAfter(today)) {
            throw ValidationException.of("paid_at", "out_of_range", "A data de pagamento não pode ser futura.");
        }
        charge.setStatus(ChargeStatus.paid);
        charge.setPaidAt(date);
        return ChargeResponse.of(charge, today);
    }

    @Transactional
    public ChargeResponse unpay(UUID accountId, UUID id) {
        Charge charge = find(accountId, id);
        if (charge.getStatus() != ChargeStatus.paid) {
            throw ApiException.conflict("charge_not_editable", "Esta cobrança não está paga.");
        }
        charge.setStatus(ChargeStatus.pending);
        charge.setPaidAt(null);
        return ChargeResponse.of(charge, clock.today());
    }

    @Transactional
    public void cancel(UUID accountId, UUID id) {
        Charge charge = find(accountId, id);
        if (charge.getStatus() == ChargeStatus.cancelled) {
            return;
        }
        requirePending(charge);
        charge.setStatus(ChargeStatus.cancelled);
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID accountId, YearMonth month) {
        LocalDate today = clock.today();
        long paid = 0, pending = 0, late = 0, cost = 0;
        int paidN = 0, pendingN = 0, lateN = 0;
        for (Object[] row : charges.monthFigures(accountId, month.atDay(1), ChargeStatus.cancelled)) {
            ChargeStatus status = (ChargeStatus) row[0];
            LocalDate due = (LocalDate) row[1];
            long amount = (Integer) row[2] + (Long) row[3];
            cost += (Long) row[4];
            if (status == ChargeStatus.paid) {
                paid += amount;
                paidN++;
            } else if (due.isBefore(today)) {
                late += amount;
                lateN++;
            } else {
                pending += amount;
                pendingN++;
            }
        }
        long total = paid + pending + late;
        return new SummaryResponse(month.toString(), total, paid, pending, late,
                paidN + pendingN + lateN, paidN, pendingN, lateN, cost, total - cost);
    }

    private Charge find(UUID accountId, UUID id) {
        return charges.findByIdAndAccountId(id, accountId).orElseThrow(ApiException::notFound);
    }

    private static void requirePending(Charge charge) {
        if (charge.getStatus() != ChargeStatus.pending) {
            throw ApiException.conflict("charge_not_editable", "Só é possível alterar cobranças pendentes.");
        }
    }
}
