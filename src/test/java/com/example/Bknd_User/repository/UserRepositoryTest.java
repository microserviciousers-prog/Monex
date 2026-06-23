package com.example.Bknd_User.repository;

import com.example.Bknd_User.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_repository",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=clave-test-1234567890-clave-test-1234567890",
    "jwt.expiration=86400000",
    "resend.api.key=test-api-key",
    "resend.from.email=test@monex.cl"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User crearUsuario(String usernameBase) {
        long timestamp = System.currentTimeMillis();

        return User.builder()
                .username(usernameBase + timestamp)
                .email(usernameBase + timestamp + "@demo.cl")
                .password("12345678")
                .googleLinked(false)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .role("USER")
                .build();
    }

    @Test
    @DisplayName("findByEmail: debe encontrar usuario por email")
    void findByEmail_ok() {
        User user = crearUsuario("manuel");

        userRepository.save(user);

        Optional<User> result = userRepository.findByEmail(user.getEmail());

        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().getUsername());
        assertEquals(user.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("findByUsername: debe encontrar usuario por username")
    void findByUsername_ok() {
        User user = crearUsuario("carlos");

        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername(user.getUsername());

        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().getUsername());
        assertEquals(user.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("existsByEmail: debe retornar true si email existe")
    void existsByEmail_true() {
        User user = crearUsuario("ana");

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail(user.getEmail());

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByUsername: debe retornar true si username existe")
    void existsByUsername_true() {
        User user = crearUsuario("benja");

        userRepository.save(user);

        boolean exists = userRepository.existsByUsername(user.getUsername());

        assertTrue(exists);
    }
}