package com.wordonline.account.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.domain.Authority;
import com.wordonline.account.dto.AuthorityResponse;
import com.wordonline.account.entity.AuthorityEntity;
import com.wordonline.account.entity.MemberAuthority;
import com.wordonline.account.mapper.AuthorityMapper;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorityService {

    private final MemberRepository memberRepository;
    private final MemberAuthorityRepository memberAuthorityRepository;
    private final AuthorityRepository authorityRepository;
    private final AuthorityMapper authorityMapper;
    private final AuthorizationService authorizationService;

    public Mono<Boolean> grantAuthority(Long adminId, Long applierId, Long authorityId) {

        // TODO - 권한 부여자, 권한 체크

        return requireMemberAndAuthority(applierId, authorityId)
                .flatMap(found -> memberAuthorityRepository
                        .save(new MemberAuthority(applierId, authorityId)))
                .hasElement();
    }

    public Mono<Boolean> revokeAuthority(Long adminId, Long applierId, Long authorityId) {

        // TODO - 권한 부여자, 권한 체크

        return requireMemberAndAuthority(applierId, authorityId)
                .flatMap(found -> memberAuthorityRepository
                        .deleteByMemberIdAndAuthorityId(applierId, authorityId))
                .map(num -> num > 0);
    }

    private Mono<?> requireMemberAndAuthority(Long memberId, Long authorityId) {
        return memberRepository.findById(memberId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Member not found: " + memberId)))
                .zipWith(authorityRepository.findById(authorityId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "Authority not found: " + authorityId))));
    }

    public Mono<AuthorityResponse> createAuthority(Long systemId, String name) {

        // TODO - 권한 부여자, 권한 체크

        AuthorityEntity authorityEntity = new AuthorityEntity(systemId, name);
        return authorityRepository.save(authorityEntity)
                .flatMap(authorityMapper::toDomain)
                .map(AuthorityResponse::new);
    }

    public Mono<AuthorityResponse> updateAuthority(Long authorityId, String name) {

        // TODO - 권한 부여자, 권한 체크

        return authorityRepository.findById(authorityId)
                .flatMap(authority -> {
                    authority.setValue(name);
                    return authorityRepository.save(authority);
                })
                .flatMap(authorityMapper::toDomain)
                .map(AuthorityResponse::new);
    }

    public Flux<AuthorityResponse> getAuthorities(long offset, int size) {

        // TODO - 권한 부여자, 권한 체크

        return authorityRepository.findPage(offset, size)
                .flatMap(authorityMapper::toDomain)
                .map(AuthorityResponse::new)
                .doOnError(e -> log.error("[ERROR]", e));
    }

    public Flux<Authority> getAuthoritiesByIds(java.util.List<Long> authorityIds) {
        if (authorityIds == null || authorityIds.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(authorityIds)
                .flatMap(authorityRepository::findById)
                .flatMap(authorityMapper::toDomain);
    }
}
