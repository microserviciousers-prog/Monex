package com.example.Bknd_User.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.Bknd_User.dto.LoginRequest;
import com.example.Bknd_User.dto.LoginResponse;
import com.example.Bknd_User.dto.GoogleLoginRequest;
import com.example.Bknd_User.dto.RegisterRequest;
import com.example.Bknd_User.dto.UserDTO;
import com.example.Bknd_User.dto.PasswordRecoveryRequest;
import com.example.Bknd_User.dto.VerifyCodeRequest;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.service.UserServices;
import com.example.Bknd_User.service.JwtService;
import com.example.Bknd_User.service.PasswordRecoveryService;

@Tag(name = "Autenticación", description = "Endpoints para autenticación y gestión de tokens JWT")
@RestController
@RequestMapping(path = "api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {

    @Autowired
    private UserServices userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Operation(summary = "Login de usuario", description = "Permite a un usuario autenticarse y obtener un token JWT.")
    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        String token = userService.intentarLogin(loginRequest.getEmail(), loginRequest.getPassword());
        return ResponseEntity.ok(new LoginResponse(token, jwtExpiration));
    }

    @Operation(summary = "Login con Google", description = "Valida el token de Google, vincula o crea el usuario y devuelve un JWT propio.")
    @PostMapping("google")
    public ResponseEntity<LoginResponse> loginConGoogle(@RequestBody @Valid GoogleLoginRequest request) {
        String token = userService.autenticarConGoogle(request.getIdToken());
        return ResponseEntity.ok(new LoginResponse(token, jwtExpiration));
    }

    @Operation(summary = "Registro de usuario", description = "Crea un nuevo usuario en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, email o username ya registrado")
    })
    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            User created = userService.registrar(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            UserDTO response = UserDTO.builder()
                    .id(created.getId())
                    .username(created.getUsername())
                    .email(created.getEmail())
                    .enabled(created.getEnabled())
                    .role(created.getRole())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @Operation(summary = "Obtener mi perfil", description = "Devuelve los datos del usuario autenticado actualmente.")
    @GetMapping("me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDTO> getCurrentUser(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {

        User user = jwtService.comprobarToken(authHeader);

        UserDTO response = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Enviar código de recuperación", description = "Envía un código de verificación al correo registrado del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código enviado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El correo no está registrado")
    })
    @PostMapping("recuperar/enviar-codigo")
    public ResponseEntity<?> enviarCodigoRecuperacion(@RequestBody @Valid PasswordRecoveryRequest request) {
        try {
            passwordRecoveryService.enviarCodigoRecuperacion(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Se envió un código de verificación a tu correo"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar el código de recuperación");
        }
    }

    @Operation(summary = "Verificar código de recuperación", description = "Verifica el código de recuperación enviado al correo del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código verificado correctamente"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado o correo no registrado")
    })
    @PostMapping("recuperar/verificar-codigo")
    public ResponseEntity<?> verificarCodigoRecuperacion(@RequestBody @Valid VerifyCodeRequest request) {
        try {
            passwordRecoveryService.verificarCodigo(request.getEmail(), request.getCodigo());
            return ResponseEntity.ok(new MessageResponse("Código verificado correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al verificar el código");
        }
    }
}