package com.example.Bknd_User.service;

import com.example.Bknd_User.entity.PasswordRecoveryToken;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.repository.PasswordRecoveryTokenRepository;
import com.example.Bknd_User.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordRecoveryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordRecoveryTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    private static final int TOKEN_EXPIRATION_MINUTES = 15;
    private static final Random random = new Random();

    public void enviarCodigoRecuperacion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El correo no está registrado"));

        String codigo = generarCodigo();

        PasswordRecoveryToken token = PasswordRecoveryToken.builder()
                .user(user)
                .token(codigo)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                .build();

        tokenRepository.save(token);
        emailService.enviarCodigoRecuperacion(email, codigo);
    }

    public boolean verificarCodigo(String email, String codigo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El correo no está registrado"));

        PasswordRecoveryToken token = tokenRepository.findByUserAndTokenAndUsedFalse(user, codigo)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o ya utilizado"));

        if (!token.isValid()) {
            throw new IllegalArgumentException("El código ha expirado");
        }

        token.setUsed(true);
        tokenRepository.save(token);

        return true;
    }

    public void cambiarContraseña(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El correo no está registrado"));

        user.setPassword(newPassword);
        userRepository.save(user);
    }

    private String generarCodigo() {
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }
}
