package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.request.profile.PaymentDetailsRequest;
import uteq.edu.ec.artisync.dto.response.profile.PaymentDetailsResponse;
import uteq.edu.ec.artisync.entity.profile.CreatorPaymentDetails;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.profile.CreatorPaymentDetailsRepository;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.service.profile.IPaymentDetailsService;

@Service
@RequiredArgsConstructor
public class PaymentDetailsServiceImpl implements IPaymentDetailsService {

    private final CreatorPaymentDetailsRepository datosPagoCreadorRepository;
    private final UserRepository usuarioRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PaymentDetailsResponse getMyPaymentDetails(Long idUsuario) {
        return datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .map(this::map)
                .orElseGet(() -> PaymentDetailsResponse.builder().correoPaypal(null).fechaActualizacion(null).build());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Auditable(accion = "RETIRO_DATOS_PAGO_ACTUALIZAR", modulo = AuditModule.FINANZAS,
            entidad = "datos_pago_creador", idEntidad = "#idUsuario")
    public PaymentDetailsResponse updatePaypalEmail(Long idUsuario, PaymentDetailsRequest peticion) {
        CreatorPaymentDetails datos = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseGet(() -> {
                    User usuario = usuarioRepository.findById(idUsuario)
                            .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
                    return CreatorPaymentDetails.builder().usuario(usuario).build();
                });

        datos.setCorreoPaypal(peticion.getCorreoPaypal());
        return map(datosPagoCreadorRepository.save(datos));
    }

    private PaymentDetailsResponse map(CreatorPaymentDetails datos) {
        return PaymentDetailsResponse.builder()
                .correoPaypal(datos.getCorreoPaypal())
                .fechaActualizacion(datos.getFechaActualizacion())
                .build();
    }
}
