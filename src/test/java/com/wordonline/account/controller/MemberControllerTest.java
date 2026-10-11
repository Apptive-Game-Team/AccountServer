package com.wordonline.account.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.wordonline.account.domain.Member;
import com.wordonline.account.service.MemberService;

import reactor.core.publisher.Mono;

/**
 * The member lookup the lobby calls; its public ranking leaves guests out by the guest field.
 */
class MemberControllerTest {

    private MemberService memberService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        client = WebTestClient.bindToController(new MemberController(memberService)).build();
    }

    private static Member member(long id, String email, boolean guest) {
        return new Member(id, 1L, "name" + id, email, "hash", guest, List.of());
    }

    @Test
    void getMemberReportsAGuest() {
        when(memberService.getMember(1L))
                .thenReturn(Mono.just(member(1L, "guest_x@example.com", true)));

        client.get().uri("/api/members/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.guest").isEqualTo(true);
    }

    @Test
    void getMemberReportsANonGuest() {
        when(memberService.getMember(2L))
                .thenReturn(Mono.just(member(2L, "player@example.com", false)));

        client.get().uri("/api/members/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.guest").isEqualTo(false);
    }

    @Test
    void guestFlagComesFromTheColumnNotTheEmailShape() {
        when(memberService.getMember(3L))
                .thenReturn(Mono.just(member(3L, "guest_looks@example.com", false)));

        client.get().uri("/api/members/3")
                .exchange()
                .expectBody()
                .jsonPath("$.guest").isEqualTo(false);
    }
}
