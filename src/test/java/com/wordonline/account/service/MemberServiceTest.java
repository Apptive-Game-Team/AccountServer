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

    private void givenMemberRow(boolean guest) {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Mono.just(
                new MemberEntity(MEMBER_ID, 1L, "tester", "tester@example.com", "stored-hash",
                        guest)));
        when(memberAuthorityRepository.findAllByMemberId(MEMBER_ID)).thenReturn(Flux.empty());
    }
}
