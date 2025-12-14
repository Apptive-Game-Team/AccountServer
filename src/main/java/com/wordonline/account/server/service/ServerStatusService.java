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

    @Getter
    private ServerState currentState = ServerState.ACTIVE;

    public Mono<Void> setServerStatus(ServerState state) {
        currentState = state;
        return serverRepository.findByDomainAndPort(domain, port)
                .defaultIfEmpty(new Server(protocol, domain, port, ServerType.ACCOUNT, state))
                .map(server -> {
                    server.setState(state);
                    return server;
                })
                .flatMap(serverRepository::save)
                .then();
    }
}
