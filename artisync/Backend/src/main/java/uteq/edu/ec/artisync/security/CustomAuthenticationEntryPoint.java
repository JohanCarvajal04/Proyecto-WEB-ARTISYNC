package uteq.edu.ec.artisync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaError;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

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
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String jwtError = (String) request.getAttribute("JWT_ERROR");
        String message = jwtError != null ? jwtError : "Autenticación requerida";

        log.warn("Fallo de autenticación en {}: {} (Excepción: {})", request.getRequestURI(), message, authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        RespuestaError respuestaError = RespuestaError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(null)
                .build();

        objectMapper.writeValue(response.getWriter(), respuestaError);
    }
}

