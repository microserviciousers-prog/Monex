package com.example.Bknd_User.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@monex.app}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void enviarCodigoRecuperacion(String email, String codigo) {
        try {
            String htmlContent = "<html><body>" +
                    "<h2>Código de Recuperación de Contraseña - Monex</h2>" +
                    "<p>Hola,</p>" +
                    "<p>Tu código de recuperación es: <strong>" + codigo + "</strong></p>" +
                    "<p>Este código tiene una validez de 15 minutos.</p>" +
                    "<p>Si no solicitaste este código, ignora este correo.</p>" +
                    "<p>Saludos,<br>Equipo Monex</p>" +
                    "</body></html>";

            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("from", fromEmail);
            emailRequest.put("to", email);
            emailRequest.put("subject", "Código de Recuperación de Contraseña - Monex");
            emailRequest.put("html", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            String requestBody = objectMapper.writeValueAsString(emailRequest);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.postForObject("https://api.resend.com/emails", entity, String.class);

        } catch (Exception e) {
            System.err.println("Error enviando email con Resend: " + e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo electrónico");
        }
    }
}
