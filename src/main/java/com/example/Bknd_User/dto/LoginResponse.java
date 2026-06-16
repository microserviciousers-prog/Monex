package com.example.Bknd_User.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private Long expiresIn;

    // Constructor simple (seguro)
    public LoginResponse(String token, Long expiresIn) {
        this.accessToken = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }
}