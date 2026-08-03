package com.example.eyal.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping({"/admin", "/admin/"})
    public String redirectToAdminDashboard() {
        return "redirect:/index.html";
    }
}
