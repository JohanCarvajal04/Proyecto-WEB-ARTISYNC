package uteq.edu.ec.artisync.service.shared;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Componente de Offering Transversal para notificaciones por correo electronico.
 * 
 * Propósito: Proveer una abstracción asíncrona para la composicion y envio de correos 
 * transaccionales utilizando JavaMailSender y plantillas HTML de Thymeleaf.
 * 
 * Responsabilidad arquitectónica: Opera fuera del hilo principal de las peticiones web (@Async) 
 * para garantizar que los tiempos de respuesta de la API no se degraden durante la 
 * resolución SMTP. Su uso principal incluye la recuperación de credenciales y avisos críticos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    /**
     * Despacha un mensaje o notificacion a los destinatarios especificados.
     *
     * @param destinatario parametro requerido para la correcta ejecucion del procedimiento
     * @param nombres parametro requerido para la correcta ejecucion del procedimiento
     * @param tokenPlano parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void enviarCorreoRecuperacion(String destinatario, String nombres, String tokenPlano) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            Context contexto = new Context();
            contexto.setVariable("nombres", nombres);
            contexto.setVariable("enlaceRecuperacion", frontendUrl + "/auth/reset-password?token=" + tokenPlano);

            String contenidoHtml = templateEngine.process("email/recuperacion", contexto);

            helper.setFrom(remitente, "Artisync Soporte");
            helper.setTo(destinatario);
            helper.setSubject("🔒 Restablece tu contraseña en Artisync");
            helper.setText(contenidoHtml, true);

            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado exitosamente a: {}", destinatario);

        } catch (Exception e) {
            log.error("Error al enviar el correo de recuperación a {}: {}", destinatario, e.getMessage());
        }
    }
}
