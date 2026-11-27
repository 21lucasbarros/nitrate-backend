package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.ForgotPasswordRequestDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.dto.ResetPasswordRequestDTO;
import com.lucasbarros.nitrate.dto.UsernameAvailabilityDTO;
import com.lucasbarros.nitrate.security.ClientIp;
import com.lucasbarros.nitrate.services.AuthService;
import com.lucasbarros.nitrate.services.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/register")
    public AuthResponseDTO registrar(@RequestBody RegisterRequestDTO request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request, HttpServletRequest http) {
        return authService.login(request, ClientIp.de(http));
    }

    /** encerra o token desta requisição; sempre 204, mesmo sem token ou com um já vencido */
    @PostMapping("/logout")
    public ResponseEntity<Void> sair(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.sair(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-username")
    public UsernameAvailabilityDTO checkUsername(@RequestParam String username) {
        return new UsernameAvailabilityDTO(authService.usernameDisponivel(username));
    }

    /** sempre 204, com ou sem conta para o e-mail (não revela quem está cadastrado) */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> esqueciSenha(@RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.solicitar(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> redefinirSenha(@RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.redefinir(request.getToken(), request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
