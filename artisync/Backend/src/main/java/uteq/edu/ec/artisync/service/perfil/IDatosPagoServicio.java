package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDatosPago;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaDatosPago;

public interface IDatosPagoServicio {

    /** Nunca lanza si no existe todavía: devuelve correoPaypal=null (el creador aún no lo configuró). */
    RespuestaDatosPago obtenerMisDatosPago(Long idUsuario);

    RespuestaDatosPago actualizarCorreoPaypal(Long idUsuario, PeticionDatosPago peticion);
}
