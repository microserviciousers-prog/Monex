package com.example.Bknd_User.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCardConfigResponse {
    private Long id;
    private Integer fechaFacturacion;
    private Double sueldoMes;
    private Double cupoTarjeta;
    private String mensaje;
}
