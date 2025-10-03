package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.KeyValue;

import reactor.core.publisher.Mono;

public interface KeyValueRepository extends R2dbcRepository<KeyValue, Long> {

    Mono<KeyValue> findByPrincipalIdAndKey(Long principalId, String key);
}
