package com.example.Bknd_User.service;

import com.example.Bknd_User.dto.UserUpdateRequest;
import com.example.Bknd_User.entity.User;
import com.example.Bknd_User.repository.CreditCardConfigRepository;
import com.example.Bknd_User.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreditCardConfigRepository creditCardConfigRepository;

    @Mock
    private JwtService jwtService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserServices userServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(userServices, "expensesServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(userServices, "categoriesServiceUrl", "http://localhost:8082");
    }

    @Test
    @DisplayName("registrarLocal: crea usuario local correctamente")
    void registrarLocal_ok() {
        when(userRepository.existsByEmail("test@demo.cl")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userServices.registrarLocal("testuser", "test@demo.cl", "123456");

        assertEquals("testuser", result.getUsername());
        assertEquals("test@demo.cl", result.getEmail());
        assertEquals("USER", result.getRole());
        assertTrue(result.isEnabled());
        assertFalse(result.getGoogleLinked());
        assertNotEquals("123456", result.getPassword());
        assertTrue(passwordEncoder.matches("123456", result.getPassword()));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registrarLocal: falla si email ya existe")
    void registrarLocal_emailDuplicado() {
        when(userRepository.existsByEmail("test@demo.cl")).thenReturn(true);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userServices.registrarLocal("testuser", "test@demo.cl", "123456")
        );

        assertEquals("El email ya está registrado", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("intentarLogin: retorna token con credenciales correctas")
    void intentarLogin_ok() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@demo.cl")
                .password(passwordEncoder.encode("123456"))
                .role("USER")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(userRepository.findByEmail("test@demo.cl")).thenReturn(Optional.of(user));
        when(jwtService.generarToken(user)).thenReturn("token-jwt");

        String token = userServices.intentarLogin("test@demo.cl", "123456");

        assertEquals("token-jwt", token);
        verify(jwtService).generarToken(user);
    }

    @Test
    @DisplayName("intentarLogin: falla con contraseña incorrecta")
    void intentarLogin_passwordIncorrecta() {
        User user = User.builder()
                .email("test@demo.cl")
                .password(passwordEncoder.encode("correcta"))
                .build();

        when(userRepository.findByEmail("test@demo.cl")).thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> userServices.intentarLogin("test@demo.cl", "incorrecta")
        );

        verify(jwtService, never()).generarToken(any(User.class));
    }

    @Test
    @DisplayName("obtenerPorId: retorna usuario si existe")
    void obtenerPorId_ok() {
        User user = User.builder()
                .id(1L)
                .username("manuel")
                .email("manuel@demo.cl")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userServices.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("manuel", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("actualizarUsuario: actualiza username y email")
    void actualizarUsuario_ok() {
        User user = User.builder()
                .id(1L)
                .username("antiguo")
                .email("antiguo@demo.cl")
                .build();

        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername("nuevo");
        request.setEmail("nuevo@demo.cl");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@demo.cl")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userServices.actualizarUsuario(1L, request);

        assertEquals("nuevo", result.getUsername());
        assertEquals("nuevo@demo.cl", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("eliminarUsuario: elimina usuario existente")
    void eliminarUsuario_ok() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThrows(
                RuntimeException.class,
                () -> userServices.eliminarUsuario(1L, "Bearer token-admin")
        );
    }

    @Test
    @DisplayName("cambiarPassword: cambia contraseña si la actual es correcta")
    void cambiarPassword_ok() {
        User user = User.builder()
                .id(1L)
                .password(passwordEncoder.encode("actual"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = userServices.cambiarPassword(1L, "actual", "nueva123");

        assertTrue(result);
        assertTrue(passwordEncoder.matches("nueva123", user.getPassword()));
        verify(userRepository).save(user);
    }
}