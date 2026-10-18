package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wordonline.account.dto.MemberPutRequest;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.mapper.AuthorityMapper;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;
import com.wordonline.account.repository.PrincipalRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final long MEMBER_ID = 7L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberAuthorityRepository memberAuthorityRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private PrincipalRepository principalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityMapper authorityMapper;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, memberAuthorityRepository,
                authorityRepository, principalRepository, passwordEncoder, authorityMapper);
    }

    /**
     * V003 puts ON DELETE CASCADE on refresh_token_member_id_fkey specifically so this call can
     * still succeed for a member who has a refresh_token row. deleteMember() only issues the
     * delete; the cascade itself is a database constraint and is verified against a real
     * PostgreSQL instance, not through this mock.
     */
    @Test
    void deleteMemberSucceedsForAMemberThatHasARefreshTokenRow() {
        when(memberRepository.deleteById(MEMBER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(memberService.deleteMember(MEMBER_ID)).verifyComplete();

        verify(memberRepository).deleteById(eq(MEMBER_ID));
    }

    /**
     * A converting guest sends no lastPassword: the generated one lives only in the client's
     * memory and is gone after a restart. The check is skipped rather than merely tolerant of a
     * null, so it never reaches PasswordEncoder.matches(), which answers a null raw password by
     * throwing.
     */
    @Test
    void aGuestConvertsWithoutPresentingALastPassword() {
        givenMemberRow(true);
        when(passwordEncoder.encode("chosen-password")).thenReturn("chosen-hash");
        ArgumentCaptor<MemberEntity> saved = ArgumentCaptor.forClass(MemberEntity.class);
        when(memberRepository.save(saved.capture())).thenAnswer(
                invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(memberService.putMember(MEMBER_ID, new MemberPutRequest(
                        "chosen@example.com", "chosen name", null, "chosen-password")))
                .verifyComplete();

        verify(passwordEncoder, never()).matches(any(), any());
        assertEquals("chosen@example.com", saved.getValue().getEmail());
        assertFalse(saved.getValue().isGuest(), "a converted account is no longer a guest");
    }

    @Test
    void aRealMemberIsStillRejectedWithoutTheRightLastPassword() {
        givenMemberRow(false);
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        StepVerifier.create(memberService.putMember(MEMBER_ID, new MemberPutRequest(
                        "new@example.com", "new name", "wrong-password", "new-password")))
                .expectErrorSatisfies(error -> assertInstanceOf(
                        AuthorizationDeniedException.class, error))
                .verify();

        verify(memberRepository, never()).save(any());
    }

    @Test
    void aRealMemberUpdatesWithTheRightLastPassword() {
        givenMemberRow(false);
        when(passwordEncoder.matches("right-password", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        ArgumentCaptor<MemberEntity> saved = ArgumentCaptor.forClass(MemberEntity.class);
        when(memberRepository.save(saved.capture())).thenAnswer(
                invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(memberService.putMember(MEMBER_ID, new MemberPutRequest(
                        "new@example.com", "new name", "right-password", "new-password")))
                .verifyComplete();

        assertEquals("new-hash", saved.getValue().getPasswordHash());
        assertFalse(saved.getValue().isGuest());
    }

    @Test
    void searchMembers_blankOrNullQuery_returnsEmpty() {
        StepVerifier.create(memberService.searchMembers(null, 20))
                .verifyComplete();

        StepVerifier.create(memberService.searchMembers("   ", 20))
                .verifyComplete();

        verify(memberRepository, never()).searchMembers(any(), any(int.class));
    }

    @Test
    void searchMembers_validQuery_delegatesToRepositoryWithClampedLimit() {
        MemberEntity entity1 = new MemberEntity(1L, 101L, "Alice", "alice@example.com", "hash1", false);
        MemberEntity entity2 = new MemberEntity(2L, 102L, "Alicia", "alicia@example.com", "hash2", false);
        when(memberRepository.searchMembers("ali", 50)).thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(memberService.searchMembers("ali", 100))
                .expectNextMatches(m -> m.getId().equals(1L) && m.getName().equals("Alice"))
                .expectNextMatches(m -> m.getId().equals(2L) && m.getName().equals("Alicia"))
                .verifyComplete();

        verify(memberRepository).searchMembers("ali", 50);
    }

    @Test
    void getMember_loadsAuthoritiesInBatchWithFindAllById() {
        MemberEntity memberEntity = new MemberEntity(MEMBER_ID, 100L, "User", "user@example.com", "hash", false);
        com.wordonline.account.entity.MemberAuthority ma1 = new com.wordonline.account.entity.MemberAuthority(MEMBER_ID, 10L);
        com.wordonline.account.entity.MemberAuthority ma2 = new com.wordonline.account.entity.MemberAuthority(MEMBER_ID, 20L);
        com.wordonline.account.entity.AuthorityEntity auth1 = new com.wordonline.account.entity.AuthorityEntity(10L, 1L, "ROLE_ADMIN");
        com.wordonline.account.entity.AuthorityEntity auth2 = new com.wordonline.account.entity.AuthorityEntity(20L, 1L, "ROLE_USER");
        com.wordonline.account.domain.Authority domainAuth1 = new com.wordonline.account.domain.Authority(10L, null, "ROLE_ADMIN");
        com.wordonline.account.domain.Authority domainAuth2 = new com.wordonline.account.domain.Authority(20L, null, "ROLE_USER");

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Mono.just(memberEntity));
        when(memberAuthorityRepository.findAllByMemberId(MEMBER_ID)).thenReturn(Flux.just(ma1, ma2));
        when(authorityRepository.findAllById(java.util.List.of(10L, 20L))).thenReturn(Flux.just(auth1, auth2));
        when(authorityMapper.toDomain(auth1)).thenReturn(Mono.just(domainAuth1));
        when(authorityMapper.toDomain(auth2)).thenReturn(Mono.just(domainAuth2));

        StepVerifier.create(memberService.getMember(MEMBER_ID))
                .expectNextMatches(member -> member.getId().equals(MEMBER_ID) && member.getAuthorityList().size() == 2)
                .verifyComplete();

        verify(authorityRepository).findAllById(java.util.List.of(10L, 20L));
        verify(authorityRepository, never()).findById(any(Long.class));
    }

    private void givenMemberRow(boolean guest) {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Mono.just(
                new MemberEntity(MEMBER_ID, 1L, "tester", "tester@example.com", "stored-hash",
                        guest)));
        when(memberAuthorityRepository.findAllByMemberId(MEMBER_ID)).thenReturn(Flux.empty());
    }
}
