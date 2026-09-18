package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wordonline.account.entity.AuthorityEntity;
import com.wordonline.account.entity.MemberAuthority;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.mapper.AuthorityMapper;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberAuthorityRepository memberAuthorityRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private AuthorityMapper authorityMapper;

    @Mock
    private AuthorizationService authorizationService;

    private AuthorityService authorityService;

    @BeforeEach
    void setUp() {
        authorityService = new AuthorityService(memberRepository, memberAuthorityRepository,
                authorityRepository, authorityMapper, authorizationService);
    }

    @Test
    void grantAuthorityFailsWhenAuthorityDoesNotExist() {
        when(memberRepository.findById(7L))
                .thenReturn(Mono.just(new MemberEntity(7L, 1L, "name", "a@b.c", "hash", false)));
        when(authorityRepository.findById(120L)).thenReturn(Mono.empty());

        StepVerifier.create(authorityService.grantAuthority(null, 7L, 120L))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void grantAuthoritySavesMemberIdAndAuthorityId() {
        when(memberRepository.findById(7L))
                .thenReturn(Mono.just(new MemberEntity(7L, 1L, "name", "a@b.c", "hash", false)));
        when(authorityRepository.findById(120L))
                .thenReturn(Mono.just(new AuthorityEntity(120L, 1L, "READ")));
        when(memberAuthorityRepository.save(any(MemberAuthority.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(authorityService.grantAuthority(null, 7L, 120L))
                .expectNext(true)
                .verifyComplete();

        ArgumentCaptor<MemberAuthority> saved = ArgumentCaptor.forClass(MemberAuthority.class);
        verify(memberAuthorityRepository).save(saved.capture());
        assertEquals(7L, saved.getValue().getMemberId());
        assertEquals(120L, saved.getValue().getAuthorityId());
    }

    @Test
    void revokeAuthorityFailsWhenMemberDoesNotExist() {
        when(memberRepository.findById(999L)).thenReturn(Mono.empty());
        when(authorityRepository.findById(1L))
                .thenReturn(Mono.just(new AuthorityEntity(1L, 1L, "READ")));

        StepVerifier.create(authorityService.revokeAuthority(null, 999L, 1L))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
