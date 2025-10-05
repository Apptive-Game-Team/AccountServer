package com.wordonline.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.dto.AuthorityResponse;
import com.wordonline.account.entity.Authority;
import com.wordonline.account.entity.MemberAuthority;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService {

    private final MemberRepository memberRepository;
    private final MemberAuthorityRepository memberAuthorityRepository;
    private final AuthorityRepository authorityRepository;

    public Mono<Boolean> grantAuthority(Long adminId, Long applierId, Long authorityId) {

        // TODO - 권한 부여자, 권한 체크

        Mono<MemberEntity> applier = memberRepository.findById(authorityId);

        return applier.flatMap(memberEntity -> {
            MemberAuthority memberAuthority = new MemberAuthority(applierId, authorityId);
            return memberAuthorityRepository.save(memberAuthority);
        }).hasElement();
    }

    public Mono<Boolean> revokeAuthority(Long adminId, Long applierId, Long authorityId) {

        // TODO - 권한 부여자, 권한 체크

        Mono<MemberEntity> applier = memberRepository.findById(authorityId);

        return applier.flatMap(memberEntity ->
                        memberAuthorityRepository.deleteByMemberIdAndAuthorityId(applierId, authorityId))
                .map(num -> num > 0);
    }

    public Mono<AuthorityResponse> createAuthority(String name) {
        Authority authority = new Authority(name);
        return authorityRepository.save(authority)
                .map(savedAuthority -> new AuthorityResponse(authority.getValue()));
    }

    public Mono<AuthorityResponse> updateAuthority(Long authorityId, String name) {
        return authorityRepository.findById(authorityId)
                .flatMap(authority -> {
                    authority.setValue(name);
                    return authorityRepository.save(authority);
                })
                .map(savedAuthority -> new AuthorityResponse(savedAuthority.getValue()));
    }
}
