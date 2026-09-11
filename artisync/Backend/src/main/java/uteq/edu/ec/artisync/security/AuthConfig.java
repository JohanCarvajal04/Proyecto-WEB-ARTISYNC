package uteq.edu.ec.artisync.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Componente Core de Seguridad: Configuracion de beans de autenticacion.
 * 
 * Propósito: Exponer y configurar los beans fundamentales para el proceso de validacion criptografica y autenticacion.
 * 
 * Flujo interno: Registra el PasswordEncoder (BCrypt), el AuthenticationProvider delegando al UserDetailsService, y el AuthenticationManager global.
 */
@Configuration
@RequiredArgsConstructor
public class AuthConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }
}

