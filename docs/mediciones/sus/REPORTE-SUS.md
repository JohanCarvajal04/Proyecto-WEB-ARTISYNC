# Reporte de Usabilidad — System Usability Scale (SUS)

## Ficha metodológica

- Fecha de las sesiones: 2026-08-16
- Commit del sistema evaluado: `1b34b8d`
- Número de participantes: 10 (externos al equipo)
- Perfil de los participantes: ver `docs/etica/consentimientos/` (referenciados solo por código;
  no se registra perfil demográfico adicional en el repositorio)
- Entorno de prueba: `docker compose up -d --build` — navegador/dispositivo usado por los
  participantes: no registrado en esta ronda (ver limitación metodológica más abajo)
- Tarea realizada: registro/login → explorar catálogo → crear pedido → revisar seguimiento →
  cerrar sesión (ver §4 de `instrucciones-formulario.md`)

## Resultados

| Métrica | Valor |
|---|---|
| n participantes | 10 |
| Media SUS | 71.75 |
| Mediana SUS | 68.75 |
| Desviación típica | 15.19 |
| IC 95% | [60.89, 82.61] |
| Interpretación (escala de Bangor) | C+ |
| Umbral del proyecto (> 68) | **Supera** (71.75) |

Salida completa del script: ver [`salida-sus.txt`](salida-sus.txt).

### Puntajes individuales

| Código | Puntaje SUS |
|---|---|
| P01 | 82.50 |
| P02 | 92.50 |
| P03 | 92.50 |
| P04 | 65.00 |
| P05 | 67.50 |
| P06 | 67.50 |
| P07 | 70.00 |
| P08 | 50.00 |
| P09 | 80.00 |
| P10 | 50.00 |

### Media normalizada por ítem (contribución 0–4, tras invertir los ítems pares)

| Ítem | Enunciado (según formulario aplicado) | Media |
|---|---|---|
| Q1 | Creo que me gustará usar este sistema con frecuencia. | 3.00 |
| Q2 | Encontré el sistema innecesariamente complejo. | 2.80 |
| Q3 | Me pareció que el sistema era fácil de usar. | 3.10 |
| Q4 | Creo que necesitaría el apoyo de una persona técnica para poder usar este sistema. | 3.10 |
| Q5 | Encontré que las diversas funciones de este sistema estaban bastante bien integradas. | 3.00 |
| Q6 | Me pareció que había demasiada inconsistencia en este sistema. | 2.80 |
| Q7 | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente. | 3.00 |
| Q8 | Encontré el sistema muy engorroso o difícil de usar. | 2.70 |
| Q9 | Me sentí muy confiado/a al usar el sistema. | **2.50 (peor puntaje)** |
| Q10 | Necesité aprender muchas cosas antes de poder empezar a usar este sistema. | 2.70 |

## Interpretación cualitativa

El ítem con peor puntaje normalizado es **Q9 — "Me sentí muy confiado/a al usar el sistema"**
(2.50/4), seguido de Q8 y Q10 (2.70/4). Esto sugiere que, aunque los participantes no encontraron
el sistema complejo ni inconsistente (Q2 y Q6 puntúan relativamente bien), no terminaron la tarea
con una sensación fuerte de confianza — un patrón típico de sistemas nuevos sin fricciones graves
de UI, pero con curva de aprendizaje perceptible en el flujo evaluado (registro/login → catálogo →
pedido → seguimiento).

**No se registraron observaciones cualitativas de sesión** (duración, si algún participante quedó
bloqueado en algún paso, notas del facilitador): `registro-sesiones.csv` no se completó durante
las 10 sesiones, solo se pudo reconstruir el código de participante y la marca temporal a partir
del export de Google Forms. Se deja la estructura en
[`registro-sesiones.csv`](registro-sesiones.csv) para que la próxima ronda sí capture esta
información en el momento, en vez de perderla.

## Limitaciones metodológicas

- **Redacción del cuestionario.** El formulario aplicado usa una traducción española estándar del
  SUS de Brooke, pero **no es literalmente la redacción especificada** en
  [`instrucciones-formulario.md`](instrucciones-formulario.md#1-crear-el-formulario) (p. ej. "Me
  pareció que el sistema era fácil de usar" en vez de "Pensé que el sistema era fácil de usar").
  El orden de los 10 ítems y su polaridad (impares en positivo, pares en negativo) coinciden
  exactamente con el original, por lo que el cálculo numérico no se ve afectado — pero se deja
  constancia de la desviación textual en vez de reportarla como si fuera literal.
- **Perfil y entorno no registrados.** No hay constancia versionada del navegador/dispositivo
  usado por cada participante ni de su perfil, más allá de lo que exige el consentimiento
  informado.

## Referencias de consentimiento

Participantes referenciados solo por código (consentimientos firmados fuera del repositorio,
ver `docs/etica/consentimientos/`):

| Código | Fecha |
|---|---|
| P01 | 2026-08-16 |
| P02 | 2026-08-16 |
| P03 | 2026-08-16 |
| P04 | 2026-08-16 |
| P05 | 2026-08-16 |
| P06 | 2026-08-16 |
| P07 | 2026-08-16 |
| P08 | 2026-08-16 |
| P09 | 2026-08-16 |
| P10 | 2026-08-16 |
