package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {
}
