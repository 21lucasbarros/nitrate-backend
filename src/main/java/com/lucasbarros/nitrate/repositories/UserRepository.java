package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    // e-mail sem diferenciar maiúsculas: "Ana@x.com" e "ana@x.com" são a mesma conta
    Optional<User> findFirstByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    // busca por nome de usuário ou nome; quem começa com o termo vem antes.
    // `termo` chega com \, % e _ escapados (ver SocialService.pesquisarUsuarios)
    @Query("select u from User u where lower(u.username) like concat('%', :termo, '%') escape '\\' " +
            "or lower(u.name) like concat('%', :termo, '%') escape '\\' " +
            "order by case when lower(u.username) like concat(:termo, '%') escape '\\' then 0 " +
            "when lower(u.name) like concat(:termo, '%') escape '\\' then 1 else 2 end, u.username")
    List<User> pesquisar(@Param("termo") String termo, Pageable pageable);
}
