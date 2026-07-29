package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FranchiseRepository extends JpaRepository<Franchise, Long> {
    Optional<Franchise> findByName(String name);
}
