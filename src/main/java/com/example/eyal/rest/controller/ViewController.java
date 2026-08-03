package com.example.eyal.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ViewController {

    private static final Logger log = LoggerFactory.getLogger(ViewController.class);

    @GetMapping({"/admin", "/admin/"})
    public String redirectToAdminDashboard() {
        log.debug("redirectToAdminDashboard called, redirecting path to /index.html");
        return "redirect:/index.html";
    }
}
