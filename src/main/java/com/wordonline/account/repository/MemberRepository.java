package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.MemberEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemberRepository extends R2dbcRepository<MemberEntity, Long> {

    Mono<MemberEntity> findByEmail(String email);

    @Query("SELECT * FROM member ORDER BY id LIMIT :size OFFSET :offset")
    Flux<MemberEntity> findPage(long offset, int size);
}
