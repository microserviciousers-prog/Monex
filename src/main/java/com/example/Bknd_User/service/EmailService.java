package com.example.Bknd_User.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarCodigoRecuperacion(String email, String codigo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Código de Recuperación de Contraseña - Monex");
            message.setText("Hola,\n\n" +
                    "Tu código de recuperación es: " + codigo + "\n\n" +
                    "Este código tiene una validez de 15 minutos.\n\n" +
                    "Si no solicitaste este código, ignora este correo.\n\n" +
                    "Saludos,\nEquipo Monex");
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo electrónico");
        }
    }
}
