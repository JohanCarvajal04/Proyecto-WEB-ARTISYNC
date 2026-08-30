package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.seguridad.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.seguridad.CodigoRespaldo2FaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private final UsuarioRepository usuarioRepository;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    private final CodigoRespaldo2FaRepository codigoRespaldo2FaRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final CertificadoIaRepository certificadoIaRepository;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Override
    @Transactional
    // Nunca el secreto TOTP ni los códigos de respaldo en el detalle: solo el
    // hecho de que se inició la configuración.
    @Auditable(accion = "SEGURIDAD_2FA_CONFIGURAR", modulo = ModuloAuditoria.SEGURIDAD, correoActor = "#correo")
    public TwoFactorSetupResponse setup2Fa(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean esCreador = usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .anyMatch(ur -> "CREADOR".equalsIgnoreCase(ur.getRol().getNombreRol()));
        // V21: la verificación ya no cuelga de perfiles_creadores, cuelga
        // directo del usuario — no hace falta resolver el perfil para esto.
        if (esCreador && !certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                usuario.getIdUsuario(), "IDENTIDAD", "APROBADO")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes verificar tu identidad antes de activar la autenticación de dos factores");
        }

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secreto = key.getKey();

        // Generar 8 nuevos códigos de respaldo (en texto plano, para devolver
        // al usuario una única vez) y sus hashes (lo único que se persiste).
        List<String> codigosPlano = new ArrayList<>();
        String[] hashes = new String[8];
        for (int i = 0; i < 8; i++) {
            String codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            codigosPlano.add(codigo);
            hashes[i] = hashSha256(codigo);
        }

        // Fase 3 concurrencia (§7): fn_configurar_2fa hace el upsert del
        // secreto TOTP + el reemplazo completo de los codigos de respaldo en
        // UNA transaccion atomica, en vez de los 10 pasos no atomicos
        // anteriores (upsert manual + delete + 8 save() individuales), que
        // dejaban al usuario con un secreto nuevo y codigos incompletos si el
        // proceso fallaba a mitad del bucle.
        autenticacionDosFactoresRepository.configurar2Fa(usuario.getIdUsuario(), secreto, hashes);

        String otpauthUri = String.format("otpauth://totp/Artisync:%s?secret=%s&issuer=Artisync", correo, secreto);

        return TwoFactorSetupResponse.builder()
                .secreto(secreto)
                .otpauthUri(otpauthUri)
                .codigosRespaldo(codigosPlano)
                .build();
    }

    @Override
    @Transactional
    @Auditable(accion = "SEGURIDAD_2FA_ACTIVAR", modulo = ModuloAuditoria.SEGURIDAD, correoActor = "#correo")
    public RespuestaMensaje confirm2Fa(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha iniciado la configuración de 2FA"));

        if (!validarTotp(dosFactores.getLlaveSecreta(), codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado");
        }

        dosFactores.setEstaHabilitado(true);
        autenticacionDosFactoresRepository.save(dosFactores);

        return new RespuestaMensaje("Autenticación de dos factores activada exitosamente");
    }

    @Override
    @Transactional
    @Auditable(accion = "SEGURIDAD_2FA_DESACTIVAR", modulo = ModuloAuditoria.SEGURIDAD, correoActor = "#correo")
    public RespuestaMensaje disable2Fa(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no está configurado"));

        if (!Boolean.TRUE.equals(dosFactores.getEstaHabilitado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no se encuentra activo");
        }

        if (!validarCodigoOBackup(correo, codigo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado");
        }

        // Fase 3 concurrencia (§7): fn_desactivar_2fa desactiva el flag y
        // purga los codigos de respaldo en una unica transaccion, en vez del
        // UPDATE + DELETE separados anteriores.
        autenticacionDosFactoresRepository.desactivar2Fa(usuario.getIdUsuario());

        return new RespuestaMensaje("Autenticación de dos factores desactivada exitosamente");
    }

    @Override
    @Transactional
    public boolean validarCodigoOBackup(String correo, String codigoIngresado) {
        if (codigoIngresado == null || codigoIngresado.isBlank()) {
            return false;
        }

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElse(null);
        if (usuario == null) return false;

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElse(null);

        if (dosFactores == null || !Boolean.TRUE.equals(dosFactores.getEstaHabilitado())) {
            return false;
        }

        // Probar si es código TOTP de 6 dígitos
        if (codigoIngresado.matches("^[0-9]{6}$")) {
            if (validarTotp(dosFactores.getLlaveSecreta(), codigoIngresado)) {
                return true;
            }
        }

        // Probar si es un código de respaldo. §2 (Fase 1 concurrencia):
        // fn_consumir_codigo_respaldo_2fa hace el UPDATE atomico
        // "WHERE usado = FALSE" en una sola sentencia, en vez de leer todos los
        // códigos no usados a memoria y comparar en un bucle Java -- ese patrón
        // read-modify-write permitía que dos peticiones concurrentes con el
        // mismo código de respaldo lo consumieran ambas (actualización perdida).
        String hashIngresado = hashSha256(codigoIngresado.trim().toUpperCase());
        boolean consumido = codigoRespaldo2FaRepository.consumirCodigoRespaldo(usuario.getIdUsuario(), hashIngresado);
        if (consumido) {
            log.info("Código de respaldo 2FA utilizado para el usuario: {}", correo);
        }
        return consumido;
    }

    private boolean validarTotp(String secreto, String codigo) {
        try {
            int codigoInt = Integer.parseInt(codigo);
            return gAuth.authorize(secreto, codigoInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular hash SHA-256", e);
        }
    }
}

