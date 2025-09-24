package com.wordonline.account.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.System;

public interface SystemRepository extends R2dbcRepository<System, Long> {

}
