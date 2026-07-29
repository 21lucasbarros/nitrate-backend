package com.lucasbarros.nitrate.resources;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthResource {

    @GetMapping("/health")
    public String health() {
        return "Nitrate backend está no ar!";
    }
}