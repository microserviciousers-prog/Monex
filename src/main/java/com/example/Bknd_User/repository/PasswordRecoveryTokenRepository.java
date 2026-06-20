package com.example.Bknd_User.repository;

import com.example.Bknd_User.entity.PasswordRecoveryToken;
import com.example.Bknd_User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByUserAndTokenAndUsedFalse(User user, String token);
    Optional<PasswordRecoveryToken> findByUserAndTokenAndUsedTrue(User user, String token);
}
