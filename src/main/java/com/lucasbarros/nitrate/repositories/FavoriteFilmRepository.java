package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.FavoriteFilm;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteFilmRepository extends JpaRepository<FavoriteFilm, Long> {
    List<FavoriteFilm> findByUserOrderByPositionAsc(User user);

    // apaga direto no banco antes de inserir a lista nova (o deleteBy derivado apagaria um a um,
    // depois dos inserts, e esbarraria na restrição única)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FavoriteFilm f where f.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
