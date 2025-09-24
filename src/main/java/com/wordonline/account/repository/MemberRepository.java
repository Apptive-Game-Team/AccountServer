package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.MemberEntity;

import reactor.core.publisher.Mono;

public interface MemberRepository extends R2dbcRepository<MemberEntity, Long> {

    Mono<MemberEntity> findByEmail(String email);
}
