package com.wordonline.account.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.entity.Authority;
import com.wordonline.account.entity.MemberAuthority;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.entity.Principal;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;
import com.wordonline.account.repository.PrincipalRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberAuthorityRepository memberAuthorityRepository;
    private final AuthorityRepository authorityRepository;
    private final PrincipalRepository principalRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<Member> getMember(Long memberId) {
        Mono<MemberEntity> memberEntityMono = memberRepository.findById(memberId);
        return toMember(memberEntityMono);
    }

    public Mono<Member> getMember(String email) {
        Mono<MemberEntity> memberEntityMono = memberRepository.findByEmail(email);
        return toMember(memberEntityMono);
    }

    private Mono<Member> toMember(Mono<MemberEntity> memberEntityMono) {
        Flux<MemberAuthority> memberAuthorityFlux = memberEntityMono
                .flatMapMany(memberEntity -> memberAuthorityRepository.findAllByMemberId(memberEntity.getId()));

        Flux<Authority> authorityFlux = memberAuthorityFlux
                .flatMap(memberAuthority -> authorityRepository.findById(memberAuthority.getAuthorityId()));

        return Mono.zip(memberEntityMono, authorityFlux.collectList())
                .map(tuple -> {
                    MemberEntity memberEntity = tuple.getT1();
                    List<Authority> authorities = tuple.getT2();
                    return memberEntity.toDomain(authorities);
                });
    }

    public Mono<Member> createMember(JoinRequest joinRequest) {
        return principalRepository.save(new Principal())
                .flatMap(
                principal -> {
                    Member member = new Member(
                            principal.getId(),
                            joinRequest.name(),
                            joinRequest.email(),
                            passwordEncoder.encode(joinRequest.password())
                    );
                    return memberRepository.save(new MemberEntity(member));
                }
        ).map(MemberEntity::toDomain);
    }
}
