package com.example.Bknd_User.controllers;

import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.service.JwtService;
import com.example.Bknd_User.service.PasswordRecoveryService;
import com.example.Bknd_User.service.UserServices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest {

        private MockMvc mockMvc;

        @Mock
        private UserServices userService;

        @Mock
        private JwtService jwtService;

        @Mock
        private PasswordRecoveryService passwordRecoveryService;

        @InjectMocks
        private AuthenticationController authenticationController;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);

                ReflectionTestUtils.setField(authenticationController, "jwtExpiration", 86400000L);

                mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        }

        @Test
        @DisplayName("POST /api/auth/login retorna token JWT")
        void login_ok() throws Exception {
                when(userService.intentarLogin("test@demo.cl", "12345678"))
                                .thenReturn("token-jwt");

                String json = """
                                {
                                "email": "test@demo.cl",
                                "password": "12345678"
                                }
                                """;

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.access_token").value("token-jwt"))
                                .andExpect(jsonPath("$.token_type").value("Bearer"))
                                .andExpect(jsonPath("$.expires_in").value(86400000));
        }

        @Test
        @DisplayName("POST /api/auth/register crea usuario")
        void register_ok() throws Exception {
                User user = User.builder()
                                .id(1L)
                                .username("manuel")
                                .email("manuel@demo.cl")
                                .enabled(true)
                                .role("USER")
                                .build();

                when(userService.registrar("manuel", "manuel@demo.cl", "12345678"))
                                .thenReturn(user);

                String json = """
                                {
                                "username": "manuel",
                                "email": "manuel@demo.cl",
                                "password": "12345678"
                                }
                                """;

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.username").value("manuel"))
                                .andExpect(jsonPath("$.email").value("manuel@demo.cl"))
                                .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("POST /api/auth/register retorna 400 si email ya existe")
        void register_emailDuplicado() throws Exception {
                when(userService.registrar("manuel", "manuel@demo.cl", "12345678"))
                                .thenThrow(new IllegalArgumentException("El email ya está registrado"));

                String json = """
                                {
                                "username": "manuel",
                                "email": "manuel@demo.cl",
                                "password": "12345678"
                                }
                                """;

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("El email ya está registrado"));
        }

        @Test
        @DisplayName("GET /api/auth/me retorna usuario autenticado")
        void getCurrentUser_ok() throws Exception {
                User user = User.builder()
                                .id(1L)
                                .username("manuel")
                                .email("manuel@demo.cl")
                                .role("USER")
                                .enabled(true)
                                .build();

                when(jwtService.comprobarToken(anyString()))
                                .thenReturn(user);

                mockMvc.perform(get("/api/auth/me")
                                .header("Authorization", "Bearer token-jwt"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("manuel"))
                                .andExpect(jsonPath("$.email").value("manuel@demo.cl"))
                                .andExpect(jsonPath("$.role").value("USER"));
        }
}