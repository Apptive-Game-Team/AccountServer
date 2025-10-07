package com.wordonline.account.service;

import org.springframework.stereotype.Service;

import com.wordonline.account.repository.SystemRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service("auth")
@RequiredArgsConstructor
public class AuthorizationService {

    private final SystemRepository systemRepository;

    public Mono<Boolean> checkAuthority(String authority, Mono<Long> systemIdMono, String role) {
        return systemIdMono.flatMap(systemId -> checkAuthority(authority, systemId, role));
    }

    public Mono<Boolean> checkAuthority(String authority, Long systemId, String role) {
        return Mono.zip(
                isSystem(authority, systemId),
                isRole(authority, role)
                ).map(zip -> zip.getT1() && zip.getT2());
    }

    public Mono<Boolean> isRole(String authority, String role) {
        return Mono.just(
                authority.split("_")[1]
                        .equals(role.toUpperCase())
        );
    }

    public Mono<Boolean> isSystem(String authority, Long systemId) {
        String systemString = authority.split("_")[0];
        return systemRepository.findById(systemId)
                .map(system -> system.getNormalizedName().equals(systemString));
    }
}
