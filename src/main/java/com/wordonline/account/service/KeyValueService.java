package com.wordonline.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.entity.KeyValue;
import com.wordonline.account.repository.KeyValueRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@Transactional
@RequiredArgsConstructor
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;

    public Mono<String> getValue(Long principalId, String key) {

        // TODO - 권한 체크

        return keyValueRepository.findByPrincipalIdAndKey(principalId, key)
                .map(KeyValue::getValue);
    }

    public Mono<Boolean> setValue(Long principalId, String key, String value) {

        // TODO - 권한 체크

        return keyValueRepository.findByPrincipalIdAndKey(principalId, key)
                .flatMap(existingKeyValue -> {
                    existingKeyValue.setValue(value);
                    return keyValueRepository.save(existingKeyValue);
                })
                .switchIfEmpty(
                        keyValueRepository.save(new KeyValue(principalId, key, value))
                )
                .map(saved -> true);
    }
}
