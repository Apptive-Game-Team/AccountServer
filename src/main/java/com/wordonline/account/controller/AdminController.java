package com.wordonline.account.controller;

import com.wordonline.account.repository.SystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.spring6.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SystemRepository systemRepository;

    @GetMapping
    public String adminHome() {
        return "admin/index";
    }

    @GetMapping("/systems")
    public String systemList(Model model) {
        IReactiveDataDriverContextVariable reactiveDataDrivenMode =
                new ReactiveDataDriverContextVariable(systemRepository.findAll(), 1);
        model.addAttribute("systems", reactiveDataDrivenMode);
        return "admin/systems";
    }
}
