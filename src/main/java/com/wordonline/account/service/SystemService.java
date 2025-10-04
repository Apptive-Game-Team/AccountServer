package com.wordonline.account.service;

import org.springframework.stereotype.Service;

import com.wordonline.account.entity.Principal;
import com.wordonline.account.entity.System;
import com.wordonline.account.repository.PrincipalRepository;
import com.wordonline.account.repository.SystemRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemRepository systemRepository;
    private final PrincipalRepository principalRepository;

    public Mono<System> createSystem(Long memberId, String name) {
        Principal principal = new Principal();
        return principalRepository.save(principal)
                .flatMap(savedPrincipal -> {
                    System system = new System(savedPrincipal.getId(), name, memberId);
                    return systemRepository.save(system);
                });
    }

    public Mono<System> updateSystem(Long systemId, String name) {
        return systemRepository.findById(systemId)
                .flatMap(system -> {
                    system.setName(name);
                    return systemRepository.save(system);
                });
    }

    public Flux<System> getSystems() {
        return systemRepository.findAll();
    }
}
