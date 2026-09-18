package com.wordonline.account.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wordonline.account.mapper.AuthorityMapper;
import com.wordonline.account.repository.AuthorityRepository;
import com.wordonline.account.repository.MemberAuthorityRepository;
import com.wordonline.account.repository.MemberRepository;
import com.wordonline.account.repository.PrincipalRepository;

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
}
