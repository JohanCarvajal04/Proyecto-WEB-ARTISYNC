package uteq.edu.ec.artisync.security;
import uteq.edu.ec.artisync.controller.security.*;
import uteq.edu.ec.artisync.repository.security.*;
import uteq.edu.ec.artisync.repository.profile.*;
import uteq.edu.ec.artisync.dto.security.request.*;
import uteq.edu.ec.artisync.dto.security.response.*;
import uteq.edu.ec.artisync.dto.response.comun.*;
import uteq.edu.ec.artisync.service.security.*;
import uteq.edu.ec.artisync.service.security.impl.*;
import uteq.edu.ec.artisync.service.shared.*;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldContinue_WhenNoAuthHeader() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldRejectRefreshTokenAsAccessToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer refresh-token-string");

        Claims claims = mock(Claims.class);
        when(jwtService.extractAllClaims("refresh-token-string")).thenReturn(claims);
        when(claims.get("type")).thenReturn("refresh");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_ShouldRejectBlacklistedToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer blacklisted-token");

        Claims claims = mock(Claims.class);
        when(jwtService.extractAllClaims("blacklisted-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getId()).thenReturn("jti-123");
        when(redisTemplate.hasKey("jti:jti-123")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_ShouldRejectTokenWithUnknownType() throws ServletException, IOException {
        // OBS-AUTO-05: lista blanca — un token sin claim "type" (p. ej. emitido antes
        // del despliegue de esta correccion) o con un valor desconocido debe rechazarse,
        // no aceptarse por defecto.
        request.addHeader("Authorization", "Bearer legacy-token-without-type");

        Claims claims = mock(Claims.class);
        when(jwtService.extractAllClaims("legacy-token-without-type")).thenReturn(claims);
        when(claims.get("type")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenUserDisabled() throws ServletException, IOException {
        // §2.4 (OBS-AUTO-05): una cuenta deshabilitada (estadoCuenta=false) no debe
        // autenticar aunque el JWT sea valido y no este en la blacklist de Redis.
        request.addHeader("Authorization", "Bearer disabled-user-token");

        Claims claims = mock(Claims.class);
        when(jwtService.extractAllClaims("disabled-user-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getId()).thenReturn("jti-456");
        when(redisTemplate.hasKey("jti:jti-456")).thenReturn(false);
        when(claims.get("email", String.class)).thenReturn("deshabilitado@example.com");

        UserDetails userDetailsDeshabilitado = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("deshabilitado@example.com")).thenReturn(userDetailsDeshabilitado);
        when(jwtService.isAccessTokenValid("disabled-user-token", userDetailsDeshabilitado)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService).loadUserByUsername("deshabilitado@example.com");
    }

    @Test
    void doFilterInternal_ShouldAuthenticate_WhenTokenValidAndUserEnabled() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");

        Claims claims = mock(Claims.class);
        when(jwtService.extractAllClaims("valid-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getId()).thenReturn("jti-789");
        when(redisTemplate.hasKey("jti:jti-789")).thenReturn(false);
        when(claims.get("email", String.class)).thenReturn("activo@example.com");

        UserDetails userDetailsActivo = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("activo@example.com")).thenReturn(userDetailsActivo);
        when(jwtService.isAccessTokenValid("valid-token", userDetailsActivo)).thenReturn(true);
        when(userDetailsActivo.getAuthorities()).thenReturn(java.util.List.of());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetailsActivo, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}

