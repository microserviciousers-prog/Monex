package com.example.Bknd_User.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.Bknd_User.dto.UserDTO;
import com.example.Bknd_User.dto.UserUpdateRequest;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.service.UserServices;
import com.example.Bknd_User.service.JwtService;

@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios")
@RestController
@RequestMapping(path = "api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    
    @Autowired
    private UserServices userService;
    
    @Autowired
    private JwtService jwtService;
    
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene los datos de un usuario específico por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDTO> obtenerUsuario(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {

        User authUser = jwtService.comprobarToken(authHeader);

        if (!esMismoUsuarioOAdmin(authUser, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.obtenerPorId(id);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertirADTO(user));
    }
    
    @Operation(summary = "Obtener datos del usuario autenticado", description = "Retorna los datos del usuario actualmente autenticado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Datos del usuario"),
        @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDTO> obtenerMiPerfil(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {

        User user = jwtService.comprobarToken(authHeader);
        return ResponseEntity.ok(convertirADTO(user));
    }

    @Operation(summary = "Listar usuarios paginados", description = "Retorna usuarios de forma paginada. Solo disponible para ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuarios obtenidos correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/admin")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> listarUsuariosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {

        User authUser = jwtService.comprobarToken(authHeader);

        if (!"ADMIN".equalsIgnoreCase(authUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo administradores pueden ver los usuarios");
        }

        Page<UserDTO> usuarios = userService.listarUsuariosPaginados(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        ).map(this::convertirADTO);

        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Estadística de usuarios por mes", description = "Retorna la cantidad de usuarios registrados agrupados por mes. Solo disponible para usuarios ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estadística generada correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/admin/stats/users-by-month")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> obtenerUsuariosPorMes(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {

        User authUser = jwtService.comprobarToken(authHeader);

        if (!"ADMIN".equalsIgnoreCase(authUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo administradores pueden ver esta estadística");
        }

        return ResponseEntity.ok(userService.obtenerUsuariosPorMes());
    }
    
    @Operation(summary = "Actualizar usuario", description = "Actualiza la información de un usuario específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest updateRequest,
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {
        
        User authUser = jwtService.comprobarToken(authHeader);

        if (!esMismoUsuarioOAdmin(authUser, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            User user = userService.actualizarUsuario(id, updateRequest);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(convertirADTO(user));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
    
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {
        
        User authUser = jwtService.comprobarToken(authHeader);

        if (!esMismoUsuarioOAdmin(authUser, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        boolean eliminado = userService.eliminarUsuario(id, authHeader);

        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Cambiar contraseña", description = "Permite al usuario autenticado cambiar su contraseña")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
        @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PostMapping("/cambiar-password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> cambiarPassword(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        User authUser = jwtService.comprobarToken(authHeader);

        boolean success = userService.cambiarPassword(
                authUser.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        
        if (!success) {
            return ResponseEntity.badRequest().body("Contraseña actual incorrecta");
        }

        return ResponseEntity.ok("Contraseña actualizada exitosamente");
    }
    
    private boolean esMismoUsuarioOAdmin(User authUser, Long id) {
        return authUser.getId().equals(id) || "ADMIN".equalsIgnoreCase(authUser.getRole());
    }

    private UserDTO convertirADTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .role(user.getRole())
                .googleLinked(user.getGoogleLinked())
                .build();
    }
    
    @Schema(description = "Request para cambiar contraseña")
    public static class ChangePasswordRequest {

        @NotBlank(message = "La contraseña actual es requerida")
        private String currentPassword;
        
        @NotBlank(message = "La nueva contraseña es requerida")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        private String newPassword;
        
        public ChangePasswordRequest() {}
        
        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
        
        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }
        
        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}