package com.wordonline.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.ValueResponse;
import com.wordonline.account.entity.KeyValue;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.repository.KeyValueRepository;
import com.wordonline.account.repository.MemberRepository;
import com.wordonline.account.repository.SystemRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@Transactional
@RequiredArgsConstructor
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;
    private final MemberRepository memberRepository;
    private final SystemRepository systemRepository;

    public Mono<ValueResponse> getMemberValue(Long memberId, String key) {
        return memberRepository.findById(memberId)
                .flatMap(memberEntity -> getValue(memberEntity.getPrincipalId(), key))
                .map(ValueResponse::new);
    }

    public Mono<ValueResponse> getSystemValue(Long systemId, String key) {
        return systemRepository.findById(systemId)
                .flatMap(system -> getValue(system.getPrincipalId(), key))
                .map(ValueResponse::new);
    }

    public Mono<ValueResponse> setMemberValue(Long memberId, String key, String value) {
        return memberRepository.findById(memberId)
                .flatMap(memberEntity -> setValue(memberEntity.getPrincipalId(), key, value))
                .map(ValueResponse::new);
    }

    public Mono<String> getValue(Long principalId, String key) {

        // TODO - 권한 체크

        return keyValueRepository.findByPrincipalIdAndKey(principalId, key)
                .map(KeyValue::getValue);
    }

    public Mono<String> setValue(Long principalId, String key, String value) {

        // TODO - 권한 체크

        return keyValueRepository.findByPrincipalIdAndKey(principalId, key)
                .flatMap(existingKeyValue -> {
                    existingKeyValue.setValue(value);
                    return keyValueRepository.save(existingKeyValue);
                })
                .switchIfEmpty(keyValueRepository.save(new KeyValue(principalId, key, value)))
                .map(KeyValue::getValue);
    }
}
