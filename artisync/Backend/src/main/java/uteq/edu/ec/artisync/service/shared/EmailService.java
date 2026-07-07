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
