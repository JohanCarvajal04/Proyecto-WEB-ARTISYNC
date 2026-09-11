package uteq.edu.ec.artisync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Componente de Seguridad: Punto de entrada para peticiones no autenticadas.
 * 
 * Propósito: Manejar de forma controlada y estandarizada los intentos de acceso a recursos protegidos sin proveer credenciales validas.
 * 
 * Flujo interno: Es invocado por el ExceptionTranslationFilter de Spring Security cuando se lanza una AuthenticationException. Retorna una respuesta JSON (ProblemDetail) con HTTP 401 en lugar del redirect por defecto a login HTML.
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Autowired
    public CustomAuthenticationEntryPoint(@Autowired(required = false) ObjectMapper objectMapper) {
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            this.objectMapper = new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        // Asegura el soporte de serializacion de ProblemDetail (RFC 7807) sin depender
        // de si el ObjectMapper inyectado ya trae el mixin de Spring MVC registrado.
        this.objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String jwtError = (String) request.getAttribute("JWT_ERROR");
        String message = jwtError != null ? jwtError : "AutenticaciÃ³n requerida";

        log.warn("Fallo de autenticaciÃ³n en {}: {} (ExcepciÃ³n: {})", request.getRequestURI(), message, authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
        problemDetail.setType(URI.create("https://artisync.dev/errors/autenticacion"));
        problemDetail.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}


