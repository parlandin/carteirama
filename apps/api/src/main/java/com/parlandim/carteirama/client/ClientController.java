package com.parlandim.carteirama.client;

import com.parlandim.carteirama.client.dto.ClientDetailResponse;
import com.parlandim.carteirama.client.dto.ClientResponse;
import com.parlandim.carteirama.client.dto.CreateClientRequest;
import com.parlandim.carteirama.client.dto.UpdateClientRequest;
import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.security.WriteAccess;
import com.parlandim.carteirama.common.web.PageResponse;
import com.parlandim.carteirama.common.web.Paging;
import com.parlandim.carteirama.model.enums.ClientStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public PageResponse<ClientResponse> list(@AuthenticationPrincipal AuthUser user,
                                             @RequestParam(name = "group_id", required = false) UUID groupId,
                                             @RequestParam(required = false) ClientStatus status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer limit) {
        var pageable = Paging.of(page, limit, Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id")));
        return clientService.list(user.requireAccountId(), groupId, status, q, pageable);
    }

    @PostMapping
    @WriteAccess
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateClientRequest body) {
        return clientService.create(user.requireAccountId(), body);
    }

    @GetMapping("/{id}")
    public ClientDetailResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return clientService.get(user.requireAccountId(), id);
    }

    @PatchMapping("/{id}")
    @WriteAccess
    public ClientResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                 @Valid @RequestBody UpdateClientRequest body) {
        return clientService.update(user.requireAccountId(), id, body);
    }

    @DeleteMapping("/{id}")
    @WriteAccess
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        clientService.archive(user.requireAccountId(), id);
    }
}
