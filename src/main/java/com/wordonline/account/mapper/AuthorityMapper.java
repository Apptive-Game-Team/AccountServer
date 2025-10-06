package com.wordonline.account.mapper;

import org.springframework.stereotype.Component;

import com.wordonline.account.domain.Authority;
import com.wordonline.account.entity.AuthorityEntity;
import com.wordonline.account.repository.SystemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorityMapper {

    private final SystemRepository systemRepository;

    public Mono<Authority> toDomain(AuthorityEntity authorityEntity) {
        log.info("[MAP] to authority domain");
        return systemRepository.findById(authorityEntity.getSystemId())
                .map(system -> new Authority(authorityEntity, system));
    }

    public Authority toDomainBlock(AuthorityEntity authorityEntity) {
        log.info("[MAP] to authority domain");
        return systemRepository.findById(authorityEntity.getSystemId())
                .map(system -> new Authority(authorityEntity, system))
                .block();
    }
}
