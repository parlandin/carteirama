package com.parlandim.carteirama.client;

import com.parlandim.carteirama.client.dto.CreateExtraRequest;
import com.parlandim.carteirama.client.dto.ExtraResponse;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.ClientExtra;
import com.parlandim.carteirama.model.enums.ChargeKind;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.model.enums.ExtraKind;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.ChargeRepository;
import com.parlandim.carteirama.repository.ClientExtraRepository;
import com.parlandim.carteirama.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientExtraService {

    private final ClientRepository clients;
    private final ClientExtraRepository extras;
    private final ChargeRepository charges;
    private final AccountRepository accounts;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public List<ExtraResponse> list(UUID accountId, UUID clientId) {
        YearMonth since = clock.currentMonth().minusMonths(12);
        return extras.listVisible(accountId, clientId, since.atDay(1)).stream().map(ExtraResponse::of).toList();
    }

    @Transactional
    public ExtraResponse create(UUID accountId, UUID clientId, CreateExtraRequest req) {
        Client client = clients.findByIdAndAccountId(clientId, accountId).orElseThrow(ApiException::notFound);
        if (client.getStatus() == ClientStatus.archived) {
            throw ApiException.conflict("client_not_active", "O cliente está arquivado.");
        }
        boolean bonus = req.kind() == ExtraKind.bonus;
        ClientExtra extra = new ClientExtra();
        extra.setAccount(accounts.getReferenceById(accountId));
        extra.setClient(client);
        extra.setKind(req.kind());
        extra.setDescription(req.description());
        extra.setAmountCents(req.amountCents());
        if (req.month() == null || req.month().isBlank()) {
            YearMonth start = clock.currentMonth();
            if (bonus && paid(clientId, start)) {
                start = start.plusMonths(1);
            }
            extra.setRecurring(true);
            extra.setStartMonth(start.atDay(1));
        } else {
            YearMonth month = clock.parseMonth("month", req.month());
            if (bonus) {
                requireNotPaid(clientId, month);
            }
            extra.setRecurring(false);
            extra.setStartMonth(month.atDay(1));
            extra.setEndMonth(month.atDay(1));
        }
        return ExtraResponse.of(extras.saveAndFlush(extra));
    }

    @Transactional
    public void remove(UUID accountId, UUID clientId, UUID id) {
        ClientExtra extra = extras.findByIdAndAccountIdAndClientId(id, accountId, clientId)
                .orElseThrow(ApiException::notFound);
        boolean bonus = extra.getKind() == ExtraKind.bonus;
        YearMonth start = YearMonth.from(extra.getStartMonth());
        if (!extra.isRecurring()) {
            if (bonus) {
                requireNotPaid(clientId, start);
            }
            extras.delete(extra);
            return;
        }
        if (extra.getEndMonth() != null) {
            return;
        }
        YearMonth current = clock.currentMonth();
        YearMonth last = bonus && paid(clientId, current) ? current : current.minusMonths(1);
        if (start.isAfter(last)) {
            extras.delete(extra);
        } else {
            extra.setEndMonth(last.atDay(1));
        }
    }

    private boolean paid(UUID clientId, YearMonth month) {
        return charges.findFirstByClientIdAndReferenceMonthAndKind(clientId, month.atDay(1), ChargeKind.recurring)
                .map(c -> c.getStatus() == ChargeStatus.paid).orElse(false);
    }

    private void requireNotPaid(UUID clientId, YearMonth month) {
        if (paid(clientId, month)) {
            throw ApiException.conflict("charge_already_paid",
                    "A mensalidade deste mês já foi paga. Desfaça o pagamento para mudar os bônus dela.");
        }
    }
}
