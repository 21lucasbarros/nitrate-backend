package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * A conta de sistema "nitrate", dona das listas oficiais (as que o admin cria "como o Nitrate").
 * Criada na primeira vez que o admin mexe nas listas; ninguém entra nela (senha aleatória que não
 * é guardada em lugar nenhum), e o @ é reservado no cadastro (AuthService).
 */
@Service
public class NitrateAccountService {

    public static final String USERNAME = "nitrate";
    public static final String NOME = "Nitrate";
    public static final String EMAIL = "listas@nitrate.com.br";

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** a conta, se já existe (as rotas públicas não a criam) */
    @Transactional(readOnly = true)
    public Optional<User> existente() {
        return userRepository.findFirstByUsernameIgnoreCase(USERNAME).filter(NitrateAccountService::ehConta);
    }

    /** a conta, criada agora se ainda não existe */
    @Transactional
    public User conta() {
        Optional<User> comOArroba = userRepository.findFirstByUsernameIgnoreCase(USERNAME);
        if (comOArroba.isPresent()) {
            // alguém já tinha o @ antes de ele ser reservado: não usa a conta da pessoa como a do Nitrate
            if (!ehConta(comOArroba.get())) {
                throw new IllegalArgumentException("O @" + USERNAME + " pertence a outra conta; renomeie-a antes de criar listas oficiais.");
            }
            return comOArroba.get();
        }
        User user = new User(NOME, USERNAME, EMAIL, passwordEncoder.encode(senhaAleatoria()));
        user.setTermsAcceptedAt(Instant.now());
        return userRepository.save(user);
    }

    /** a lista é oficial: o @ é reservado, mas confere o e-mail também, por garantia */
    public static boolean ehConta(User user) {
        return user != null
                && USERNAME.equalsIgnoreCase(user.getUsername())
                && EMAIL.equalsIgnoreCase(user.getEmail());
    }

    private static String senhaAleatoria() {
        // 48 bytes viram 64 caracteres: abaixo do limite de 72 bytes do bcrypt
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
