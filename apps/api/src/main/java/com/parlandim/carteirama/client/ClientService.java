package com.parlandim.carteirama.client;

import com.parlandim.carteirama.charge.dto.ChargeResponse;
import com.parlandim.carteirama.client.dto.ClientDetailResponse;
import com.parlandim.carteirama.client.dto.ClientResponse;
import com.parlandim.carteirama.client.dto.CreateClientRequest;
import com.parlandim.carteirama.client.dto.UpdateClientRequest;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.common.validation.Documents;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.model.Client;
import com.parlandim.carteirama.model.Group;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.ChargeRepository;
import com.parlandim.carteirama.repository.ClientRepository;
import com.parlandim.carteirama.repository.GroupRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clients;
    private final GroupRepository groups;
    private final ChargeRepository charges;
    private final AccountRepository accounts;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> list(UUID accountId, UUID groupId, ClientStatus status, String q, Pageable page) {
        Specification<Client> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("account").get("id"), accountId));
            if (groupId != null) {
                p.add(cb.equal(root.get("group").get("id"), groupId));
            }
            p.add(status != null ? cb.equal(root.get("status"), status)
                    : cb.notEqual(root.get("status"), ClientStatus.archived));
            if (q != null && !q.isBlank()) {
                String like = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("name")), like, '\\'));
                any.add(cb.like(cb.lower(root.get("email")), like, '\\'));
                String digits = q.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    any.add(cb.like(root.get("document"), "%" + digits + "%"));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.of(clients.findAll(spec, page), ClientResponse::of);
    }

    @Transactional(readOnly = true)
    public ClientDetailResponse get(UUID accountId, UUID id) {
        Client client = find(accountId, id);
        LocalDate today = clock.today();
        List<ChargeResponse> history = charges.findAllByAccountIdAndClientIdOrderByReferenceMonthDesc(accountId, id)
                .stream().map(c -> ChargeResponse.of(c, today)).toList();
        return new ClientDetailResponse(ClientResponse.of(client), history);
    }

    @Transactional
    public ClientResponse create(UUID accountId, CreateClientRequest req) {
        checkDocument(req.document());
        Client client = new Client();
        client.setAccount(accounts.getReferenceById(accountId));
        client.setGroup(activeGroup(accountId, req.groupId()));
        client.setName(req.name());
        client.setDocument(req.document());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        client.setAmountCents(req.amountCents());
        client.setDueDay(req.dueDay().shortValue());
        client.setStatus(ClientStatus.active);
        client.setNotes(req.notes());
        return ClientResponse.of(clients.saveAndFlush(client));
    }

    @Transactional
    public ClientResponse update(UUID accountId, UUID id, UpdateClientRequest req) {
        Client client = find(accountId, id);
        if (client.getStatus() == ClientStatus.archived) {
            throw ApiException.conflict("client_not_active", "O cliente está arquivado.");
        }
        if (req.getStatus() == ClientStatus.archived) {
            throw ValidationException.of("status", "invalid_value", "Para arquivar, use DELETE /clients/:id.");
        }
        if (req.getDocument() != null) {
            checkDocument(req.getDocument().orElse(null));
            client.setDocument(req.getDocument().orElse(null));
        }
        if (req.getName() != null) {
            client.setName(req.getName());
        }
        if (req.getGroupId() != null && !req.getGroupId().equals(client.getGroup().getId())) {
            client.setGroup(activeGroup(accountId, req.getGroupId()));
        }
        if (req.getEmail() != null) {
            client.setEmail(req.getEmail().orElse(null));
        }
        if (req.getPhone() != null) {
            client.setPhone(req.getPhone().orElse(null));
        }
        if (req.getAmountCents() != null) {
            client.setAmountCents(req.getAmountCents());
        }
        if (req.getDueDay() != null) {
            client.setDueDay(req.getDueDay().shortValue());
        }
        if (req.getStatus() != null) {
            client.setStatus(req.getStatus());
        }
        if (req.getNotes() != null) {
            client.setNotes(req.getNotes().orElse(null));
        }
        return ClientResponse.of(clients.saveAndFlush(client));
    }

    @Transactional
    public void archive(UUID accountId, UUID id) {
        Client client = find(accountId, id);
        if (client.getStatus() == ClientStatus.archived) {
            return;
        }
        client.setStatus(ClientStatus.archived);
        charges.cancelPendingByClient(accountId, id);
    }

    public Client find(UUID accountId, UUID id) {
        return clients.findByIdAndAccountId(id, accountId).orElseThrow(ApiException::notFound);
    }

    private Group activeGroup(UUID accountId, UUID groupId) {
        return groups.findByIdAndAccountId(groupId, accountId)
                .filter(g -> g.getArchivedAt() == null)
                .orElseThrow(ApiException::notFound);
    }

    private static void checkDocument(String document) {
        if (document != null && !Documents.isValidCpfOrCnpj(document)) {
            throw ValidationException.of("document", "invalid_document", "CPF ou CNPJ inválido.");
        }
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
