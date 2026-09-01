package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.perfil.ICertificadoIaServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificadoIaServicioImpl implements ICertificadoIaServicio {

    private final CertificadoIaRepository certificadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoVerificacionRepository estadoRepository;

    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_EMITIR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#resultado.idCertificado",
            detalle = "{idUsuario: #peticion.idUsuario, idEstadoVerificacion: #peticion.idEstadoVerificacion}")
    public RespuestaCertificadoIa emitirCertificado(PeticionCrearCertificadoIa peticion) {
        Usuario usuario = usuarioRepository.findById(peticion.idUsuario())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado con ID: " + peticion.idUsuario()));

        EstadoVerificacion estado = estadoRepository.findById(peticion.idEstadoVerificacion())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Estado de verificación no encontrado con ID: " + peticion.idEstadoVerificacion()));

        CertificadoIa certificado = CertificadoIa.builder()
                .usuario(usuario)
                .estadoVerificacion(estado)
                .urlDocumentoS3(peticion.urlDocumentoS3())
                .puntajeConfianzaIa(peticion.puntajeConfianzaIa())
                .build();

        CertificadoIa guardado = certificadoRepository.save(certificado);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaCertificadoIa obtenerCertificadoPorId(Long idCertificado) {
        CertificadoIa certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Certificado IA no encontrado con ID: " + idCertificado));
        return mapearARespuesta(certificado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCertificadoIa> listarCertificadosPorUsuario(Long idUsuario) {
        return certificadoRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCertificadoIa> listarTodosLosCertificados() {
        return certificadoRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "CERTIFICADO_ELIMINAR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "certificados_ia", idEntidad = "#idCertificado")
    public void eliminarCertificado(Long idCertificado) {
        if (!certificadoRepository.existsById(idCertificado)) {
            throw new ExcepcionRecursoNoEncontrado("Certificado IA no encontrado con ID: " + idCertificado);
        }
        certificadoRepository.deleteById(idCertificado);
    }

    private RespuestaCertificadoIa mapearARespuesta(CertificadoIa certificado) {
        return RespuestaCertificadoIa.builder()
                .idCertificado(certificado.getIdCertificado())
                .idUsuario(certificado.getUsuario() != null ? certificado.getUsuario().getIdUsuario() : null)
                .idEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getIdEstadoVerificacion() : null)
                .nombreEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getNombreEstado() : null)
                .urlDocumentoS3(certificado.getUrlDocumentoS3())
                .puntajeConfianzaIa(certificado.getPuntajeConfianzaIa())
                .fechaAnalisis(certificado.getFechaAnalisis())
                .build();
    }
}

