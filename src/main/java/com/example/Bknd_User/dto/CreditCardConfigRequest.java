package com.example.Bknd_User.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardConfigRequest {
    
    @NotNull(message = "Fecha de facturación es obligatoria")
    @Min(value = 1, message = "Fecha de facturación debe ser entre 1 y 31")
    @Max(value = 31, message = "Fecha de facturación debe ser entre 1 y 31")
    private Integer fechaFacturacion;
    
    @NotNull(message = "Sueldo del mes es obligatorio")
    private Double sueldoMes;
    
    @NotNull(message = "Cupo de tarjeta es obligatorio")
    private Double cupoTarjeta;
}
