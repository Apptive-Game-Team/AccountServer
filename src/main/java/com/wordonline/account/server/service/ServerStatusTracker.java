package com.wordonline.account.server.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.wordonline.account.server.entity.ServerState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerStatusTracker {

    private final ServerStatusService serverStatusService;

    @PostConstruct
    public void onStart() {
        serverStatusService.setServerStatus(ServerState.ACTIVE)
                .subscribe();
    }

    @PreDestroy
    public void onDestroy() {
        serverStatusService.setServerStatus(ServerState.INACTIVE)
                .block();
    }
}
