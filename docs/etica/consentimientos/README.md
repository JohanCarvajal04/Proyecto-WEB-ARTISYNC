# Registro de consentimientos — prueba de usabilidad SUS

`registro-consentimientos.csv` es la constancia anonimizada de que cada participante (P01–P16,
mismos códigos que `docs/mediciones/sus/sus-raw.csv`) aceptó la plantilla de consentimiento antes
de responder el cuestionario. No contiene nombres, correos ni ninguna otra columna que permita
reidentificar a alguien — eso es justo lo que este archivo debe seguir siendo cierto en cada
edición.

## Columnas

| Columna | Qué va aquí |
|---|---|
| `codigo_participante` | Ya está poblado (P01–P16); no editar. |
| `fecha_consentimiento` | Fecha en que esa persona aceptó, formato `AAAA-MM-DD`. |
| `medio` | Cómo se recogió la aceptación: `presencial`, `formulario`, `whatsapp`, etc. |
| `acepta` | `si` o `no`. Una fila con `no` significa que esa persona respondió el SUS sin haber aceptado — repórtalo en el documento igual, no la borres. |

## Si no se completan las 16 filas antes del cierre

No dejes el archivo a medias en silencio: añade una línea al final del CSV (o una nota en este
README) indicando cuántas de las 16 sí quedaron con constancia y cuántas no, y por qué. Es
preferible declarar "11 de 16 con constancia, 5 sin respuesta del participante" que dejar el
archivo incompleto sin explicación — la guía del examen suspenso trata como falta grave un dato
inventado, no una brecha admitida.

## Procedencia de las 16 filas (transcritas, no recolectadas de nuevo)

**Añadido 2026-09-14, revisión externa (Claude Code).** Las 16 filas de
`registro-consentimientos.csv` no se recolectaron en esta sesión: se transcribieron de la tabla
"Referencias de consentimiento" que ya existía en
[`docs/mediciones/sus/REPORTE-SUS.md`](../../mediciones/sus/REPORTE-SUS.md#referencias-de-consentimiento)
(commit `06d1a364`, 2026-09-04, Bryan Figueroa), donde cada participante tiene fecha
(`2026-08-16` para los 16) y el hash SHA-256 de su consentimiento firmado y escaneado
(`G:\EPSCAN\p0.PDF`–`p15.PDF`, en el equipo, fuera del repositorio). `medio` se infiere como
`presencial` porque esa misma sección describe consentimiento individual "firmado antes de cada
sesión"; `acepta` se marca `si` en los 16 porque los 16 tienen hash registrado y ninguno aparece
como rechazo.

**Advertencia — esto no está verificado de forma independiente.** Nadie en esta revisión tuvo
acceso a `G:\EPSCAN\` para recalcular los 16 SHA-256 contra los PDF reales y confirmar que
correspondan. Antes de defender esto como constancia cerrada, quien tenga esos archivos localmente
debería volver a correr `sha256sum p0.PDF ... p15.PDF` y confirmar que coinciden con la tabla de
`REPORTE-SUS.md` — esa orden y su salida son las que deberían entrar al expediente de verificación
(`VERIFICACION.md`) para que P14 pase de "transcrito" a "comprobado".
