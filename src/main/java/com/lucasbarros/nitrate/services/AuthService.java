package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.services.exceptions.EmailJaCadastradoException;
import com.lucasbarros.nitrate.services.exceptions.UsernameJaCadastradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private AuthenticationManager authenticationManager;

    public AuthResponseDTO registrar(RegisterRequestDTO request) {
        String username = request.getUsername().toLowerCase().trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Nome de usuário deve ter entre 3 e 20 caracteres, apenas letras minúsculas, números e _");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException(request.getEmail());
        }

        if (userRepository.existsByUsername(username)) {
            throw new UsernameJaCadastradoException(username);
        }

        String senhaCriptografada = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), username, request.getEmail(), senhaCriptografada);
        user = userRepository.save(user);

        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        User user = userRepository.findByEmail(request.getLogin())
                .or(() -> userRepository.findByUsername(request.getLogin()))
                .orElseThrow();

        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getEmail());
    }

    public boolean usernameDisponivel(String username) {
        return !userRepository.existsByUsername(username.toLowerCase().trim());
    }
}