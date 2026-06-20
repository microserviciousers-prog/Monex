package com.example.Bknd_User.repository;

import com.example.Bknd_User.entity.PasswordRecoveryToken;
import com.example.Bknd_User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByUserAndTokenAndUsedFalse(User user, String token);

    // Borra todos los tokens asociados a un usuario (usado antes de eliminar el usuario)
    void deleteByUser(User user);
    
    // Borra tokens por user id (método más directo y seguro desde el servicio)
    void deleteByUserId(Long userId);
}
