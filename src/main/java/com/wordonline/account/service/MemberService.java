package com.wordonline.account.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.account.domain.Authority;
import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.entity.AuthorityEntity;
import com.wordonline.account.entity.MemberAuthority;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.entity.Principal;
import com.wordonline.account.mapper.AuthorityMapper;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;
import com.wordonline.account.repository.PrincipalRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberAuthorityRepository memberAuthorityRepository;
    private final AuthorityRepository authorityRepository;
    private final PrincipalRepository principalRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityMapper authorityMapper;

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
                .flatMapMany(memberEntity -> memberAuthorityRepository.findAllByMemberId(
                        memberEntity.getId()));

        Flux<AuthorityEntity> authorityFlux = memberAuthorityFlux
                .flatMap(memberAuthority -> authorityRepository.findById(
                        memberAuthority.getAuthorityId()));

        return Mono.zip(memberEntityMono, authorityFlux.collectList())
                .flatMap(tuple -> {
                    MemberEntity memberEntity = tuple.getT1();
                    List<AuthorityEntity> authorityEntities = tuple.getT2();

                    Flux<Authority> authorityFluxMapped = Flux.fromIterable(authorityEntities)
                            .flatMap(authorityMapper::toDomain);

                    return authorityFluxMapped.collectList()
                            .map(memberEntity::toDomain);
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

    public Flux<Member> getSimpleMembers(long offset, int size) {
        return memberRepository.findPage(offset, size)
                .map(MemberEntity::toDomain);
    }

    public Mono<Long> getMemberCount() {
        return memberRepository.count();
    }

    public Mono<Void> deleteMember(long memberId) {
        return memberRepository.deleteById(memberId);
    }
}
