package com.parlandim.carteirama.export;

import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.group.dto.GroupListItem;
import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.Group;
import com.parlandim.carteirama.model.enums.ChargeKind;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public File export(UUID accountId, Collection<Type> requested, Format format, YearMonth month) {
        Set<Type> types = EnumSet.copyOf(requested);
        Sheet sheet;
        String base;
        if (types.size() == 1) {
            Type type = types.iterator().next();
            sheet = switch (type) {
                case clients -> clientsSheet(accountId);
                case charges -> chargesSheet(accountId, month);
                case groups -> groupsSheet(accountId, month);
            };
            base = "carteirama-" + switch (type) {
                case clients -> "clientes";
                case charges -> "cobrancas-" + month;
                case groups -> "grupos-" + month;
            };
        } else {
            sheet = combinedSheet(accountId, types, month);
            List<String> parts = new ArrayList<>();
            if (types.contains(Type.clients)) {
                parts.add("clientes");
            }
            if (types.contains(Type.groups)) {
                parts.add("grupos");
            }
            if (types.contains(Type.charges)) {
                parts.add("cobrancas");
            }
            base = "carteirama-" + String.join("-", parts) + "-" + month;
        }
        return format == Format.csv
                ? new File(base + ".csv", "text/csv; charset=UTF-8", SheetWriters.csv(sheet))
                : new File(base + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                SheetWriters.xlsx(sheet));
    }

    private record Line(String group, String client, List<Object> cells) {
    }

    private Sheet combinedSheet(UUID accountId, Set<Type> types, YearMonth month) {
        boolean withClients = types.contains(Type.clients);
        boolean withGroups = types.contains(Type.groups);
        boolean withCharges = types.contains(Type.charges);
        LocalDate today = clock.today();

        Map<UUID, GroupListItem> groupStats = new LinkedHashMap<>();
        if (withGroups) {
            for (GroupListItem g : groups.listWithTotals(accountId, month.atDay(1))) {
                groupStats.put(g.id(), g);
            }
        }
        List<Charge> monthCharges = withCharges ? monthCharges(accountId, month) : List.of();

        List<String> headers = new ArrayList<>(List.of("Cliente"));
        if (withClients) {
            headers.addAll(List.of("Documento", "E-mail", "Telefone", "Assinatura (R$)", "Dia de vencimento",
                    "Status do cliente", "Observações do cliente"));
        }
        headers.add("Grupo");
        if (withGroups) {
            headers.addAll(List.of("Cor do grupo", "Clientes no grupo", "Total do grupo no mês (R$)",
                    "Descrição do grupo"));
        }
        if (withCharges) {
            headers.addAll(List.of("Mês", "Cobrança", "Vencimento", "Serviço (R$)", "Bônus (R$)",
                    "Valor cobrado (R$)", "Custos (R$)", "Líquido (R$)", "Situação da cobrança", "Pago em",
                    "Nota da cobrança"));
        }

        List<Line> lines = new ArrayList<>();
        Set<UUID> groupsWithLines = new HashSet<>();

        if (withClients) {
            Map<UUID, List<Charge>> chargesByClient = new HashMap<>();
            for (Charge c : monthCharges) {
                if (c.getClient() != null) {
                    chargesByClient.computeIfAbsent(c.getClient().getId(), k -> new ArrayList<>()).add(c);
                }
            }
            for (Client c : allClients(accountId)) {
                List<Charge> clientCharges = chargesByClient.getOrDefault(c.getId(), List.of());
                List<Charge> perRow = withCharges && !clientCharges.isEmpty()
                        ? clientCharges : Collections.singletonList(null);
                for (Charge charge : perRow) {
                    List<Object> row = new ArrayList<>(Arrays.asList(c.getName(), c.getDocument(), c.getEmail(),
                            c.getPhone(), new Sheet.Money(c.getAmountCents()), (int) c.getDueDay(),
                            label(c.getStatus()), c.getNotes(), c.getGroup().getName()));
                    if (withGroups) {
                        row.addAll(groupCells(groupStats.get(c.getGroup().getId())));
                    }
                    if (withCharges) {
                        row.addAll(chargeCells(charge, month, today));
                    }
                    lines.add(new Line(c.getGroup().getName(), c.getName(), row));
                }
                groupsWithLines.add(c.getGroup().getId());
            }
            for (Charge c : monthCharges) {
                if (c.getClient() != null) {
                    continue;
                }
                Group group = c.getGroup();
                List<Object> row = new ArrayList<>();
                row.add(c.getPayer());
                row.addAll(Collections.nCopies(7, null));
                row.add(group == null ? null : group.getName());
                if (withGroups) {
                    row.addAll(groupCells(group == null ? null : groupStats.get(group.getId())));
                }
                row.addAll(chargeCells(c, month, today));
                if (group != null) {
                    groupsWithLines.add(group.getId());
                }
                lines.add(new Line(group == null ? "" : group.getName(), c.getPayer(), row));
            }
        } else {
            for (Charge c : monthCharges) {
                Group group = c.getEffectiveGroup();
                List<Object> row = new ArrayList<>(Arrays.asList(c.getPayer(), group == null ? null : group.getName()));
                if (withGroups) {
                    row.addAll(groupCells(group == null ? null : groupStats.get(group.getId())));
                }
                row.addAll(chargeCells(c, month, today));
                if (group != null) {
                    groupsWithLines.add(group.getId());
                }
                lines.add(new Line(group == null ? "" : group.getName(), c.getPayer(), row));
            }
        }

        if (withGroups) {
            for (GroupListItem g : groupStats.values()) {
                if (groupsWithLines.contains(g.id())) {
                    continue;
                }
                List<Object> row = new ArrayList<>();
                row.add(null);
                if (withClients) {
                    row.addAll(Collections.nCopies(7, null));
                }
                row.add(g.name());
                row.addAll(groupCells(g));
                if (withCharges) {
                    row.addAll(Collections.nCopies(11, null));
                }
                lines.add(new Line(g.name(), "", row));
            }
        }

        Comparator<String> ci = String.CASE_INSENSITIVE_ORDER;
        lines.sort(Comparator.comparing(Line::group, ci).thenComparing(Line::client, ci));
        return new Sheet("Carteirama " + month, headers, lines.stream().map(Line::cells).toList());
    }

    private static List<Object> groupCells(GroupListItem g) {
        if (g == null) {
            return Arrays.asList(null, null, null, null);
        }
        return Arrays.asList(g.color(), g.clientCount(), new Sheet.Money(g.monthTotalCents()), g.notes());
    }

    private static List<Object> chargeCells(Charge c, YearMonth month, LocalDate today) {
        if (c == null) {
            return Arrays.asList(month.toString(), null, null, null, null, null, null, null, null, null, null);
        }
        return Arrays.asList(month.toString(), chargeName(c), c.getDueDate(), new Sheet.Money(c.getAmountCents()),
                new Sheet.Money(c.getBonusCents()), new Sheet.Money(c.getTotalCents()), new Sheet.Money(c.getCostCents()),
                new Sheet.Money(c.getNetCents()), chargeLabel(c, today), c.getPaidAt(), c.getNote());
    }

    private static String chargeName(Charge c) {
        return c.getKind() == ChargeKind.one_off ? c.getDescription() : "Mensalidade";
    }

    private static String chargeLabel(Charge c, LocalDate today) {
        boolean late = c.getStatus() == ChargeStatus.pending && c.getDueDate().isBefore(today);
        return late ? "Atrasada" : label(c.getStatus());
    }

    private List<Client> allClients(UUID accountId) {
        return clients.findAll((root, q, cb) -> cb.equal(root.get("account").get("id"), accountId),
                Sort.by(Sort.Order.asc("name").ignoreCase()));
    }

    private List<Charge> monthCharges(UUID accountId, YearMonth month) {
        return charges.findAll((root, q, cb) -> cb.and(
                cb.equal(root.get("account").get("id"), accountId),
                cb.equal(root.get("referenceMonth"), month.atDay(1))), Sort.by("dueDate", "id"));
    }

    private Sheet clientsSheet(UUID accountId) {
        List<List<Object>> rows = new ArrayList<>();
        for (Client c : allClients(accountId)) {
            rows.add(Arrays.asList(c.getName(), c.getGroup().getName(), c.getDocument(), c.getEmail(), c.getPhone(),
                    new Sheet.Money(c.getAmountCents()), (int) c.getDueDay(), label(c.getStatus()), c.getNotes()));
        }
        return new Sheet("Clientes", List.of("Nome", "Grupo", "Documento", "E-mail", "Telefone", "Valor (R$)",
                "Dia de vencimento", "Status", "Observações"), rows);
    }

    private Sheet chargesSheet(UUID accountId, YearMonth month) {
        LocalDate today = clock.today();
        List<List<Object>> rows = new ArrayList<>();
        for (Charge c : monthCharges(accountId, month)) {
            rows.add(Arrays.asList(c.getPayer(), c.getEffectiveGroup() == null ? null : c.getEffectiveGroup().getName(), month.toString(),
                    chargeName(c), c.getDueDate(), new Sheet.Money(c.getAmountCents()),
                    new Sheet.Money(c.getBonusCents()), new Sheet.Money(c.getTotalCents()),
                    new Sheet.Money(c.getCostCents()), new Sheet.Money(c.getNetCents()), chargeLabel(c, today),
                    c.getPaidAt(), c.getNote()));
        }
        return new Sheet("Cobranças " + month, List.of("Cliente", "Grupo", "Mês", "Cobrança", "Vencimento", "Serviço (R$)", "Bônus (R$)",
                "Valor cobrado (R$)", "Custos (R$)", "Líquido (R$)",
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
