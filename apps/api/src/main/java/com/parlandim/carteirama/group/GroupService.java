package com.parlandim.carteirama.group;

import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.group.dto.CreateGroupRequest;
import com.parlandim.carteirama.group.dto.GroupDetailResponse;
import com.parlandim.carteirama.group.dto.GroupListItem;
import com.parlandim.carteirama.group.dto.GroupResponse;
import com.parlandim.carteirama.group.dto.UpdateGroupRequest;
import com.parlandim.carteirama.model.Group;
import com.parlandim.carteirama.model.enums.ClientStatus;
import com.parlandim.carteirama.repository.AccountRepository;
import com.parlandim.carteirama.repository.ClientRepository;
import com.parlandim.carteirama.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groups;
    private final ClientRepository clients;
    private final AccountRepository accounts;
    private final BusinessClock clock;

    @Transactional(readOnly = true)
    public List<GroupListItem> list(UUID accountId, YearMonth month) {
        return groups.listWithTotals(accountId, month.atDay(1));
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse get(UUID accountId, UUID id) {
        Group group = find(accountId, id);
        return GroupDetailResponse.of(group, clients.findAllByAccountIdAndGroupIdAndStatusNotOrderByNameAsc(
                accountId, id, ClientStatus.archived));
    }

    @Transactional
    public GroupResponse create(UUID accountId, CreateGroupRequest req) {
        requireNameAvailable(accountId, req.name(), null);
        Group group = new Group();
        group.setAccount(accounts.getReferenceById(accountId));
        group.setName(req.name());
        group.setColor(req.color());
        group.setNotes(req.notes());
        return GroupResponse.of(groups.saveAndFlush(group));
    }

    @Transactional
    public GroupResponse update(UUID accountId, UUID id, UpdateGroupRequest req) {
        Group group = find(accountId, id);
        if (req.getName() != null && !req.getName().equals(group.getName())) {
            if (group.getArchivedAt() == null) {
                requireNameAvailable(accountId, req.getName(), group.getId());
            }
            group.setName(req.getName());
        }
        if (req.getColor() != null) {
            group.setColor(req.getColor().orElse(null));
        }
        if (req.getNotes() != null) {
            group.setNotes(req.getNotes().orElse(null));
        }
        return GroupResponse.of(groups.saveAndFlush(group));
    }

    @Transactional
    public void archive(UUID accountId, UUID id) {
        Group group = find(accountId, id);
        if (group.getArchivedAt() != null) {
            return;
        }
        if (clients.existsByAccountIdAndGroupIdAndStatus(accountId, id, ClientStatus.active)) {
            throw ApiException.conflict("group_not_empty", "O grupo tem clientes ativos. Mova ou arquive os clientes antes.");
        }
        group.setArchivedAt(clock.now());
    }

    Group find(UUID accountId, UUID id) {
        return groups.findByIdAndAccountId(id, accountId).orElseThrow(ApiException::notFound);
    }

    private void requireNameAvailable(UUID accountId, String name, UUID exceptId) {
        boolean taken = exceptId == null
                ? groups.existsByAccountIdAndNameIgnoreCaseAndArchivedAtIsNull(accountId, name)
                : groups.existsByAccountIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(accountId, name, exceptId);
        if (taken) {
            throw ApiException.conflict("group_name_taken", "Já existe um grupo com este nome.");
        }
    }
}
