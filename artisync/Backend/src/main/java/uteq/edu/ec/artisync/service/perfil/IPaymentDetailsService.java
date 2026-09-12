package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.PaymentDetailsRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PaymentDetailsResponse;

public interface IPaymentDetailsService {

    /**
     * Obtiene los datos de pago del usuario.
     * Nunca lanza si no existe todavía: devuelve correoPaypal=null (el creador aún no lo configuró).
     *
     * @param idUsuario id del usuario
     * @return los datos de pago del usuario, con {@code correoPaypal} nulo si no los ha configurado
     */
    PaymentDetailsResponse getMyPaymentDetails(Long idUsuario);

    /**
     * Configura o reemplaza el correo de PayPal del usuario.
     *
     * @param idUsuario id del usuario
     * @param peticion  nuevo correo de PayPal
     * @return los datos de pago ya actualizados
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     */
    PaymentDetailsResponse updatePaypalEmail(Long idUsuario, PaymentDetailsRequest peticion);
}
