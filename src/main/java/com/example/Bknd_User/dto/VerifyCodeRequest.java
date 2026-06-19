package com.example.Bknd_User.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyCodeRequest {
    
    @NotBlank(message = "El correo es requerido")
    @Email(message = "El correo debe ser válido")
    private String email;
    
    @NotBlank(message = "El código es requerido")
    private String codigo;
}
