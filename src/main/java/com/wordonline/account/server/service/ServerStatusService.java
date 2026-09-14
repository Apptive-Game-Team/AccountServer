package com.wordonline.account.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wordonline.account.server.entity.Server;
import com.wordonline.account.server.entity.ServerState;
import com.wordonline.account.server.entity.ServerType;
import com.wordonline.account.server.repository.ServerRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ServerStatusService {

    private final ServerRepository serverRepository;

    @Value("${server.external-port}")
    private Integer port;

    @Value("${server.domain}")
    private String domain;

    @Value("${server.protocol}")
    private String protocol;

    @Value("${server.internal-base-url}")
    private String internalBaseUrl;

    @Getter
    private ServerState currentState = ServerState.ACTIVE;

    public Mono<Void> setServerStatus(ServerState state) {
        currentState = state;
        String resolvedInternalBaseUrl = normalizeInternalBaseUrl(internalBaseUrl);
        return serverRepository.findByDomainAndPort(domain, port)
                .defaultIfEmpty(new Server(protocol, domain, port, ServerType.ACCOUNT, resolvedInternalBaseUrl, state))
                .map(server -> {
                    server.setState(state);
                    server.setInternalBaseUrl(resolvedInternalBaseUrl);
                    return server;
                })
                .flatMap(serverRepository::save)
                .then();
    }

    private String normalizeInternalBaseUrl(String rawInternalBaseUrl) {
        if (rawInternalBaseUrl == null) {
            return null;
        }
        String trimmed = rawInternalBaseUrl.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("/+$", "");
    }
}
