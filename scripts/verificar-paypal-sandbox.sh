#!/usr/bin/env bash
# Apoyo ejecutable para cerrar REQ-F-022b, REQ-F-022c y REQ-NF-019 contra el
# sandbox real de PayPal (api-m.sandbox.paypal.com), no contra los mocks de
# Mockito que usan PagoServicioImplWebhookTest y compania.
#
# Lo que este script SI automatiza (no depende de tu login en el navegador):
#   - Pedir un access token OAuth2 a PayPal con las credenciales del .env.
#   - Consultar el estado real de una orden/captura directamente en PayPal.
#   - Llamar a los endpoints ya existentes del backend (login, crear orden de
#     pago, estado de pago, cancelar/reembolsar) para no repetir esos pasos a
#     mano en cada corrida.
#
# Lo que NO automatiza, por diseno (no se introducen credenciales de terceros
# por script, ni siquiera de sandbox): iniciar sesion en el checkout de
# PayPal con la cuenta de prueba "Personal" y aprobar el pago. Esos pasos
# quedan impresos como instrucciones manuales en los subcomandos `fase*`.
#
# Variables de entorno esperadas (cargalas antes de llamar al script, p.ej.
# `set -a; source artisync/.env; set +a`):
#   PAYPAL_CLIENT_ID, PAYPAL_CLIENT_SECRET, PAYPAL_MODE (sandbox|live)
#   BACKEND_URL (por defecto http://localhost:8080)
#
# Uso:
#   bash scripts/verificar-paypal-sandbox.sh fase0|fase1|fase2|fase3
#   bash scripts/verificar-paypal-sandbox.sh token
#   bash scripts/verificar-paypal-sandbox.sh orden <orderId>
#   bash scripts/verificar-paypal-sandbox.sh login <correo> <contrasena>
#   bash scripts/verificar-paypal-sandbox.sh crear-pago <idPedido> <accessToken>
#   bash scripts/verificar-paypal-sandbox.sh estado-pago <idPedido> <accessToken>
#   bash scripts/verificar-paypal-sandbox.sh cancelar-pago <idPedido> <accessToken> [REEMBOLSAR|LIBERAR]
#
# Ver docs/mediciones/pagos/REPORTE-PAYPAL-SANDBOX.md para la evidencia
# archivada de cada corrida real.

set -uo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
PAYPAL_MODE="${PAYPAL_MODE:-sandbox}"

if [[ "$PAYPAL_MODE" == "sandbox" ]]; then
    PAYPAL_API="https://api-m.sandbox.paypal.com"
else
    PAYPAL_API="https://api-m.paypal.com"
fi

need_paypal_credentials() {
    if [[ -z "${PAYPAL_CLIENT_ID:-}" || -z "${PAYPAL_CLIENT_SECRET:-}" ]]; then
        echo "[FALLO] Falta PAYPAL_CLIENT_ID / PAYPAL_CLIENT_SECRET en el entorno." >&2
        echo "        Cargalos con: set -a; source artisync/.env; set +a" >&2
        exit 1
    fi
}

obtener_token() {
    local resp
    resp="$(curl -sS --max-time 20 -u "$PAYPAL_CLIENT_ID:$PAYPAL_CLIENT_SECRET" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        "$PAYPAL_API/v1/oauth2/token")"
    echo "$resp" | grep -o '"access_token":"[^"]*"' | head -1 | cut -d'"' -f4
}

cmd_token() {
    need_paypal_credentials
    local token
    token="$(obtener_token)"
    if [[ -z "$token" ]]; then
        echo "[FALLO] PayPal no devolvio access_token. Revisa PAYPAL_CLIENT_ID/SECRET y PAYPAL_MODE=$PAYPAL_MODE." >&2
        exit 1
    fi
    echo "[OK] access_token obtenido de $PAYPAL_API (modo=$PAYPAL_MODE):"
    echo "$token"
}

cmd_orden() {
    local order_id="${1:?Uso: orden <orderId>}"
    need_paypal_credentials
    local token
    token="$(obtener_token)"
    [[ -z "$token" ]] && { echo "[FALLO] No se pudo obtener token de PayPal." >&2; exit 1; }

    echo "== Orden $order_id en $PAYPAL_API =="
    curl -sS --max-time 20 -H "Authorization: Bearer $token" \
        "$PAYPAL_API/v2/checkout/orders/$order_id" | tee /dev/stderr | \
        grep -oE '"status":"[A-Z_]*"|"id":"[A-Za-z0-9-]*"' \
        > /dev/null
    echo
    echo "Busca en el JSON de arriba:"
    echo "  - status de la orden (CREATED / APPROVED / COMPLETED / VOIDED)"
    echo "  - purchase_units[].payments.captures[].status (COMPLETED / REFUNDED)"
}

cmd_login() {
    local correo="${1:?Uso: login <correo> <contrasena>}"
    local contrasena="${2:?Uso: login <correo> <contrasena>}"
    local resp
    resp="$(curl -sS --max-time 20 -X POST "$BACKEND_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"correo\":\"$correo\",\"contrasena\":\"$contrasena\"}")"
    local token
    token="$(echo "$resp" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)"
    if [[ -z "$token" ]]; then
        echo "[FALLO] Login rechazado por el backend:" >&2
        echo "$resp" >&2
        exit 1
    fi
    echo "[OK] accessToken:"
    echo "$token"
}

cmd_crear_pago() {
    local id_pedido="${1:?Uso: crear-pago <idPedido> <accessToken>}"
    local token="${2:?Uso: crear-pago <idPedido> <accessToken>}"
    echo "== POST $BACKEND_URL/api/v1/pedidos/$id_pedido/pago =="
    curl -sS --max-time 20 -X POST "$BACKEND_URL/api/v1/pedidos/$id_pedido/pago" \
        -H "Authorization: Bearer $token"
    echo
    echo "Copia 'approvalUrl' de la respuesta y abrela en el navegador:"
    echo "ahi inicias sesion con la cuenta PERSONAL de sandbox (no tu correo real)."
}

cmd_crear_ticket() {
    local id_pedido="${1:?Uso: crear-ticket <idPedido> <idMotivo> <descripcion> <accessToken>}"
    local id_motivo="${2:?Uso: crear-ticket <idPedido> <idMotivo> <descripcion> <accessToken>}"
    local descripcion="${3:?Uso: crear-ticket <idPedido> <idMotivo> <descripcion> <accessToken>}"
    local token="${4:?Uso: crear-ticket <idPedido> <idMotivo> <descripcion> <accessToken>}"
    curl -sS --max-time 20 -X POST "$BACKEND_URL/api/v1/pedidos/$id_pedido/tickets-revision" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{\"idMotivo\":$id_motivo,\"descripcionCliente\":\"$descripcion\"}"
    echo
}

cmd_estado_pago() {
    local id_pedido="${1:?Uso: estado-pago <idPedido> <accessToken>}"
    local token="${2:?Uso: estado-pago <idPedido> <accessToken>}"
    curl -sS --max-time 20 "$BACKEND_URL/api/v1/pedidos/$id_pedido/pago/estado" \
        -H "Authorization: Bearer $token"
    echo
}

cmd_cancelar_pago() {
    local id_pedido="${1:?Uso: cancelar-pago <idPedido> <accessToken> [REEMBOLSAR|LIBERAR]}"
    local token="${2:?Uso: cancelar-pago <idPedido> <accessToken> [REEMBOLSAR|LIBERAR]}"
    local accion="${3:-REEMBOLSAR}"
    curl -sS --max-time 20 -X POST "$BACKEND_URL/api/v1/pedidos/$id_pedido/pago/cancelar" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{\"accionFondos\":\"$accion\",\"motivo\":\"Verificacion REQ-NF-019 contra sandbox real\"}"
    echo
}

cmd_fase0() {
    cat <<'EOF'
== Fase 0: preparar el sandbox (una sola vez, la haces tu) ==
1. developer.paypal.com -> tu cuenta -> Apps & Credentials -> Sandbox ->
   crear/usar una app REST. Copiar Client ID y Secret.
2. Sandbox -> Accounts -> confirmar 1 cuenta Business y >=1 cuenta Personal.
   Menu (...) de la cuenta Personal -> "View/Edit account" para ver su
   correo/clave de prueba (ESA es la que va en el checkout, nunca tu Gmail).
3. Completar en artisync/.env: PAYPAL_CLIENT_ID, PAYPAL_CLIENT_SECRET,
   PAYPAL_MODE=sandbox.
4. Levantar el stack CON el override de desarrollo que publica el 8080
   (docker-compose.yml NO lo publica a proposito -- OBS-AUTO-06/A07 OWASP,
   ver el comentario junto al servicio backend):
     make up-backend-publico
   (o, si no tienes `make`: docker compose -f artisync/docker-compose.yml
    -f artisync/docker-compose.dev.yml --env-file artisync/.env up -d --build backend)
5. ngrok http 8080   -- IMPORTANTE: al backend (8080), NO al frontend (4200).
   El frontend de este compose corre con live-reload (Vite/Angular dev
   server), que por diseno responde 403 "Blocked request... allowedHosts"
   a cualquier Host que no reconozca (asi se comporta su chequeo anti
   DNS-rebinding) -- el dominio publico de ngrok nunca va a estar en esa
   lista, asi que apuntar ahi nunca funciona. Ir directo al backend evita
   esa capa por completo.
6. En la app de PayPal -> Webhooks -> Add Webhook:
     URL: https://<tu-subdominio>.ngrok-free.app/api/webhooks/paypal
     Eventos: CHECKOUT.ORDER.APPROVED, PAYMENT.CAPTURE.COMPLETED
   Copiar el Webhook ID a PAYPAL_WEBHOOK_ID en .env.
7. Si cambias PAYPAL_WEBHOOK_ID despues de que el backend ya arranco,
   reinicia el contenedor backend para que lo relea.

Verificar credenciales:  bash scripts/verificar-paypal-sandbox.sh token
Verificar que el tunel llega al backend real:
  curl -X POST https://<tu-subdominio>.ngrok-free.app/api/webhooks/paypal \
    -H "Content-Type: application/json" -d '{"event_type":"TEST"}'
  -> debe responder 200 "OK" (PayPalWebhookControlador.recibirWebhook)
EOF
}

cmd_fase1() {
    cat <<'EOF'
== Fase 1 (REQ-F-022b): pago real de un ticket de revision ==
1. Con un pedido+contrato ya firmados, genera un ticket de revision con
   costoAdicionalGenerado > limite (dispara la orden PayPal automaticamente).
2. bash scripts/verificar-paypal-sandbox.sh login <correo-cliente> <clave>
3. bash scripts/verificar-paypal-sandbox.sh estado-pago <idPedido> <token>
   (o consulta directamente pagos_ticket_revision.url_aprobacion en BD)
4. Abre esa approvalUrl en el navegador. TU inicias sesion con la cuenta
   Personal de sandbox y apruebas el pago.
5. Revisa el log del backend: debe llegar el POST a /api/webhooks/paypal.
6. bash scripts/verificar-paypal-sandbox.sh orden <orderId>
   -> confirma status COMPLETED y la captura COMPLETED.
7. Confirma en BD: pagos_ticket_revision.estado_pago = 'Pagado'.
EOF
}

cmd_fase2() {
    cat <<'EOF'
== Fase 2 (REQ-F-022c): expiracion a 48h sin esperar 48h ==
1. Repite los pasos 1-2 de la fase 1 pero NO apruebes el pago.
2. Reinicia el backend con overrides SOLO para esta sesion de prueba
   (no los comitees como default nuevo):
     TICKETREVISION_EXPIRACION_HORAS=0
     TICKETREVISION_EXPIRACION_INTERVALO_MS=60000
3. Espera <=1 minuto. Confirma en BD:
     tickets_revision.estado_ticket = 'Rechazado'
     pagos_ticket_revision.estado_pago = 'Expirado'
4. bash scripts/verificar-paypal-sandbox.sh orden <orderId>
   -> si sigue CREATED sin capturar, anotalo como hallazgo en el reporte
   (el codigo actual no la anula en PayPal), no lo declares 'verificado'
   a medias.
EOF
}

cmd_fase3() {
    cat <<'EOF'
== Fase 3 (REQ-NF-019): reconciliacion con webhook retrasado + reembolso real ==
1. Crea un pedido+contrato firmado.
2. bash scripts/verificar-paypal-sandbox.sh login <correo-cliente> <clave>
3. bash scripts/verificar-paypal-sandbox.sh crear-pago <idPedido> <token>
4. ANTES de aprobar: apaga el tunel de ngrok (Ctrl+C o `ngrok` down) para
   que el webhook no pueda llegar.
5. Abre la approvalUrl, inicia sesion con la cuenta Personal y aprueba.
   En PayPal queda APPROVED; en la BD sigue 'Pendiente' (no llego webhook).
6. Reinicia el backend con overrides temporales:
     PAYPAL_RECONCILIACION_UMBRAL_MINUTOS=0
     PAYPAL_RECONCILIACION_INTERVALO_MS=60000
7. Espera <=1 minuto. Confirma pagos_garantia.estado_fondos = 'Retenido'
   SIN que haya llegado ningun webhook (revisa el log para confirmarlo).
8. bash scripts/verificar-paypal-sandbox.sh cancelar-pago <idPedido> <token> REEMBOLSAR
9. bash scripts/verificar-paypal-sandbox.sh orden <orderId>
   -> confirma la captura en status REFUNDED.
10. Confirma pagos_garantia.estado_fondos = 'Reembolsado'.
11. Reactiva ngrok y quita los overrides de entorno usados aqui y en fase2.
EOF
}

case "${1:-}" in
    token)         cmd_token ;;
    orden)         shift; cmd_orden "$@" ;;
    login)         shift; cmd_login "$@" ;;
    crear-ticket)  shift; cmd_crear_ticket "$@" ;;
    crear-pago)    shift; cmd_crear_pago "$@" ;;
    estado-pago)   shift; cmd_estado_pago "$@" ;;
    cancelar-pago) shift; cmd_cancelar_pago "$@" ;;
    fase0)         cmd_fase0 ;;
    fase1)         cmd_fase1 ;;
    fase2)         cmd_fase2 ;;
    fase3)         cmd_fase3 ;;
    *)
        echo "Uso: $0 {fase0|fase1|fase2|fase3|token|orden|login|crear-ticket|crear-pago|estado-pago|cancelar-pago}" >&2
        echo "Ver docs/mediciones/pagos/REPORTE-PAYPAL-SANDBOX.md y el encabezado de este script." >&2
        exit 1
        ;;
esac
