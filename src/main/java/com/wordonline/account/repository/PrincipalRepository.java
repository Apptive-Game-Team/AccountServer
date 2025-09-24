package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.Principal;

public interface PrincipalRepository extends R2dbcRepository<Principal, Long> {

}
