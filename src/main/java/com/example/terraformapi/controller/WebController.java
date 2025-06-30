package com.example.terraformapi.controller;

import com.example.terraformapi.model.OpenStackInstance;
import com.example.terraformapi.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final InstanceService instanceService;

    @GetMapping("/")
    public String index(Model model) {
        List<OpenStackInstance> instances = instanceService.getAllInstances();
        model.addAttribute("instances", instances);
        return "index";
    }

    @GetMapping("/create")
    public String createForm() {
        return "create";
    }
} 