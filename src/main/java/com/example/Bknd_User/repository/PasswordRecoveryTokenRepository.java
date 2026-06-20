package com.example.Bknd_User.repository;

import com.example.Bknd_User.entity.PasswordRecoveryToken;
import com.example.Bknd_User.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByUserAndTokenAndUsedFalse(User user, String token);

    // Borra tokens por user id con consulta JPQL transaccional segura
    @Modifying
    @Query("delete from PasswordRecoveryToken t where t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
