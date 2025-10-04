package com.wordonline.account.controller;

import com.wordonline.account.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import org.thymeleaf.spring6.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SystemService systemService;

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
}
