package uteq.edu.ec.artisync.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.TwoFactorSetupResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.CodigoRespaldo2Fa;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.CodigoRespaldo2FaRepository;
import uteq.edu.ec.artisync.repository.UsuarioRepository;
import uteq.edu.ec.artisync.service.TwoFactorService;

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

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @Override
    @Transactional
    public TwoFactorSetupResponse setup2Fa(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secreto = key.getKey();

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseGet(() -> AutenticacionDosFactores.builder().usuario(usuario).build());

        dosFactores.setLlaveSecreta(secreto);
        dosFactores.setEstaHabilitado(false);
        autenticacionDosFactoresRepository.save(dosFactores);

        // Limpiar códigos anteriores si hubiera
        codigoRespaldo2FaRepository.deleteByUsuarioIdUsuario(usuario.getIdUsuario());

        // Generar 8 nuevos códigos de respaldo
        List<String> codigosPlano = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            codigosPlano.add(codigo);

            CodigoRespaldo2Fa respaldo = CodigoRespaldo2Fa.builder()
                    .usuario(usuario)
                    .codigoHash(hashSha256(codigo))
                    .usado(false)
                    .build();
            codigoRespaldo2FaRepository.save(respaldo);
        }

        String otpauthUri = String.format("otpauth://totp/Artisync:%s?secret=%s&issuer=Artisync", correo, secreto);

        return TwoFactorSetupResponse.builder()
                .secreto(secreto)
                .otpauthUri(otpauthUri)
                .codigosRespaldo(codigosPlano)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse confirm2Fa(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha iniciado la configuración de 2FA"));

        if (!validarTotp(dosFactores.getLlaveSecreta(), codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código TOTP inválido. Verifica la hora de tu dispositivo e inténtalo de nuevo.");
        }

        dosFactores.setEstaHabilitado(true);
        autenticacionDosFactoresRepository.save(dosFactores);

        return new MessageResponse("Autenticación de dos factores activada exitosamente");
    }

    @Override
    @Transactional
    public MessageResponse disable2Fa(String correo, String codigo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        AutenticacionDosFactores dosFactores = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no está configurado"));

        if (!Boolean.TRUE.equals(dosFactores.getEstaHabilitado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El 2FA no se encuentra activo");
        }

        if (!validarCodigoOBackup(correo, codigo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de autenticación o de respaldo incorrecto");
        }

        dosFactores.setEstaHabilitado(false);
        autenticacionDosFactoresRepository.save(dosFactores);
        codigoRespaldo2FaRepository.deleteByUsuarioIdUsuario(usuario.getIdUsuario());

        return new MessageResponse("Autenticación de dos factores desactivada exitosamente");
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

        // Probar si es un código de respaldo
        String hashIngresado = hashSha256(codigoIngresado.trim().toUpperCase());
        List<CodigoRespaldo2Fa> codigos = codigoRespaldo2FaRepository.findByUsuarioIdUsuarioAndUsadoFalse(usuario.getIdUsuario());
        for (CodigoRespaldo2Fa respaldo : codigos) {
            if (respaldo.getCodigoHash().equals(hashIngresado)) {
                respaldo.setUsado(true);
                codigoRespaldo2FaRepository.save(respaldo);
                log.info("Código de respaldo 2FA utilizado para el usuario: {}", correo);
                return true;
            }
        }

        return false;
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
