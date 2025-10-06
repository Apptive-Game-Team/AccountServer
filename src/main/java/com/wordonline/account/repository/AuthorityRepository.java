package com.wordonline.account.repository;

import com.wordonline.account.entity.AuthorityEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import reactor.core.publisher.Flux;

public interface AuthorityRepository extends R2dbcRepository<AuthorityEntity, Long> {

    @Query("SELECT * FROM authority ORDER BY id LIMIT :size OFFSET :offset")
    Flux<AuthorityEntity> findPage(long offset, int size);
}
