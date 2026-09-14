package uteq.edu.ec.artisync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ArtisyncApplication {

	/**
	 * Punto de entrada del backend: arranca el contexto de Spring Boot.
	 * @param args argumentos de línea de comandos, delegados a {@link SpringApplication}
	 */
	public static void main(String[] args) {
		SpringApplication.run(ArtisyncApplication.class, args);
	}

	/**
	 * {@link ObjectMapper} por defecto de Jackson, expuesto como bean para que
	 * los componentes que lo necesitan inyectado (por ejemplo, para parsear el
	 * JSONB devuelto por las funciones de base de datos) usen la misma instancia.
	 * @return una nueva instancia de {@link ObjectMapper} con la configuración por defecto
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
