package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.Authority;

public interface AuthorityRepository extends R2dbcRepository<Authority, Long> {

}
