package com.wordonline.account.controller;

import com.wordonline.account.domain.Member;
import com.wordonline.account.dto.AuthorityResponse;
import com.wordonline.account.entity.Authority;
import com.wordonline.account.service.AuthorizationService;
import com.wordonline.account.service.MemberService;
import com.wordonline.account.service.SystemService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.thymeleaf.spring6.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SystemService systemService;
    private final MemberService memberService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public String adminHome() {
        return "admin/index";
    }

    @GetMapping("/systems")
    public String systemList(Model model) {
        IReactiveDataDriverContextVariable reactiveDataDrivenMode =
                new ReactiveDataDriverContextVariable(systemService.getSystems(), 1);
        model.addAttribute("systems", reactiveDataDrivenMode);
        return "admin/systems";
    }

    @PostMapping("/systems")
    public String createSystem(
            ServerWebExchange exchange,
            @AuthenticationPrincipal Jwt principal
    ) {
        exchange.getFormData()
                .flatMap(formdata -> {
                    String name = formdata.getFirst("name");
                    return systemService.createSystem(principal.getClaim("memberId"), name);
                }).subscribe();
        return "redirect:/admin/systems";
    }

    @PostMapping("/systems/{id}")
    public String updateSystem(
            @PathVariable Long id,
            ServerWebExchange exchange) {
        exchange.getFormData()
                .flatMap(formdata -> {
                    String name = formdata.getFirst("name");
                    return systemService.updateSystem(id, name);
                }).subscribe();
        return "redirect:/admin/systems";
    }

    @GetMapping("/members")
    public Mono<String> memberList(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var members = memberService.getSimpleMembers(page * size, size);

        return memberService.getMemberCount()
                .map(count -> {
                    var membersIReactive = new ReactiveDataDriverContextVariable(members, size);
                    model.addAttribute("members", membersIReactive);
                    model.addAttribute("memberCount", count);
                    model.addAttribute("currentPage", page);
                    model.addAttribute("size", size);
                    return "admin/members";
                });
    }

    @GetMapping("/authorities")
    public String authorityList(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var authorities = authorizationService.getAuthorities(page * size, size);
        IReactiveDataDriverContextVariable reactiveDataDrivenMode =
                new ReactiveDataDriverContextVariable(authorities, 1);
        model.addAttribute("authorities", reactiveDataDrivenMode);
        // TODO: Add pagination support
        return "admin/authorities";
    }

    @PostMapping("/authorities")
    public String createAuthority(ServerWebExchange exchange) {
        exchange.getFormData()
                .flatMap(formdata -> {
                    String name = formdata.getFirst("name");
                    return authorizationService.createAuthority(name);
                }).subscribe();
        return "redirect:/admin/authorities";
    }

    @PostMapping("/authorities/{id}")
    public String updateAuthority(
            @PathVariable Long id,
            ServerWebExchange exchange) {
        exchange.getFormData()
                .flatMap(formdata -> {
                    String name = formdata.getFirst("name");
                    return authorizationService.updateAuthority(id, name);
                }).subscribe();
        return "redirect:/admin/authorities";
    }

    @GetMapping("/members/{id}")
    public Mono<String> memberDetails(@PathVariable Long id, Model model) {
        Mono<Member> memberMono = memberService.getMember(id);
        Flux<AuthorityResponse> allAuthoritiesFlux = authorizationService.getAuthorities(0, 1000); // Assuming max 1000 authorities

        return memberMono.zipWith(allAuthoritiesFlux.collectList())
                .map(tuple -> {
                    Member member = tuple.getT1();
                    List<AuthorityResponse> allAuthorities = tuple.getT2();

                    Set<Long> assignedAuthorityIds = member.getAuthorityList().stream()
                            .map(Authority::getId)
                            .collect(Collectors.toSet());

                    List<AuthorityResponse> availableAuthorities = allAuthorities.stream()
                            .filter(auth -> !assignedAuthorityIds.contains(auth.id()))
                            .collect(Collectors.toList());

                    model.addAttribute("member", member);
                    model.addAttribute("availableAuthorities", availableAuthorities);
                    return "admin/member";
                });
    }

    @PostMapping("/members/{memberId}/authorities")
    public String grantAuthorityToMember(@PathVariable Long memberId, ServerWebExchange exchange) {
        exchange.getFormData()
                .flatMap(formdata -> {
                    String authorityIdStr = formdata.getFirst("authorityId");
                    if (authorityIdStr == null) {
                        return Mono.error(new IllegalArgumentException("Authority ID is missing"));
                    }
                    Long authorityId = Long.parseLong(authorityIdStr);
                    // Assuming adminId is not strictly needed for now, passing null
                    return authorizationService.grantAuthority(null, memberId, authorityId);
                }).subscribe();
        return "redirect:/admin/members/" + memberId;
    }

    @PostMapping("/members/{memberId}/authorities/{authorityId}/delete")
    public String revokeAuthorityFromMember(@PathVariable Long memberId, @PathVariable Long authorityId) {
        // Assuming adminId is not strictly needed for now, passing null
        authorizationService.revokeAuthority(null, memberId, authorityId).subscribe();
        return "redirect:/admin/members/" + memberId;
    }
}
