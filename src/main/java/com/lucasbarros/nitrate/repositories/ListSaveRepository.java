package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.ListSave;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListSaveRepository extends JpaRepository<ListSave, Long> {
    boolean existsByListAndUser(MovieList list, User user);
    Optional<ListSave> findByListAndUser(MovieList list, User user);
    List<ListSave> findByUserOrderByCreatedAtDescIdDesc(User user);

    // quais dessas listas eu salvei
    @Query("select s.list.id from ListSave s where s.user = :me and s.list.id in :ids")
    List<Long> listasMinhas(@Param("me") User me, @Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from ListSave s where s.list = :list")
    void deleteAllByList(@Param("list") MovieList list);

    // exclusão da conta: o que a pessoa fez nas listas dos outros (as dela saem com deleteAllByList)
    @Modifying
    @Query("delete from ListSave s where s.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
