package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoritePersonRepository extends JpaRepository<FavoritePerson, Long> {
    List<FavoritePerson> findByUserOrderByPositionAsc(User user);

    List<FavoritePerson> findByUserAndKindOrderByPositionAsc(User user, FavoritePerson.Kind kind);

    // mesmo motivo do FavoriteFilmRepository: apaga direto no banco antes de inserir a lista nova
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FavoritePerson f where f.user = :user and f.kind = :kind")
    void deleteAllByUserAndKind(@Param("user") User user, @Param("kind") FavoritePerson.Kind kind);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FavoritePerson f where f.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
