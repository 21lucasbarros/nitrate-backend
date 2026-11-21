package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

// o id é o do usuário (@MapsId)
public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
}
