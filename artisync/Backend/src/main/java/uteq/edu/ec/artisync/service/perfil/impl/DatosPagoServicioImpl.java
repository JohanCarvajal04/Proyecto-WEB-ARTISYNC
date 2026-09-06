package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDatosPago;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaDatosPago;
import uteq.edu.ec.artisync.entity.perfil.DatosPagoCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.perfil.IDatosPagoServicio;

@Service
@RequiredArgsConstructor
public class DatosPagoServicioImpl implements IDatosPagoServicio {

    private final DatosPagoCreadorRepository datosPagoCreadorRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public RespuestaDatosPago obtenerMisDatosPago(Long idUsuario) {
        return datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .map(this::mapear)
                .orElseGet(() -> RespuestaDatosPago.builder().correoPaypal(null).fechaActualizacion(null).build());
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_DATOS_PAGO_ACTUALIZAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "datos_pago_creador", idEntidad = "#idUsuario")
    public RespuestaDatosPago actualizarCorreoPaypal(Long idUsuario, PeticionDatosPago peticion) {
        DatosPagoCreador datos = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findById(idUsuario)
                            .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado"));
                    return DatosPagoCreador.builder().usuario(usuario).build();
                });

        datos.setCorreoPaypal(peticion.getCorreoPaypal());
        return mapear(datosPagoCreadorRepository.save(datos));
    }

    private RespuestaDatosPago mapear(DatosPagoCreador datos) {
        return RespuestaDatosPago.builder()
                .correoPaypal(datos.getCorreoPaypal())
                .fechaActualizacion(datos.getFechaActualizacion())
                .build();
    }
}
