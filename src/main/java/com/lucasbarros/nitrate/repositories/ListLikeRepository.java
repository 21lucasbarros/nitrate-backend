package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.ListLike;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListLikeRepository extends JpaRepository<ListLike, Long> {
    boolean existsByListAndUser(MovieList list, User user);
    Optional<ListLike> findByListAndUser(MovieList list, User user);

    // várias listas de uma vez. Linhas: [Long listId, Long quantidade]
    @Query("select k.list.id, count(k) from ListLike k where k.list.id in :ids group by k.list.id")
    List<Object[]> contarPorLista(@Param("ids") Collection<Long> ids);

    // quais dessas listas eu curti
    @Query("select k.list.id from ListLike k where k.user = :me and k.list.id in :ids")
    List<Long> listasMinhas(@Param("me") User me, @Param("ids") Collection<Long> ids);

    // home: ids das listas públicas mais curtidas (no empate, a curtida mais recente na frente)
    @Query("select k.list.id from ListLike k where k.list.publicList = true " +
            "group by k.list.id order by count(k) desc, max(k.createdAt) desc")
    List<Long> maisCurtidas(Pageable pageable);

    @Modifying
    @Query("delete from ListLike k where k.list = :list")
    void deleteAllByList(@Param("list") MovieList list);

    // exclusão da conta: o que a pessoa fez nas listas dos outros (as dela saem com deleteAllByList)
    @Modifying
    @Query("delete from ListLike k where k.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
