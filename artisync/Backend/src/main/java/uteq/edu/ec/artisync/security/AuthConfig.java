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

    /**
     * Codificador de contraseñas usado en registro, login y cambio de contraseña.
     * Factor de coste 12 (2^12 iteraciones), por encima del default de BCrypt (10),
     * como margen frente al abaratamiento del hardware de fuerza bruta.
     *
     * @return un {@link BCryptPasswordEncoder} con factor de coste 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Autenticador usado por {@code AuthServiceImpl.login} para validar
     * correo y contraseña contra la base de datos.
     *
     * @return un {@link AuthenticationManager} que delega en un {@link DaoAuthenticationProvider}
     *         respaldado por {@link #userDetailsService} y {@link #passwordEncoder()}
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }
}

