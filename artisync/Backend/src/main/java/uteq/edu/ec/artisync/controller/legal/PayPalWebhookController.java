package uteq.edu.ec.artisync.controller.legal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.service.legal.IPaymentService;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PayPalWebhookController {

    private final IPaymentService pagoServicio;

    /**
     * Endpoint público para recibir notificaciones de PayPal.
     * No requiere autenticación JWT (configurado en SecurityConfig).
     * PayPal envía headers de verificación de firma.
     *
     * @param payload cuerpo del evento enviado por PayPal
     * @param transmissionId identificador de transmisión del webhook, usado en la verificación de firma
     * @param transmissionTime marca de tiempo de la transmisión del webhook
     * @param transmissionSig firma de la transmisión del webhook
     * @param certUrl URL del certificado usado para verificar la firma
     * @param authAlgo algoritmo de firma utilizado
     * @param authVersion versión del esquema de autenticación utilizado
     * @return respuesta de confirmación "OK" para PayPal
     */
    @PostMapping("/paypal")
    public ResponseEntity<String> recibirWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-ID", required = false) String transmissionId,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-TIME", required = false) String transmissionTime,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-SIG", required = false) String transmissionSig,
            @RequestHeader(value = "PAYPAL-CERT-URL", required = false) String certUrl,
            @RequestHeader(value = "PAYPAL-AUTH-ALGO", required = false) String authAlgo,
            @RequestHeader(value = "PAYPAL-AUTH-VERSION", required = false) String authVersion) {

        log.info("Webhook PayPal recibido - transmissionId: {}", transmissionId);

        pagoServicio.procesarWebhookPayPal(payload, transmissionId, transmissionTime,
                transmissionSig, certUrl, authAlgo, authVersion);

        return ResponseEntity.ok("OK");
    }
}
