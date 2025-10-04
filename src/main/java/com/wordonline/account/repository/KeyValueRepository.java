package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.KeyValue;

import reactor.core.publisher.Mono;

public interface KeyValueRepository extends R2dbcRepository<KeyValue, Long> {

    Mono<KeyValue> findByMemberIdAndSystemIdAndKey(Long memberId, Long systemId, String key);
}
