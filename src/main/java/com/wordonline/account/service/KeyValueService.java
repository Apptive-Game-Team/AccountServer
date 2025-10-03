package com.wordonline.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.dto.ValueResponse;
import com.wordonline.account.entity.KeyValue;
import com.wordonline.account.repository.KeyValueRepository;
import com.wordonline.account.repository.SystemRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@Transactional
@RequiredArgsConstructor
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;
    private final SystemRepository systemRepository;

    public Mono<ValueResponse> getValue(Long memberId, String systemName, String key) {
        return systemRepository.findByName(systemName)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("System not found: " + systemName)))
                .flatMap(system -> getValue(memberId, system.getId(), key))
                .map(ValueResponse::new);
    }

    public Mono<ValueResponse> setValue(Long memberId, String systemName, String key, String value) {
        return systemRepository.findByName(systemName)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("System not found: " + systemName)))
                .flatMap(system -> setValue(memberId, system.getId(), key, value))
                .map(ValueResponse::new);
    }

    private Mono<String> getValue(Long memberId, Long systemId, String key) {

        // TODO - 권한 체크

        return keyValueRepository.findByMemberIdAndSystemIdAndKey(memberId, systemId, key)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        String.format(
                                "Key-Value not found memberId: %d, systemId: %d, key: %s",
                                memberId, systemId, key))
                        )
                )
                .map(KeyValue::getValue);
    }

    private Mono<String> setValue(Long memberId, Long systemId, String key, String value) {

        // TODO - 권한 체크

        return keyValueRepository.findByMemberIdAndSystemIdAndKey(memberId, systemId, key)
                .flatMap(existingKeyValue -> {
                    existingKeyValue.setValue(value);
                    return keyValueRepository.save(existingKeyValue);
                })
                .switchIfEmpty(keyValueRepository.save(new KeyValue(memberId, systemId, key, value)))
                .map(KeyValue::getValue);
    }
}
