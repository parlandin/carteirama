package com.parlandim.carteirama.export;

import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.enums.ChargeStatus;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.repository.ChargeRepository;
import com.parlandim.carteirama.repository.ClientRepository;
import com.parlandim.carteirama.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    public enum Type { clients, charges, groups }

    public enum Format { xlsx, csv }

    public record File(String filename, String contentType, byte[] content) {
    }

    private final ClientRepository clients;
    private final ChargeRepository charges;
    private final GroupRepository groups;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public File export(UUID accountId, Type type, Format format, YearMonth month) {
        Sheet sheet = switch (type) {
            case clients -> clientsSheet(accountId);
            case charges -> chargesSheet(accountId, month);
            case groups -> groupsSheet(accountId, month);
        };
        String base = "carteirama-" + switch (type) {
            case clients -> "clientes";
            case charges -> "cobrancas-" + month;
            case groups -> "grupos-" + month;
        };
        return format == Format.csv
                ? new File(base + ".csv", "text/csv; charset=UTF-8", SheetWriters.csv(sheet))
                : new File(base + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                SheetWriters.xlsx(sheet));
    }

    private Sheet clientsSheet(UUID accountId) {
        List<List<Object>> rows = new ArrayList<>();
        List<Client> all = clients.findAll((root, q, cb) -> cb.equal(root.get("account").get("id"), accountId),
                Sort.by(Sort.Order.asc("name").ignoreCase()));
        for (Client c : all) {
            rows.add(Arrays.asList(c.getName(), c.getGroup().getName(), c.getDocument(), c.getEmail(), c.getPhone(),
                    new Sheet.Money(c.getAmountCents()), (int) c.getDueDay(), label(c.getStatus()), c.getNotes()));
        }
        return new Sheet("Clientes", List.of("Nome", "Grupo", "Documento", "E-mail", "Telefone", "Valor (R$)",
                "Dia de vencimento", "Status", "Observações"), rows);
    }

    private Sheet chargesSheet(UUID accountId, YearMonth month) {
        LocalDate today = clock.today();
        List<List<Object>> rows = new ArrayList<>();
        List<Charge> all = charges.findAll((root, q, cb) -> cb.and(
                cb.equal(root.get("account").get("id"), accountId),
                cb.equal(root.get("referenceMonth"), month.atDay(1))), Sort.by("dueDate", "id"));
        for (Charge c : all) {
            boolean late = c.getStatus() == ChargeStatus.pending && c.getDueDate().isBefore(today);
            rows.add(Arrays.asList(c.getClient().getName(), c.getClient().getGroup().getName(), month.toString(),
                    c.getDueDate(), new Sheet.Money(c.getAmountCents()), late ? "Atrasada" : label(c.getStatus()),
                    c.getPaidAt(), c.getNote()));
        }
        return new Sheet("Cobranças " + month, List.of("Cliente", "Grupo", "Mês", "Vencimento", "Valor (R$)",
                "Status", "Pago em", "Nota"), rows);
    }

    private Sheet groupsSheet(UUID accountId, YearMonth month) {
        List<List<Object>> rows = new ArrayList<>();
        for (var g : groups.listWithTotals(accountId, month.atDay(1))) {
            rows.add(Arrays.asList(g.name(), g.color(), g.clientCount(), new Sheet.Money(g.monthTotalCents()), g.notes()));
        }
        return new Sheet("Grupos " + month, List.of("Nome", "Cor", "Clientes", "Total do mês (R$)", "Observações"), rows);
    }

    private static String label(ClientStatus s) {
        return switch (s) {
            case active -> "Ativo";
            case paused -> "Pausado";
            case archived -> "Arquivado";
        };
    }

    private static String label(ChargeStatus s) {
        return switch (s) {
            case pending -> "Pendente";
            case paid -> "Paga";
            case cancelled -> "Cancelada";
        };
    }
}
