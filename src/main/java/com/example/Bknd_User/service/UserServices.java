package com.example.Bknd_User.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.Bknd_User.dto.CreditCardConfigRequest;
import org.springframework.transaction.annotation.Transactional;
import com.example.Bknd_User.dto.CreditCardConfigResponse;
import com.example.Bknd_User.dto.UserUpdateRequest;
import com.example.Bknd_User.entity.CreditCardConfig;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.repository.CreditCardConfigRepository;
import com.example.Bknd_User.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserServices {

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${expenses.service.url}")
    private String expensesServiceUrl;

    @Value("${categories.service.url}")
    private String categoriesServiceUrl;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CreditCardConfigRepository creditCardConfigRepository;
    
    @Autowired
    private com.example.Bknd_User.repository.PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String intentarLogin(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email o contraseña incorrectos"));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BadCredentialsException("Esta cuenta debe iniciar sesión con Google");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }
        
        return jwtService.generarToken(user);
    }
    
    public User obtenerPorId(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public User obtenerPorEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User registrar(String username, String email, String password) {
        return registrarLocal(username, email, password);
    }

    public User registrarLocal(String username, String email, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es requerida para usuarios creados localmente");
        }

        boolean emailExists = userRepository.existsByEmail(email);
        boolean usernameExists = userRepository.existsByUsername(username);
        
        if (emailExists && usernameExists) {
            throw new IllegalArgumentException("El email ya está registrado. El nombre de usuario ya está registrado");
        } else if (emailExists) {
            throw new IllegalArgumentException("El email ya está registrado");
        } else if (usernameExists) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        
        User nuevoUsuario = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .googleLinked(false)
                .googleId(null)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .role("USER")
                .build();
        
        return userRepository.save(nuevoUsuario);
    }

    public User vincularGoogle(String username, String email, String googleId) {
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            existingUser.setGoogleLinked(true);

            if (googleId != null && !googleId.isBlank()) {
                existingUser.setGoogleId(googleId);
            }

            if (existingUser.getUsername() == null || existingUser.getUsername().isBlank()) {
                existingUser.setUsername(username);
            }

            return userRepository.save(existingUser);
        }

        User nuevoUsuario = User.builder()
                .username(username)
                .email(email)
                .password(null)
                .googleLinked(true)
                .googleId(googleId)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .role("USER")
                .build();

        return userRepository.save(nuevoUsuario);
    }

    public String autenticarConGoogle(String idToken) {
        GoogleProfile profile = validarTokenGoogle(idToken);
        User user = vincularGoogle(profile.username(), profile.email(), profile.googleId());

        return jwtService.generarToken(user);
    }
    
    public User actualizarUsuario(Long id, UserUpdateRequest updateRequest) {
    User user = userRepository.findById(id).orElse(null);

    if (user == null) {
        return null;
    }

    if (updateRequest.getUsername() != null && !updateRequest.getUsername().isBlank()) {
        String nuevoUsername = updateRequest.getUsername();

        if (!nuevoUsername.equals(user.getUsername()) && userRepository.existsByUsername(nuevoUsername)) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        user.setUsername(nuevoUsername);
    }

    if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()) {
        String nuevoEmail = updateRequest.getEmail();

        if (!nuevoEmail.equals(user.getEmail()) && userRepository.existsByEmail(nuevoEmail)) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        user.setEmail(nuevoEmail);
    }

    if (updateRequest.getRole() != null &&
    !updateRequest.getRole().isBlank()) {

    String nuevoRole = updateRequest.getRole().trim().toUpperCase();

    if (!nuevoRole.equals("USER") &&
        !nuevoRole.equals("ADMIN")) {

        throw new IllegalArgumentException(
            "Rol inválido. Solo se permite USER o ADMIN"
        );
    }

    user.setRole(nuevoRole);
}

    return userRepository.save(user);
}
    
    public boolean eliminarUsuario(Long id, String authHeader) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        // Primero eliminar datos en microservicios externos
        eliminarDatosExternosDelUsuario(id, authHeader);

        // Eliminar configuración de tarjeta si existe
        creditCardConfigRepository.findByUserId(id)
                .ifPresent(creditCardConfigRepository::delete);

        // Eliminar tokens de recuperación asociados (esta transacción es activa en eliminarUsuario)
        java.util.List<com.example.Bknd_User.entity.PasswordRecoveryToken> tokens = passwordRecoveryTokenRepository.findByUserId(id);
        if (!tokens.isEmpty()) {
            passwordRecoveryTokenRepository.deleteAll(tokens);
        }

        // Finalmente eliminar el usuario
        userRepository.deleteById(id);
        return true;
    }

    private void eliminarDatosExternosDelUsuario(Long userId, String authHeader) {
        eliminarEnMicroservicio(expensesServiceUrl + "/api/expenses/admin/user/" + userId, "gastos", authHeader);
        eliminarEnMicroservicio(categoriesServiceUrl + "/api/categorias/admin/user/" + userId, "categorías", authHeader);
    }

    private void eliminarEnMicroservicio(String url, String nombreServicio, String authHeader) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", authHeader)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("No se pudieron eliminar los datos de " + nombreServicio
                        + " (status " + response.statusCode() + "): " + response.body());
            }

        } catch (IOException e) {
            throw new RuntimeException("No se pudo conectar con el microservicio de " + nombreServicio);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Se interrumpió la eliminación de datos de " + nombreServicio);
        }
    }
    
    public boolean cambiarPassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;
    }
    
    public CreditCardConfigResponse guardarConfiguracionTarjeta(User user, CreditCardConfigRequest request) {
        CreditCardConfig config = creditCardConfigRepository.findByUserId(user.getId())
                .orElse(new CreditCardConfig());
        
        config.setUser(user);
        config.setFechaFacturacion(request.getFechaFacturacion());
        config.setSueldoMes(request.getSueldoMes());
        config.setCupoTarjeta(request.getCupoTarjeta());
        
        CreditCardConfig saved = creditCardConfigRepository.save(config);
        
        return CreditCardConfigResponse.builder()
                .id(saved.getId())
                .fechaFacturacion(saved.getFechaFacturacion())
                .sueldoMes(saved.getSueldoMes())
                .cupoTarjeta(saved.getCupoTarjeta())
                .mensaje("Configuración de tarjeta guardada exitosamente")
                .build();
    }
    
    public CreditCardConfigResponse obtenerConfiguracionTarjeta(User user) {
        CreditCardConfig config = creditCardConfigRepository.findByUserId(user.getId()).orElse(null);
        
        if (config == null) {
            return null;
        }
        
        return CreditCardConfigResponse.builder()
                .id(config.getId())
                .fechaFacturacion(config.getFechaFacturacion())
                .sueldoMes(config.getSueldoMes())
                .cupoTarjeta(config.getCupoTarjeta())
                .build();
    }

    public Page<User> listarUsuariosPaginados(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<Map<String, Object>> obtenerUsuariosPorMes() {
        return userRepository.obtenerUsuariosPorMes()
                .stream()
                .map(resultado -> Map.of(
                        "mes", resultado[0],
                        "cantidad", resultado[1]
                ))
                .toList();
    }

    private GoogleProfile validarTokenGoogle(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BadCredentialsException("No se pudo validar el inicio de sesión con Google");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String aud = json.path("aud").asText("");
            String email = json.path("email").asText("");
            String emailVerified = json.path("email_verified").asText("false");
            String googleId = json.path("sub").asText("");
            String name = json.path("name").asText("");

            if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.equals(aud)) {
                throw new BadCredentialsException("El token de Google no coincide con el cliente configurado");
            }

            if (email.isBlank() || googleId.isBlank() || !"true".equalsIgnoreCase(emailVerified)) {
                throw new BadCredentialsException("La cuenta de Google no es válida");
            }

            String username = (name == null || name.isBlank()) ? email.split("@")[0] : name;
            return new GoogleProfile(username, email, googleId);
        } catch (IOException e) {
            throw new BadCredentialsException("No se pudo procesar la respuesta de Google");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadCredentialsException("No se pudo validar el inicio de sesión con Google");
        }
    }

    private record GoogleProfile(String username, String email, String googleId) {}
}