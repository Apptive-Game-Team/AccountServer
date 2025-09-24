package com.wordonline.account.repository;

import java.util.ServiceLoader.Provider;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.MemberAuthority;

import reactor.core.publisher.Flux;

public interface MemberAuthorityRepository extends R2dbcRepository<MemberAuthority, Long> {

    Flux<MemberAuthority> findAllByMemberId(Long memberId);
}
