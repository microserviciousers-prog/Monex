package com.example.Bknd_User.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.Bknd_User.dto.CreditCardConfigRequest;
import com.example.Bknd_User.dto.CreditCardConfigResponse;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.service.UserServices;
import com.example.Bknd_User.service.JwtService;

@Tag(name = "Configuración de Tarjeta", description = "Endpoints para gestión de tarjeta de crédito")
@RestController
@RequestMapping(path = "api/tarjeta", produces = MediaType.APPLICATION_JSON_VALUE)
public class TarjetaController {
    
    @Autowired
    private UserServices userService;
    
    @Autowired
    private JwtService jwtService;
    
    @Operation(summary = "Obtener configuración de tarjeta de crédito", 
               description = "Obtiene la configuración de tarjeta de crédito del usuario autenticado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración obtenida"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/configuracion")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> obtenerConfiguracionTarjeta(
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            User user = jwtService.comprobarToken(authHeader);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Usuario no autenticado");
            }
            
            CreditCardConfigResponse response = userService.obtenerConfiguracionTarjeta(user);
            
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Debe configurar su tarjeta de crédito para acceder a esta información");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener la configuración: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Guardar configuración de tarjeta de crédito", 
               description = "Guarda la fecha de facturación, sueldo del mes y cupo de tarjeta de crédito del usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración guardada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/configuracion")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> guardarConfiguracionTarjeta(
            @Valid @RequestBody CreditCardConfigRequest request,
            @Parameter(hidden = true)
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            User user = jwtService.comprobarToken(authHeader);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Usuario no autenticado");
            }
            
            CreditCardConfigResponse response = userService.guardarConfiguracionTarjeta(user, request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar la configuración: " + e.getMessage());
        }
    }
}
