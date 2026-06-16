package com.example.Bknd_User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.Bknd_User.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query(value = """
        SELECT 
            EXTRACT(MONTH FROM created_at) AS mes,
            COUNT(*) AS cantidad
        FROM usuarios
        GROUP BY EXTRACT(MONTH FROM created_at)
        ORDER BY mes
        """, nativeQuery = true)
    List<Object[]> obtenerUsuariosPorMes();
}