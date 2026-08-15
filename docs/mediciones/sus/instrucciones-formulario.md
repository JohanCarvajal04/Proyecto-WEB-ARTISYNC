# Instrucciones para la sesión de usabilidad (SUS) — Bloque C.3

Este es el único bloque que depende de personas reales. No se puede automatizar desde aquí:
requiere reclutar participantes y correr las sesiones. Esta guía deja todo listo para que el
equipo solo tenga que ejecutar los pasos.

## 1. Crear el formulario

Crear un Google Form (o equivalente) con estas 10 preguntas exactas — es el cuestionario
original de Brooke, la redacción **no se modifica**, escala Likert 1–5
(1 = Totalmente en desacuerdo, 5 = Totalmente de acuerdo):

1. Creo que me gustaría usar este sistema con frecuencia.
2. Encontré el sistema innecesariamente complejo.
3. Pensé que el sistema era fácil de usar.
4. Creo que necesitaría el apoyo de una persona técnica para poder usar este sistema.
5. Encontré que las diversas funciones de este sistema estaban bien integradas.
6. Pensé que había demasiada inconsistencia en este sistema.
7. Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente.
8. Encontré el sistema muy incómodo de usar.
9. Me sentí muy confiado usando el sistema.
10. Necesité aprender muchas cosas antes de poder usar este sistema.

Configurar el formulario para pedir también:
- **Código de participante** (campo de texto corto) — el equipo se lo asigna antes de empezar
  (`P01`, `P02`, ...), el participante NO escribe su nombre en el formulario.
- Las 10 preguntas como respuesta obligatoria, escala lineal 1–5.

No pedir nombre, correo ni ningún dato identificable en el formulario — el consentimiento
firmado (fuera del repo) es lo único que vincula el código con la identidad real.

## 2. Reclutar participantes

- Mínimo 10, externos al equipo de desarrollo (amigos, familiares, compañeros de otra materia).
- Antes de empezar, cada uno firma [`docs/etica/consentimientos/plantilla.md`](../../etica/consentimientos/plantilla.md)
  impreso o en PDF — ese archivo firmado **no se sube al repositorio**.
- El equipo le asigna el código (`P01`, `P02`, ...) y lo anota en el propio consentimiento.

## 3. Preparar el entorno de prueba

```bash
docker compose up -d --build
```

Verificar que el frontend responda en `http://localhost:4200` y que el backend esté sano. El
backend no publica el 8080 al host (OBS-AUTO-05 / A07 OWASP), así que su estado se consulta a
través del proxy del frontend:

```bash
curl -s http://localhost:4200/actuator/health
```

Comprobar además que exista al menos un servicio publicado en el catálogo para que la tarea sea
completable (ya sembrado — ver `artisync/database/seed-medicion-servicios.sql`).

## 4. Tarea estándar (idéntica para todos los participantes)

1. Registrarse o iniciar sesión.
2. Buscar/explorar el catálogo de servicios.
3. Crear un pedido sobre un servicio.
4. Revisar el estado/seguimiento de ese pedido.
5. Cerrar sesión.

No dar ayuda durante la tarea salvo que el participante esté completamente bloqueado — el
objetivo es medir qué tan usable es el sistema por sí solo, no la eficacia de un instructivo.

## 5. Al terminar la tarea

El participante completa el formulario SUS (paso 1) usando su propio dispositivo o el del
equipo. Anotar la hora de inicio/fin de la tarea (opcional, útil para la ficha metodológica).

## 6. Exportar y analizar

Exportar las respuestas del formulario a CSV con las columnas exactas
`participante,Q1,Q2,Q3,Q4,Q5,Q6,Q7,Q8,Q9,Q10` (una fila por participante) y sobrescribir
[`sus-raw.csv`](sus-raw.csv) (hoy solo tiene el encabezado). **No editar el CSV exportado a
mano** — si un valor está mal, se corrige en el formulario/fuente y se re-exporta.

```bash
python docs/mediciones/sus/analisis-sus.py > docs/mediciones/sus/salida-sus.txt
```

## 7. Redactar el reporte

Completar `docs/mediciones/sus/REPORTE-SUS.md` (plantilla ya creada) con la salida del script:
media, DT, IC 95 %, interpretación de Bangor, y la ficha metodológica (fecha, tarea realizada,
perfil de los participantes, entorno usado — versión del sistema, navegador, dispositivo).
