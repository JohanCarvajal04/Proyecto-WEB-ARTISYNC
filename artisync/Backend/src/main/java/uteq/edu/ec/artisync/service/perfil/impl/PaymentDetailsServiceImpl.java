package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.perfil.PaymentDetailsRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PaymentDetailsResponse;
import uteq.edu.ec.artisync.entity.perfil.CreatorPaymentDetails;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.perfil.CreatorPaymentDetailsRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.perfil.IPaymentDetailsService;

@Service
@RequiredArgsConstructor
public class PaymentDetailsServiceImpl implements IPaymentDetailsService {

    private final CreatorPaymentDetailsRepository datosPagoCreadorRepository;
    private final UserRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PaymentDetailsResponse getMyPaymentDetails(Long idUsuario) {
        return datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .map(this::mapear)
                .orElseGet(() -> PaymentDetailsResponse.builder().correoPaypal(null).fechaActualizacion(null).build());
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_DATOS_PAGO_ACTUALIZAR", modulo = AuditModule.FINANZAS,
            entidad = "datos_pago_creador", idEntidad = "#idUsuario")
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PaymentDetailsResponse updatePaypalEmail(Long idUsuario, PaymentDetailsRequest peticion) {
        CreatorPaymentDetails datos = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseGet(() -> {
                    User usuario = usuarioRepository.findById(idUsuario)
                            .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
                    return CreatorPaymentDetails.builder().usuario(usuario).build();
                });

        datos.setCorreoPaypal(peticion.getCorreoPaypal());
        return mapear(datosPagoCreadorRepository.save(datos));
    }

    private PaymentDetailsResponse mapear(CreatorPaymentDetails datos) {
        return PaymentDetailsResponse.builder()
                .correoPaypal(datos.getCorreoPaypal())
                .fechaActualizacion(datos.getFechaActualizacion())
                .build();
    }
}
