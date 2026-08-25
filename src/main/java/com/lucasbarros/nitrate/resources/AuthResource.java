package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public AuthResponseDTO registrar(@RequestBody RegisterRequestDTO request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}