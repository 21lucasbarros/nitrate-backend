package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String limpo = identifier == null ? "" : identifier.trim();
        // e-mail e nome de usuário sem diferenciar maiúsculas
        User user = userRepository.findFirstByEmailIgnoreCase(limpo)
                .or(() -> userRepository.findFirstByUsernameIgnoreCase(limpo))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + identifier));
        return new CustomUserDetails(user);
    }
}
