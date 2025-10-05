package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.MemberAuthority;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemberAuthorityRepository extends R2dbcRepository<MemberAuthority, Long> {

    Flux<MemberAuthority> findAllByMemberId(Long memberId);
    Mono<Long> deleteByMemberIdAndAuthorityId(Long memberId, Long authorityId);
}
