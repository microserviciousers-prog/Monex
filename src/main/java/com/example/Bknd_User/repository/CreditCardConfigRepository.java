package com.example.Bknd_User.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Bknd_User.entity.CreditCardConfig;
import com.example.Bknd_User.entity.User;

import java.util.Optional;

@Repository
public interface CreditCardConfigRepository extends JpaRepository<CreditCardConfig, Long> {
    Optional<CreditCardConfig> findByUser(User user);
    Optional<CreditCardConfig> findByUserId(Long userId);
}
