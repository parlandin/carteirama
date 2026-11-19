package com.parlandim.carteirama.group;

import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.security.WriteAccess;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.group.dto.CreateGroupRequest;
import com.parlandim.carteirama.group.dto.GroupDetailResponse;
import com.parlandim.carteirama.group.dto.GroupListItem;
import com.parlandim.carteirama.group.dto.GroupResponse;
import com.parlandim.carteirama.group.dto.UpdateGroupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final BusinessClock clock;

    public record GroupList(String month, List<GroupListItem> data) {
    }

    @GetMapping
    public GroupList list(@AuthenticationPrincipal AuthUser user, @RequestParam(required = false) String month) {
        var ym = clock.parseMonth("month", month);
        return new GroupList(ym.toString(), groupService.list(user.requireAccountId(), ym));
    }

    @PostMapping
    @WriteAccess
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateGroupRequest body) {
        return groupService.create(user.requireAccountId(), body);
    }

    @GetMapping("/{id}")
    public GroupDetailResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return groupService.get(user.requireAccountId(), id);
    }

    @PatchMapping("/{id}")
    @WriteAccess
    public GroupResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody UpdateGroupRequest body) {
        return groupService.update(user.requireAccountId(), id, body);
    }

    @DeleteMapping("/{id}")
    @WriteAccess
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        groupService.archive(user.requireAccountId(), id);
    }
}
