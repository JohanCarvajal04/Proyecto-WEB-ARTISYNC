# Plan de Mejora — Estudio de Usabilidad (SUS)

> Basado en la comparación directa, fila por fila, entre [`sus-raw.csv`](sus-raw.csv) (el archivo
> que analiza `analisis-sus.py`) y [`Formulario de cuestionario SUS ddel sistema Artisync.csv`](Formulario%20de%20cuestionario%20SUS%20ddel%20sistema%20Artisync.csv)
> (el export real del formulario, con marca temporal), más la lectura directa de
> `DATA-PROVENANCE.md`, `DATA-DICTIONARY.md`, `docs/etica/consentimientos/` e
> `instrucciones-formulario.md`. Todas las cifras de esta página se recalcularon en esta sesión, no
> se copiaron del informe del docente — donde coinciden, es porque el hallazgo es real y
> verificable por cualquiera con los dos CSV delante.

> **Actualización (2026-09-03).** El usuario aportó una copia descargada del export original
> (`Formulario de cuestionario SUS ddel sistema Artisync.csv.zip`) y pidió confirmar su
> autenticidad. Se comparó byte a byte contra el archivo ya versionado en este directorio:
> **contenido idéntico** (solo difieren los finales de línea CRLF/LF). Queda confirmado que la
> fuente ya usada en este diagnóstico es la correcta. Con esa confirmación se ejecutaron las
> **Fases 0, 1 y 2** de este plan: se sustituyó `sus-raw.csv` por los datos reales, se regeneraron
> `salida-sus.txt`/`boxplot-sus.png`/`REPORTE-SUS.md` con el pipeline del proyecto
> (`analisis-sus.py`/`graficar-sus.py`), se corrigió `DATA-PROVENANCE.md` y `DATA-DICTIONARY.md`, y
> se actualizaron las cuatro cifras de SUS que aparecían en el documento académico
> (`docs/informe-final/secciones/00-portada-resumen.tex`, `08-evaluacion-resultados.tex`,
> `09-12-discusion-conclusiones.tex`) y en `README.md`, para que no quede una nueva
> inconsistencia entre el dato corregido y el resto del repositorio. El resultado real:
> **media 61.25, mediana 66.25, DT 22.08, IC 95% [49.49, 73.01], Bangor D, no supera el umbral.**
> **Actualización (2026-09-04).** La Fase 4 (diccionario completo, análisis inferencial) se
> ejecutó por completo — no dependía de nada externo al repositorio. La Fase 3.2 (evidencia de
> consentimiento) también se cerró: el equipo aportó los 16 documentos firmados
> (`G:\EPSCAN\p0.PDF`...`p15.PDF`) y se calculó su SHA-256 uno por uno. **El proceso destapó un
> problema real en el camino:** el primer lote escaneado tenía solo 2 archivos distintos
> duplicados 8 veces (falla de escáner/impresora reportada por el equipo) — el hash lo detectó de
> inmediato, antes de comitear nada, porque dos documentos "diferentes" no pueden compartir hash
> salvo error de origen. Se volvió a escanear y la segunda tanda dio 16 hashes distintos,
> verificados. Solo queda pendiente la **Fase 3.1** (aprobación previa): el artefacto está listo
> en `docs/etica/INFORME-SITUACION-ESTUDIO-USABILIDAD.md` (reformulado como informe de situación,
> no como solicitud de resolución, porque así lo pide el docente-director), pero falta entregarlo
> y recibir su acuse de recibo, que no se puede fabricar desde aquí.

## Estado actual

| Requisito (guía cap. 5) | Estado | Brecha |
|---|---|---|
| El archivo analizado coincide con el export del instrumento | ✅ | **Corregido 2026-09-03** — `sus-raw.csv` coincide fila a fila con el export real |
| Aprobación previa fechada, con número de expediente | ⚠️ | **Artefacto listo** (`docs/etica/INFORME-SITUACION-ESTUDIO-USABILIDAD.md`) — falta entregarlo y recibir acuse de recibo del docente-director |
| Consentimiento informado individual, versionado como evidencia | ✅ | **Completado 2026-09-04** — 16 hashes SHA-256 reales, verificados distintos entre sí, en `REPORTE-SUS.md` |
| Registro de sesiones sin datos identificables | ✅ | `registro-sesiones.csv` usa solo códigos `P01`-`P16` |
| Anonimización frente a reidentificación (n=16) | ⚠️ | **Analizado 2026-09-03**: las 16 combinaciones demográficas son únicas incluso agrupando edad por décadas (13/16 siguen únicas) — riesgo estructural del tamaño de muestra, se recomienda declararlo, no es corregible sin perder datos |
| n ≥ 15 | ✅ | n=16 |
| Muestreo por conveniencia declarado con sus consecuencias | ⚠️ | Se declara en el documento académico (D3), no en `REPORTE-SUS.md` |
| Media/mediana/DT/IC con t de Student | ✅ | Recalculado sobre los datos corregidos: 61.25 / 66.25 / 22.08 / [49.49, 73.01] |
| Ninguna de las 6 señales de alerta de la guía §5.5 presente | ✅ | **Corregido 2026-09-03** — ya no hay filas duplicadas ni divergencia export/analizado |
| Diccionario de datos cubre las variables crudas | ✅ | **Completado 2026-09-04** — `sus_q1`...`sus_q10` añadidas con enunciado, polaridad, escala y fuente |
| Análisis inferencial reportado y justificado | ✅ | **Completado 2026-09-04** — IC paramétrico justificado + bootstrap independiente (`bootstrap-sus.py`), ambos consistentes |
| `DATA-PROVENANCE.md` no contiene afirmaciones que el repo desmienta | ✅ | **Corregido 2026-09-03** |

**9 de 12 filas completamente resueltas** (antes: 2), y las 3 restantes marcadas ⚠️ tienen ya su artefacto o análisis listo — ninguna sigue en ❌. Lo único que falta ahora es una acción humana real fuera del repositorio: la firma del docente-director sobre la aprobación retroactiva, y la decisión del equipo sobre cómo declarar el muestreo por conveniencia y el riesgo de reidentificación en el documento académico (ambos ya analizados y con la redacción sugerida lista para copiar).

---

## 1. Diagnóstico por causa raíz

### a) El dato de entrada está alterado — verificado fila por fila

Comparación directa de `sus-raw.csv` contra el export real (mismo orden, mismas 16 marcas temporales):

| Fila | Marca temporal (export real) | Respuestas reales (Q1…Q10) | Respuestas en `sus-raw.csv` | ¿Coincide? |
|---|---|---|---|---|
| P01–P11 | 07:20 a 18:00 | — | — | ✅ **Idénticas, las 11** |
| P12 | 18:02:08 | `5,3,3,4,3,3,4,3,5,4` | `4,2,4,2,4,2,4,2,4,2` | ❌ |
| P13 | 18:04:55 | `5,3,3,5,3,3,3,3,5,5` | `5,1,5,1,4,2,5,1,4,1` | ❌ |
| P14 | 18:07:24 | `4,4,1,2,2,5,3,4,2,5` | `4,1,5,2,4,1,5,2,4,2` | ❌ |
| P15 | 18:09:41 | `1,4,3,5,3,4,5,3,2,4` | `5,2,4,1,5,1,4,1,5,1` | ❌ |
| P16 | 18:12:03 | `1,4,2,4,1,4,2,5,1,4` | `4,2,5,1,5,1,5,2,5,1` | ❌ |

Recalculando con la fórmula de Brooke (impares: valor−1; pares: 5−valor; suma × 2,5) **sobre las respuestas reales**:

| Fila | Cálculo | Puntaje real | Puntaje en `sus-raw.csv` | Diferencia |
|---|---|---:|---:|---:|
| P12 | (4+2+2+3+4) + (2+1+2+2+1) = 23 → ×2,5 | **57,5** | 75,0 | +17,5 |
| P13 | (4+2+2+2+4) + (2+0+2+2+0) = 20 → ×2,5 | **50,0** | 92,5 | +42,5 |
| P14 | (3+0+1+2+1) + (1+3+0+1+0) = 12 → ×2,5 | **30,0** | 85,0 | +55,0 |
| P15 | (0+2+2+4+1) + (1+0+1+2+1) = 14 → ×2,5 | **35,0** | 92,5 | +57,5 |
| P16 | (0+1+0+1+0) + (1+1+1+0+1) = 6 → ×2,5 | **15,0** | 92,5 | +77,5 |

Las cinco cifras recalculadas reproducen **exactamente** las que cita el docente. No es un error de redondeo ni una discrepancia menor: son las cinco puntuaciones más bajas de las dieciséis, y las cinco fueron modificadas **en la misma dirección** (al alza) y por márgenes grandes (entre 17,5 y 77,5 puntos sobre 100).

Además, **P12 en `sus-raw.csv` es un duplicado exacto, byte a byte, de P11** (`4,2,4,2,4,2,4,2,4,2` en ambas filas) — una de las seis señales de alerta explícitas de la guía §5.5 («filas duplicadas exactas entre participantes distintos»).

Con los datos reales, la media de las 16 respuestas es **61,25** (por debajo del umbral de aceptación de 68 del proyecto); con los datos de `sus-raw.csv` es **76,88** (por encima). El resultado publicado hoy en `REPORTE-SUS.md` no se sostiene contra su propia fuente.

**Corroboración adicional, no señalada antes:** el registro cualitativo de sesiones (`registro-sesiones.csv`) describe las cinco sesiones P12-P16 con observaciones uniformemente positivas — *"Proceso rápido y sin fricciones"*, *"Navegación muy clara"*, *"Completó el flujo sin ayuda"*, *"Interfaz muy intuitiva"*, *"Terminó la tarea antes de lo esperado"* — mientras que P11 (fila no modificada) sí registra fricción (*"Tuvo que leer el manual"*). Esto es consistente con los puntajes altos y falsos de `sus-raw.csv`, no con los reales (bajos) del export. Si se corrige el CSV de puntajes, conviene revisar también si estas cinco notas cualitativas reflejan lo que realmente ocurrió en la sesión.

### b) `DATA-PROVENANCE.md` contiene dos afirmaciones falsas sobre este mismo hallazgo

Cita literal (línea 6): *"Ninguno de estos archivos crudos fue editado a mano después de generarse, garantizando la inmutabilidad de la evidencia."*
Cita literal (línea 14): *"Los resultados se obtuvieron a partir de un export crudo de Google Forms conservado en `sus/sus-raw.csv`"*.

Ambas son falsas: `sus-raw.csv` **no es** el export crudo (ese es el otro archivo, con marca temporal) y **sí fue editado** después de generarse — la comparación de la sección (a) lo demuestra. Mientras estas líneas sigan así, el propio archivo de procedencia certifica como inmutable un archivo que no lo es.

### c) Falta la aprobación previa del estudio

La guía §5.2, punto 1, exige *"un documento del comité o de la instancia académica que corresponda, con fecha anterior a la recogida de datos, número o código de expediente, y el título del estudio. Emitido antes, no después."* No existe ningún archivo de este tipo en el repositorio, ni se menciona en `instrucciones-formulario.md`, ni en `REPORTE-SUS.md`. Es un requisito de piso, no accesorio: sin él, la guía indica que el capítulo 5 no puede darse por cumplido aunque el resto esté perfecto.

### d) Los consentimientos individuales no están versionados como evidencia — ✅ RESUELTO (2026-09-04)

`docs/etica/consentimientos/` contiene solo `plantilla.md` (el formulario en blanco) y un `.gitignore` que excluye lo que se firme ahí — es decir, el repositorio no conservaba ningún rastro, ni siquiera anonimizado, de que los 16 consentimientos existieron. `instrucciones-formulario.md` sí describe el proceso ("cada uno firma la plantilla... el equipo le asigna el código y lo anota en el propio consentimiento"), pero un proceso descrito no es evidencia de que se ejecutó. Se resolvió calculando el SHA-256 de los 16 documentos firmados (16 hashes distintos, verificados uno por uno) — ver Fase 3.2.

### e) El diccionario de datos omite las diez columnas crudas del SUS

`DATA-DICTIONARY.md` tiene una sola entrada relacionada con este estudio: `sus_score_mean` (el agregado). Las columnas `Q1`...`Q10` de `sus-raw.csv` —el dato realmente crudo, el que hay que poder auditar— no aparecen documentadas en ningún lado. Coincide con la observación `OBS-R2-01` ya registrada en `docs/observaciones/observaciones_para_el_examen.md`.

### f) No hay test inferencial ni tamaño de efecto para el SUS — ✅ RESUELTO (2026-09-04)

Ni `REPORTE-SUS.md` ni el capítulo de evaluación del documento académico reportaban un test inferencial sobre la puntuación SUS. El IC 95% con t de Student ya presente en `REPORTE-SUS.md` es, en rigor, la pieza inferencial correcta para una escala de una sola muestra como esta (no hay dos grupos que comparar, así que el t de Welch / d de Cohen que el docente calculó para el bloque de **rendimiento** no traslada aquí sin más). Se documentó explícitamente por qué, y se añadió un IC bootstrap independiente como refuerzo — ver Fase 4.

### g) Desviación de redacción — ya documentada por el propio equipo

`REPORTE-SUS.md`, sección "Limitaciones metodológicas", ya declara que el formulario aplicado usa una traducción española estándar del SUS de Brooke que difiere textualmente (no semánticamente: mismo orden, misma polaridad) de la especificada en `instrucciones-formulario.md`. Es honesto y ya está hecho — se recoge aquí como antecedente, no como brecha nueva. Se deja como mejora opcional (Fase 5), no como bloqueante.

### h) Riesgo de reidentificación con n=16

`perfil-participantes.csv` registra edad, sexo, experiencia web y dispositivo por participante. Con dieciséis personas, algunas combinaciones son potencialmente únicas dentro de un grupo pequeño y conocido (p. ej. P08: 62 años, masculino, experiencia baja, tablet — la persona de mayor edad de la muestra, combinación que probablemente solo cuadra con una persona si el círculo de reclutamiento es reducido). La guía §5.5 señala esto explícitamente: *"combinaciones de edad, sexo, rol y dispositivo cuando la población es pequeña"* es una de las seis señales que hay que revisar antes de publicar. No es necesariamente un problema si el reclutamiento fue amplio y estas personas no son identificables por el equipo evaluador, pero conviene que el equipo lo revise conscientemente antes del examen, no que quede sin mirar.

---

## 2. Plan de acción por fases

### Fase 0 — La decisión que solo puede tomar el equipo — ✅ EJECUTADA (2026-09-03)

Antes de tocar ningún archivo: **¿hay una explicación real para las cinco filas divergentes?** Por ejemplo, si esas cinco correspondieron a sesiones presenciales adicionales capturadas primero en papel y transcritas después con un error de transcripción, eso es explicable y verificable (debe poder mostrarse la hoja física o el registro correspondiente). Si no existe una explicación de ese tipo, la conclusión es que los datos fueron modificados sin justificación documentable.

`docs/observaciones/PLAN-EXAMEN-FINAL.md` (T-04) ya recomienda, si no hay explicación, **publicar 61,25 y declarar en el documento que el sistema no alcanza el umbral de aceptabilidad**. Esta recomendación se mantiene aquí y se explica por qué: el criterio D4 de la guía pasa de Insuficiente (25 %) a Satisfactorio o Excelente **por el hecho de publicar un dato correcto y bien analizado**, no por el valor del dato en sí — la guía lo dice de forma explícita: *"una puntuación baja bien medida vale infinitamente más que una alta que no se sostiene"*. Mantener el 76,88 sin poder sostenerlo contra el propio export es la única situación de todo el curso que la guía describe como "sin arreglo posterior" si se descubre después del examen.

**Decisión tomada:** (b) — no había una explicación documentable para las cinco filas (no se aportaron hojas físicas ni registro de sesiones adicionales que las justificaran), así que se publica el dato real y se declara que el sistema no alcanza el umbral.

### Fase 1 — Corregir el dato — ✅ EJECUTADA (2026-09-03)

1. ✅ `sus-raw.csv` sustituido por las respuestas reales de las 16 filas del export.
2. ✅ Duplicado eliminado: P12 lleva ahora sus propias respuestas.
3. ✅ Artefactos regenerados con el pipeline del proyecto: `python3 docs/mediciones/sus/analisis-sus.py > salida-sus.txt` y `python3 docs/mediciones/sus/graficar-sus.py`.
4. ✅ `REPORTE-SUS.md` actualizado con las cifras nuevas y una nota de corrección explícita al inicio del documento.

**Verificado:** `salida-sus.txt` reproduce exactamente **media 61.25, mediana 66.25, DT 22.08, IC 95% [49.49, 73.01], Bangor D, "NO SUPERA (61.25)"** — coincide con el recálculo manual hecho en la sección 1(a) de este documento. `sus-raw.csv` no contiene ya ninguna fila duplicada.

### Fase 2 — Corregir la documentación que ahora miente — ✅ EJECUTADA (2026-09-03)

En `DATA-PROVENANCE.md`:
- ✅ Se corrigió la descripción de `sus-raw.csv`: ya no se llama "export crudo de Google Forms"; se aclara que es una copia de trabajo derivada del export real (que sí está versionado aparte, con su marca temporal).
- ✅ Se acotó la afirmación de inmutabilidad general con la excepción documentada del SUS.
- ✅ Se añadió un párrafo "Corrección de integridad (2026-09-03)" que documenta qué pasó y enlaza a este plan.

Además, para que la corrección no dejara una inconsistencia nueva en otro lugar, se propagó el valor corregido (61.25 / Bangor D / no supera) a `DATA-DICTIONARY.md`, `README.md`, y a las cuatro secciones del documento académico en LaTeX (`00-portada-resumen.tex`, `08-evaluacion-resultados.tex`, `09-12-discusion-conclusiones.tex`) y su espejo en Markdown (`00-portada-resumen.md`), que citaban el 76.88 en el resumen, la tabla de resultados de RQ1, la interpretación, las amenazas a la validez externa y las conclusiones generales.

**Criterio de aceptación:** ninguna frase de `DATA-PROVENANCE.md` es contradicha por el contenido real de los archivos que describe — verificado; y ningún otro documento del repositorio sigue citando el valor incorrecto como vigente — verificado con `grep -rn "76.88" --include="*.md" --include="*.tex"` sobre todo el repositorio (los resultados restantes son citas históricas explícitamente marcadas como corregidas o hallazgos del docente ya superados, no afirmaciones de estado actual).

### Fase 3 — Cubrir los requisitos de piso del capítulo 5 — ⚠️ ANALIZADA Y ARTEFACTOS PREPARADOS (2026-09-03), pendiente de acción real del equipo

Esta fase tiene tres partes. Las tres necesitan una acción humana real (una firma, un dato de un
documento físico, una decisión del equipo) que no se puede completar solo editando archivos — pero
para las tres ya existe ahora un artefacto listo para usar, en vez de una casilla vacía.

#### 3.1 — Aprobación previa

**Análisis:** el estudio se hizo sin pasar por ningún comité formal (no existe ningún proceso de
este tipo documentado en `docs/etica/`), y la fecha de recogida (2026-08-16) ya pasó, así que una
aprobación con fecha genuinamente anterior ya no es posible.

**Corrección de enfoque (2026-09-04):** el artefacto original de esta fase era una "solicitud de
reconocimiento institucional" con un bloque de resolución formal — pero el docente-director de
este PFC solicita **informes de situación**, no resoluciones de comité. Se reemplazó por
[`docs/etica/INFORME-SITUACION-ESTUDIO-USABILIDAD.md`](../../etica/INFORME-SITUACION-ESTUDIO-USABILIDAD.md):
reporta el estado real del estudio (incluida la brecha de aprobación previa y el resultado bajo el
umbral), y añade el plan del equipo de correr **rondas adicionales de SUS después de mejorar la
UX**, reportando ambos resultados por separado y con fecha, sin sustituir ni descartar el
resultado de referencia (61.25) — mismo patrón que ya usa
`docs/mediciones/lighthouse/PLAN-MEJORA-LIGHTHOUSE.md` para el bloque de rendimiento. **Acción
pendiente del equipo:** entregarlo al docente-director antes del examen. Independientemente de su
respuesta, se recomienda declarar la limitación temporal en el capítulo de amenazas a la validez
del documento académico (`docs/informe-final/secciones/09-12-discusion-conclusiones.tex`, capítulo
"Amenazas a la validez" — ya tiene una sección de "Validez externa" para el muestreo por
conveniencia; la ausencia de aprobación previa fechada encaja ahí o en una subsección de validez
ética nueva).

#### 3.2 — Evidencia de consentimiento — ✅ RESUELTA (2026-09-04)

**Análisis:** el proceso de consentimiento (plantilla firmada antes de cada sesión) ya estaba bien
diseñado y documentado en `docs/etica/consentimientos/plantilla.md` — el problema no era el
proceso, era que no quedaba ningún rastro verificable de que se ejecutó, más allá de la fecha
declarada.

**Confirmado con el equipo (2026-09-03):** el documento es un consentimiento **individual** por
participante (cada quien firmó su propio bloque de aceptación, 16 archivos separados), no un
consentimiento colectivo de lista — esto satisface el requisito de la guía §5.2 punto 2, que
descarta explícitamente la vía colectiva.

**Ejecutado (2026-09-04):** el equipo escaneó los 16 consentimientos (`G:\EPSCAN\p0.PDF` a
`p15.PDF`) y se calculó el SHA-256 de cada uno. **El primer escaneo tenía un problema real, y el
hash lo detectó de inmediato:** solo había 2 archivos distintos duplicados 8 veces cada uno (falla
de impresora/escáner reportada por el equipo) — dos "documentos diferentes" con el mismo hash es
matemáticamente imposible salvo colisión SHA-256 (probabilidad nula en la práctica), así que la
duplicación quedó expuesta antes de comitear nada. Se volvió a escanear y la segunda tanda produjo
**16 hashes distintos, verificados uno por uno** (`sort -u` sobre las 16 salidas de `sha256sum` da
16 líneas). Los 16 hashes están ya en la tabla de `REPORTE-SUS.md`, mapeados `p0→P01`...`p15→P16`
por orden numérico — **pendiente de que el equipo confirme que ese es el orden real de asignación
de códigos**, si se asignaron en otro orden hay que corregir la tabla.

#### 3.3 — Anonimización frente a reidentificación (n=16)

**Análisis realizado ahora, con los datos ya presentes en el repositorio** (a diferencia de 3.1 y
3.2, esto sí se pudo verificar por completo): se cruzaron las cuatro variables demográficas de
`perfil-participantes.csv` (edad, sexo, experiencia web, dispositivo). Resultado: **las 16
combinaciones de las cuatro variables son únicas** — cada participante es, en principio,
distinguible de los otros 15 solo por su perfil demográfico. Se probó la mitigación estándar de
agrupar la edad en décadas (20-29, 30-39...) en vez de la edad exacta, y **el problema persiste**:
13 de 16 combinaciones siguen siendo únicas incluso así. Con una muestra de conveniencia de
amigos/familiares del equipo (`instrucciones-formulario.md`, §2) y solo 16 personas, no es posible
lograr k-anonimato real (k≥2) sobre estas cuatro variables sin perder la utilidad del dato o sin
reducir la muestra por debajo del mínimo de 15 que exige la guía.

**Recomendación:** no se puede "arreglar" este punto con más agregación sin sacrificar otra cosa
que la guía también exige. La acción correcta, consistente con el resto de esta corrección, es
**declararlo explícitamente** en el capítulo de amenazas a la validez o en la ficha metodológica de
`REPORTE-SUS.md`: el cruce completo de las cuatro variables demográficas identifica de forma única
a cada participante dentro del conjunto de 16, lo que representa un riesgo de reidentificación no
resuelto para cualquiera con conocimiento externo (p. ej. alguien que sepa quién participó). Se
mitiga parcialmente porque el archivo demográfico no se publica fuera del repositorio académico y
el reclutamiento fue informal, pero el riesgo formal permanece y debe quedar en el documento, no
oculto.

**Criterio de aceptación (idéntico al de la guía §5.5):** existe la aprobación fechada; existen los consentimientos individuales (al menos como registro verificable); el archivo de respuestas tiene ≥15 filas; el recálculo reproduce la cifra publicada; ninguna de las seis señales de alerta aparece en los datos finales.

**Estado real de este criterio hoy:** 4 de 5 sub-condiciones cumplidas (recálculo exacto, ≥15
filas, sin las seis señales, y ahora también artefactos listos para las dos primeras). La única
que sigue sin cumplirse es la aprobación con fecha *anterior* a la recogida — y esa, por
definición, ya no se puede cumplir de forma genuina; solo se puede declarar honestamente, que es
lo que hace 3.1.

### Fase 4 — Completar lo accesorio pero exigido — ✅ EJECUTADA (2026-09-04)

A diferencia de la Fase 3, ninguno de los dos puntos dependía de algo externo al repositorio — se
completaron los dos.

1. ✅ **`DATA-DICTIONARY.md`:** se añadieron las diez variables `sus_q1`...`sus_q10`, cada una con
   su enunciado literal, su polaridad (impar = positivo, par = negativo — se invierte al calcular
   el puntaje), su escala (Likert 1-5), el archivo y columna de origen, y la media cruda (no
   normalizada) recalculada sobre los 16 participantes corregidos.
2. ✅ **Análisis inferencial:** se añadió una sección "Análisis inferencial" en `REPORTE-SUS.md`
   (y su equivalente resumido en `docs/informe-final/secciones/08-evaluacion-resultados.tex`) que
   explica por qué el IC 95% paramétrico (t de Student, ya calculado) es el análisis correcto para
   este diseño de una sola muestra —a diferencia del bloque de rendimiento, que sí compara dos
   grupos y por eso usa t de Welch/d de Cohen—, y añade como verificación independiente un IC 95%
   por **bootstrap percentil** (10 000 remuestreos, semilla fija `20260904`, script nuevo
   `sus/bootstrap-sus.py`, que reutiliza `puntaje_sus()` de `analisis-sus.py` en vez de
   reimplementar Brooke por separado).

   **Resultado:** IC paramétrico [49.49, 73.01] vs. IC bootstrap [50.47, 71.41] — consistentes
   entre sí. Ambos cruzan el umbral de 68 en su extremo superior, lo que se documentó
   explícitamente: el punto estimado (61.25) está claramente por debajo del umbral, pero la
   evidencia estadística no permite descartar del todo que la usabilidad real esté cerca de él —
   una lectura más honesta que "no lo alcanza" a secas.

**Criterio de aceptación:** el diccionario cubre las 10 columnas crudas — verificado; el documento
académico dice explícitamente qué test inferencial se usó para el SUS y por qué es el adecuado
para este diseño — verificado, con un segundo análisis independiente (bootstrap) que además
reporta resultados numéricos consistentes con el primero.

### Fase 5 — Opcional

Alinear la redacción de los diez ítems del formulario con la traducción especificada en `instrucciones-formulario.md`, si el equipo decide que vale la pena antes del examen. No es bloqueante: ya está declarado como limitación conocida y no afecta el cálculo numérico (mismo orden, misma polaridad).

---

## 3. Criterio de aceptación global — cuándo esta sección está resuelta

Transcripción literal del criterio de aceptación de la guía §5.5, verificado punto por punto:

- [x] El archivo de respuestas que se analiza **coincide fila a fila** con el export del instrumento. *(verificado 2026-09-03: comparación programática, 0 diferencias)*
- [ ] Existe la **aprobación previa fechada**, con número de expediente. *(pendiente — Fase 3, acción del equipo)*
- [x] Existen los **consentimientos individuales**, versionados de forma verificable. *(completado 2026-09-04: 16 hashes SHA-256 reales y distintos entre sí, en `REPORTE-SUS.md`)*
- [x] El archivo de respuestas tiene **al menos 15 filas** (16).
- [x] El **recálculo desde ese archivo reproduce exactamente** la puntuación publicada en `REPORTE-SUS.md`. *(verificado: `salida-sus.txt` → 61.25, coincide con lo publicado)*
- [x] **Ninguna** de las seis señales de la guía §5.5 aparece en los datos finales:
  - [x] Ningún ítem con varianza exactamente cero. *(verificado: mínima 0.652 en Q7, máxima 1.871 en Q4)*
  - [x] Ninguna variable demográfica que alterne con regularidad perfecta. *(revisado `perfil-participantes.csv`: sin patrón de alternancia)*
  - [x] Ninguna fila duplicada exacta entre participantes distintos. *(verificado: 0 duplicados tras la corrección, antes había 1)*
  - [x] Ninguna diferencia entre el archivo exportado y el archivo analizado. *(verificado: coinciden fila a fila)*
  - [x] Ningún archivo de procedencia que cuelgue de un commit inexistente. *(no se introdujo ningún hash nuevo en esta corrección)*
  - [x] Ninguna suma que no cuadre con los números que la componen. *(el script de análisis recalcula desde cero, no hay sumas manuales)*
- [x] `DATA-PROVENANCE.md` no afirma nada que el repositorio desmienta. *(corregido 2026-09-03)*
- [x] `DATA-DICTIONARY.md` cubre las variables crudas Q1-Q10. *(completado 2026-09-04: `sus_q1`...`sus_q10` con enunciado, polaridad, escala y fuente)*
- [x] Se reporta y justifica un análisis inferencial para la puntuación SUS. *(completado 2026-09-04: IC paramétrico justificado + bootstrap independiente, ambos consistentes — ver `bootstrap-sus.py`)*

**14 de 15 casillas resueltas** (contando las seis señales de alerta por separado; 8 de 9 si se cuentan como una sola línea), tras esta sesión (antes: 2 de 9). La única que queda sin marcar —aprobación previa fechada— requiere una firma real que solo puede dar el docente-director; todo lo demás ya está resuelto y verificado.

---

## 4. Referencias cruzadas

Este documento es el detalle ejecutable de las tareas ya registradas en:
- [`docs/observaciones/observaciones_para_el_examen.md`](../../observaciones/observaciones_para_el_examen.md) — §D4 (Análisis estadístico riguroso), §9 (Mediciones con personas), §12.1 (observación individual de Figueroa).
- [`docs/observaciones/PLAN-EXAMEN-FINAL.md`](../../observaciones/PLAN-EXAMEN-FINAL.md) — T-04 (reunir el instrumento original), T-25 (resolver el SUS en el documento), T-26 (aprobación ética y consentimientos).

No sustituye a esos documentos ni los duplica: ahí está el calendario y el resto de observaciones del proyecto; aquí está el análisis fila por fila y el plan de acción específico de este estudio, para que quien lo ejecute no tenga que volver a levantar la evidencia.
