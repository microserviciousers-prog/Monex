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
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.service.UserServices;
import com.example.Bknd_User.service.JwtService;

@Tag(name = "Autenticación", description = "Endpoints para autenticación y gestión de tokens JWT")
@RestController
@RequestMapping(path = "api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {

    @Autowired
    private UserServices userService;

    @Autowired
    private JwtService jwtService;

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
}