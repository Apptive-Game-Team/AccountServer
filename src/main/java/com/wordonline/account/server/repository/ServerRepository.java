package com.wordonline.account.server.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import com.wordonline.account.server.entity.Server;

import reactor.core.publisher.Mono;

@Repository
public interface ServerRepository extends R2dbcRepository<Server, Long> {

    Mono<Server> findByDomainAndPort(String domain, Integer port);
}
