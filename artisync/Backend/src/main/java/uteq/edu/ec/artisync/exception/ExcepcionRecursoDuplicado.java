package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ExcepcionRecursoDuplicado extends RuntimeException {
    public ExcepcionRecursoDuplicado(String mensaje) {
        super(mensaje);
    }
}
