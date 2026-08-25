package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.services.exceptions.EmailJaCadastradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private AuthenticationManager authenticationManager;

    public AuthResponseDTO registrar(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException(request.getEmail());
        }

        String senhaCriptografada = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), request.getEmail(), senhaCriptografada);
        user = userRepository.save(user);

        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }
}
