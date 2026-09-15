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
