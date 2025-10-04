package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.System;

import reactor.core.publisher.Mono;

public interface SystemRepository extends R2dbcRepository<System, Long> {

    Mono<System> findByName(String name);
}
