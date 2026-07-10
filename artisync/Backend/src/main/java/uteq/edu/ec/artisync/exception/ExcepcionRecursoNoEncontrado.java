package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExcepcionRecursoNoEncontrado extends RuntimeException {
    public ExcepcionRecursoNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
