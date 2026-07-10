package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.perfil.ICertificadoIaServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificadoIaServicioImpl implements ICertificadoIaServicio {

    private final CertificadoIaRepository certificadoRepository;
    private final PerfilCreadorRepository perfilRepository;
    private final EstadoVerificacionRepository estadoRepository;

    @Override
    @Transactional
    public RespuestaCertificadoIa emitirCertificado(PeticionCrearCertificadoIa peticion) {
        PerfilCreador perfil = perfilRepository.findById(peticion.idPerfil())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil no encontrado con ID: " + peticion.idPerfil()));

        EstadoVerificacion estado = estadoRepository.findById(peticion.idEstadoVerificacion())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Estado de verificación no encontrado con ID: " + peticion.idEstadoVerificacion()));

        CertificadoIa certificado = CertificadoIa.builder()
                .perfil(perfil)
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
    public List<RespuestaCertificadoIa> listarCertificadosPorPerfil(Long idPerfil) {
        return certificadoRepository.findByPerfilIdPerfil(idPerfil).stream()
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
    public RespuestaCertificadoIa actualizarEstadoVerificacion(Long idCertificado, Long idNuevoEstado) {
        CertificadoIa certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Certificado IA no encontrado con ID: " + idCertificado));

        EstadoVerificacion nuevoEstado = estadoRepository.findById(idNuevoEstado)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Estado de verificación no encontrado con ID: " + idNuevoEstado));

        certificado.setEstadoVerificacion(nuevoEstado);
        CertificadoIa actualizado = certificadoRepository.save(certificado);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    public void eliminarCertificado(Long idCertificado) {
        if (!certificadoRepository.existsById(idCertificado)) {
            throw new ExcepcionRecursoNoEncontrado("Certificado IA no encontrado con ID: " + idCertificado);
        }
        certificadoRepository.deleteById(idCertificado);
    }

    private RespuestaCertificadoIa mapearARespuesta(CertificadoIa certificado) {
        return RespuestaCertificadoIa.builder()
                .idCertificado(certificado.getIdCertificado())
                .idPerfil(certificado.getPerfil() != null ? certificado.getPerfil().getIdPerfil() : null)
                .idEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getIdEstadoVerificacion() : null)
                .nombreEstadoVerificacion(certificado.getEstadoVerificacion() != null ? certificado.getEstadoVerificacion().getNombreEstado() : null)
                .urlDocumentoS3(certificado.getUrlDocumentoS3())
                .puntajeConfianzaIa(certificado.getPuntajeConfianzaIa())
                .fechaAnalisis(certificado.getFechaAnalisis())
                .build();
    }
}

