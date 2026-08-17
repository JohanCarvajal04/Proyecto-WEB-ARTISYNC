# Reporte de Usabilidad — System Usability Scale (SUS)

## Ficha metodológica

- Fecha de las sesiones: 2026-08-16
- Commit del sistema evaluado: `1b34b8d`
- Número de participantes: 16 (externos al equipo)
- Perfil demográfico: Rango de edad: 21-62 años (M = 34.6). Sexo: 50% Femenino, 50% Masculino. Experiencia web: 50.0% Alta, 31.3% Media, 18.8% Baja. Dispositivo: 37.5% Móvil, 25.0% Laptop, 25.0% Desktop, 12.5% Tablet. (Ver detalle en `docs/mediciones/sus/perfil-participantes.csv`).
- Entorno de prueba: `docker compose up -d --build`
- Tarea realizada: registro/login → explorar catálogo → crear pedido → revisar seguimiento →
  cerrar sesión (ver §4 de `instrucciones-formulario.md`)

## Resultados

| Métrica | Valor |
|---|---|
| n participantes | 16 |
| Media SUS | 76.88 |
| Mediana SUS | 77.50 |
| Desviación típica | 14.48 |
| IC 95% | [69.16, 84.59] |
| Interpretación (escala de Bangor) | B |
| Umbral del proyecto (> 68) | **Supera** (76.88) |

### Diagrama de Caja (Boxplot) de Puntuaciones
![Diagrama de caja de puntuaciones SUS](/d:/Proyecto/Proyecto-WEB-ARTISYNC/docs/mediciones/sus/boxplot-sus.png)

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
| P11 | 75.00 |
| P12 | 75.00 |
| P13 | 92.50 |
| P14 | 85.00 |
| P15 | 92.50 |
| P16 | 92.50 |

### Media normalizada por ítem (contribución 0–4, tras invertir los ítems pares)

| Ítem | Enunciado (según formulario aplicado) | Media |
|---|---|---|
| Q1 | Creo que me gustará usar este sistema con frecuencia. | 3.12 |
| Q2 | Encontré el sistema innecesariamente complejo. | 3.00 |
| Q3 | Me pareció que el sistema era fácil de usar. | 3.25 |
| Q4 | Creo que necesitaría el apoyo de una persona técnica para poder usar este sistema. | 3.25 |
| Q5 | Encontré que las diversas funciones de este sistema estaban bastante bien integradas. | 3.12 |
| Q6 | Me pareció que había demasiada inconsistencia en este sistema. | 3.06 |
| Q7 | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente. | 3.19 |
| Q8 | Encontré el sistema muy engorroso o difícil de usar. | 2.94 |
| Q9 | Me sentí muy confiado/a al usar el sistema. | **2.81 (peor puntaje)** |
| Q10 | Necesité aprender muchas cosas antes de poder empezar a usar este sistema. | 3.00 |

## Interpretación cualitativa

El ítem con peor puntaje normalizado vuelve a ser **Q9 — "Me sentí muy confiado/a al usar el sistema"**
(2.81/4), seguido de Q8 y Q10 (3.00/4). Esto sugiere que, en general, el sistema es muy utilizable y 
los participantes pudieron completar las tareas sin mayores fricciones (como lo evidencian las observaciones
en `registro-sesiones.csv`), pero aún puede haber una ligera falta de confianza al finalizar la tarea 
(posiblemente por ser un flujo de comercio electrónico nuevo). A nivel global, el sistema supera 
ampliamente el umbral esperado (76.88 vs 68) y obtiene una calificación de "B".

Se registraron observaciones cualitativas en [`registro-sesiones.csv`](registro-sesiones.csv), 
incluyendo la duración de las tareas y puntos de fricción, lo cual es útil para cruzar con 
los bajos puntajes de usabilidad obtenidos.

## Limitaciones metodológicas

- **Redacción del cuestionario.** El formulario aplicado usa una traducción española estándar del
  SUS de Brooke, pero **no es literalmente la redacción especificada** en
  [`instrucciones-formulario.md`](instrucciones-formulario.md#1-crear-el-formulario) (p. ej. "Me
  pareció que el sistema era fácil de usar" en vez de "Pensé que el sistema era fácil de usar").
  El orden de los 10 ítems y su polaridad (impares en positivo, pares en negativo) coinciden
  exactamente con el original, por lo que el cálculo numérico no se ve afectado — pero se deja
  constancia de la desviación textual en vez de reportarla como si fuera literal.

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
| P11 | 2026-08-16 |
| P12 | 2026-08-16 |
| P13 | 2026-08-16 |
| P14 | 2026-08-16 |
| P15 | 2026-08-16 |
| P16 | 2026-08-16 |
