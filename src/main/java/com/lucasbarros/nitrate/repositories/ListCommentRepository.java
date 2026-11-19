package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.ListComment;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ListCommentRepository extends JpaRepository<ListComment, Long> {
    List<ListComment> findByListOrderByCreatedAtAscIdAsc(MovieList list);

    // várias listas de uma vez. Linhas: [Long listId, Long quantidade]
    @Query("select c.list.id, count(c) from ListComment c where c.list.id in :ids group by c.list.id")
    List<Object[]> contarPorLista(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from ListComment c where c.list = :list")
    void deleteAllByList(@Param("list") MovieList list);

    // exclusão da conta: o que a pessoa fez nas listas dos outros (as dela saem com deleteAllByList)
    @Modifying
    @Query("delete from ListComment c where c.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
