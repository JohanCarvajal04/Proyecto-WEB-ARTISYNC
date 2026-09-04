# Reporte de Usabilidad — System Usability Scale (SUS)

> **Corrección de integridad de datos (2026-09-03).** La versión anterior de este reporte
> (media 76.88, "Supera") se calculó sobre un `sus-raw.csv` cuyas filas P12–P16 no coincidían con
> el export real del formulario: fueron alteradas al alza, y P12 era además un duplicado exacto de
> P11. Se corrigió `sus-raw.csv` para que coincida fila a fila con el export real
> (`Formulario de cuestionario SUS ddel sistema Artisync.csv`) y se regeneraron con
> `analisis-sus.py`/`graficar-sus.py` todas las cifras de este documento. El resultado real es
> **61.25 — por debajo del umbral de aceptación del proyecto (68)**. Diagnóstico completo, tabla
> fila por fila del error y plan para resolver las brechas restantes del estudio (aprobación
> previa, evidencia de consentimiento, diccionario de datos) en
> [`PLAN-MEJORA-SUS.md`](PLAN-MEJORA-SUS.md).

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
| Media SUS | 61.25 |
| Mediana SUS | 66.25 |
| Desviación típica | 22.08 |
| IC 95% | [49.49, 73.01] |
| Interpretación (escala de Bangor) | D |
| Umbral del proyecto (> 68) | **No supera** (61.25) |

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
| P12 | 57.50 |
| P13 | 50.00 |
| P14 | 30.00 |
| P15 | 35.00 |
| P16 | 15.00 |

### Media normalizada por ítem (contribución 0–4, tras invertir los ítems pares)

| Ítem | Enunciado (según formulario aplicado) | Media |
|---|---|---|
| Q1 | Creo que me gustará usar este sistema con frecuencia. | 2.75 |
| Q2 | Encontré el sistema innecesariamente complejo. | 2.38 |
| Q3 | Me pareció que el sistema era fácil de usar. | 2.56 |
| Q4 | Creo que necesitaría el apoyo de una persona técnica para poder usar este sistema. | 2.44 |
| Q5 | Encontré que las diversas funciones de este sistema estaban bastante bien integradas. | 2.50 |
| Q6 | Me pareció que había demasiada inconsistencia en este sistema. | 2.31 |
| Q7 | Imagino que la mayoría de las personas aprenderían a usar este sistema muy rápidamente. | 2.81 |
| Q8 | Encontré el sistema muy engorroso o difícil de usar. | 2.31 |
| Q9 | Me sentí muy confiado/a al usar el sistema. | 2.38 |
| Q10 | Necesité aprender muchas cosas antes de poder empezar a usar este sistema. | **2.06 (peor puntaje)** |

## Interpretación cualitativa

El ítem con peor puntaje normalizado es **Q10 — "Necesité aprender muchas cosas antes de poder
empezar a usar este sistema"** (2.06/4), seguido de Q6 y Q8 (2.31/4 cada uno). El patrón indicado
por los diez ítems no apunta a un problema puntual sino a una **carga de aprendizaje inicial alta y
percepción de inconsistencia**: los tres ítems peor puntuados son exactamente los que miden curva de
aprendizaje (Q10), consistencia (Q6) y facilidad general de uso (Q8). A nivel global, el sistema
**no alcanza** el umbral de aceptabilidad del proyecto (61.25 vs 68), con una calificación "D" en la
escala de Bangor — un resultado que indica margen de mejora real en la usabilidad, no solo en el
número final.

Cinco de las dieciséis sesiones (P12–P16) puntuaron notablemente por debajo del resto del grupo
(57.5, 50.0, 30.0, 35.0 y 15.0). Al revisar [`registro-sesiones.csv`](registro-sesiones.csv) para
esas mismas cinco filas, sus notas cualitativas describen las sesiones como fluidas y sin fricción
— lo que **no es consistente** con puntuaciones tan bajas y merece una revisión por parte del equipo
antes del examen: o las notas de sesión no capturaron correctamente lo que reportó cada participante
en el formulario, o hay algo en esas cinco sesiones concretas (perfil de los participantes, orden en
que se aplicó la prueba, u otro factor) que explique por qué su experiencia reportada difiere tanto
de la de los primeros once. Esta discrepancia no se puede resolver solo con los datos disponibles en
este repositorio.

## Análisis inferencial

El bloque de rendimiento de este proyecto (k6) compara dos escenarios (caché frío vs. caliente) y
por eso usa un test de dos muestras (t de Welch) con su tamaño de efecto (d de Cohen). **El SUS no
tiene ese mismo diseño**: es una sola muestra de 16 personas evaluando un único sistema, no hay un
segundo grupo con el que comparar. El análisis inferencial que corresponde aquí no es un test de
comparación de medias, es la **estimación del intervalo de confianza de la media poblacional** a
partir de la muestra — que es exactamente lo que ya reporta la tabla de Resultados: **IC 95% [49.49,
73.01]**, calculado con la t de Student y los grados de libertad correctos (df=15), no con el
límite normal de 1.96 que solo es válido para muestras grandes.

**Verificación independiente por bootstrap.** Con n=16 y una escala acotada 0-100, el supuesto de
normalidad detrás del IC paramétrico es una simplificación razonable pero no garantizada. Como
verificación independiente, se calculó también un intervalo de confianza por **bootstrap percentil**
(10 000 remuestreos con reemplazo, semilla fija `20260904` para reproducibilidad exacta) sobre los
mismos 16 puntajes:

| Método | IC 95% |
|---|---|
| Paramétrico (t de Student, df=15) | [49.49, 73.01] |
| Bootstrap percentil (10 000 remuestreos) | [50.47, 71.41] |

Los dos intervalos son consistentes entre sí (se superponen ampliamente, con centros muy próximos),
lo que respalda que el supuesto de normalidad del IC paramétrico no está distorsionando el
resultado de forma relevante pese al tamaño de muestra reducido. **Ambos intervalos cruzan por
debajo del umbral de aceptación del proyecto (68) en su extremo superior**, lo que significa que,
aunque el punto estimado (61.25) está claramente por debajo del umbral, la evidencia estadística no
permite descartar por completo que la usabilidad real esté cerca del umbral — una lectura más
matizada que decir simplemente "no lo alcanza", y coherente con el tamaño de muestra pequeño.

Script versionado que reproduce este resultado desde `sus-raw.csv`:
```bash
python3 docs/mediciones/sus/bootstrap-sus.py
```
Salida completa: [`salida-bootstrap-sus.txt`](salida-bootstrap-sus.txt). El script reutiliza la
misma función `puntaje_sus()` de `analisis-sus.py` (no reimplementa la fórmula de Brooke por
separado), así que los 16 puntajes de partida son idénticos en ambos análisis.

## Limitaciones metodológicas

- **Redacción del cuestionario.** El formulario aplicado usa una traducción española estándar del
  SUS de Brooke, pero **no es literalmente la redacción especificada** en
  [`instrucciones-formulario.md`](instrucciones-formulario.md#1-crear-el-formulario) (p. ej. "Me
  pareció que el sistema era fácil de usar" en vez de "Pensé que el sistema era fácil de usar").
  El orden de los 10 ítems y su polaridad (impares en positivo, pares en negativo) coinciden
  exactamente con el original, por lo que el cálculo numérico no se ve afectado — pero se deja
  constancia de la desviación textual en vez de reportarla como si fuera literal.

## Referencias de consentimiento

Participantes referenciados solo por código. Los consentimientos firmados **no se suben al
repositorio** (contienen nombre y firma) y se conservan en una carpeta local del equipo, fuera de
control de versiones — ver `docs/etica/consentimientos/plantilla.md` y su `.gitignore`. La columna
**Folio** de esta tabla es lo que hace la evidencia *verificable* sin exponer datos personales: es
el identificador (número de folio físico, o el hash SHA-256 del PDF/foto del documento firmado)
que permite a un tercero pedir ver ese documento concreto y comprobar que corresponde a esta fila,
sin que el repositorio necesite contener el documento en sí.

> **Confirmado (2026-09-03):** el equipo tiene un documento de consentimiento **individual** por
> participante (cada persona aceptó y firmó su propio bloque de consentimiento, consolidados
> después en un mismo archivo/carpeta por comodidad) — no un consentimiento colectivo. Esto
> satisface el requisito de la guía §5.2 punto 2, que descarta explícitamente el consentimiento
> colectivo como sustituto del individual.
>
> **Completado (2026-09-04).** El equipo escaneó los 16 consentimientos firmados
> (`G:\EPSCAN\p0.PDF` a `p15.PDF`, un archivo por participante) y se calculó el SHA-256 de cada
> uno. **Primer intento con hallazgo real:** la primera tanda escaneada solo contenía 2 archivos
> distintos duplicados 8 veces cada uno (un error de impresora/escáner, según reportó el equipo) —
> se detectó exactamente por esto, porque el hash de dos archivos "diferentes" salía idéntico. Se
> volvió a escanear y la segunda tanda sí produjo **16 hashes distintos, verificados uno por uno**
> (`sha256sum p0.PDF ... p15.PDF`, ninguno repetido). Mapeo asumido:
> `p0→P01, p1→P02, ..., p15→P16` (por orden numérico) — **si el equipo asignó los códigos en otro
> orden, corregir la tabla antes de darla por definitiva.**

| Código | Fecha | Folio / hash del documento firmado |
|---|---|---|
| P01 | 2026-08-16 | `sha256:d8510c56ba0daff6d17e3278cb9696ef2bbc1dc3d470d4c06a75aa88cd59ab7b` |
| P02 | 2026-08-16 | `sha256:3bb3e3d07b365131a079a894304685da87707a149729fd121157ea5108319a63` |
| P03 | 2026-08-16 | `sha256:d46fcda14cf924d11f36f740a4c43db2c21b0c3f5320f73783c8f70d7fe24ba6` |
| P04 | 2026-08-16 | `sha256:2f87c434a6883400aa21f8b7a4333dfaa39ca6c072ee414597596c5899a324aa` |
| P05 | 2026-08-16 | `sha256:f464ff648963343d4bfe752dc4276dcce8855776daa9acab57568f231dba675e` |
| P06 | 2026-08-16 | `sha256:25e74c68fbe2ab9bf4ec7b0f26d3c48a70b2d20fc46c7e6d4c3eab113dc84ba2` |
| P07 | 2026-08-16 | `sha256:10c2dfacc34404c434ef910565726e0d2e8d8935942a554fea9f5b46297c3c2a` |
| P08 | 2026-08-16 | `sha256:b741d4f05de1f2190019a18b6b0e23c74f1a883c2a4dbf83b96e0e014c5d5fee` |
| P09 | 2026-08-16 | `sha256:92cb125ec8df0237cee9d8d8580c35b7d32f5fcac108b451f7b0425744ccfcbf` |
| P10 | 2026-08-16 | `sha256:f2b4b8414f2ff17639fef3c46aee261867117c751688d2b024dbcef05cc0ecec` |
| P11 | 2026-08-16 | `sha256:c0e4e83e7f485334775ab19abffb9cdff1357a176b343aeca187c8642b7930c9` |
| P12 | 2026-08-16 | `sha256:94c2e6499450ab6bc6350bc8f4d4933a108b17f6ea522a91f2e5ad871046bf99` |
| P13 | 2026-08-16 | `sha256:34deb77a6816cc4e7b34ba9f53b457b554a10671a4c4b5d05e35bfbae1ebb57b` |
| P14 | 2026-08-16 | `sha256:41a1ab3cf47efb21ca12d715069d768c2fb536026ba643b180cf900e527c9d22` |
| P15 | 2026-08-16 | `sha256:93e19fd7a65d0298ad57cf70823d69b06c10636f14b8b372456a10afc3f34708` |
| P16 | 2026-08-16 | `sha256:45bedcd2e16e9b1b1fd0a64de855dc1aa0c6bfec0163ec949059ce3f7f44623f` |

Los 16 hashes son distintos entre sí (verificado: `sort -u` sobre las 16 salidas de `sha256sum`
da 16 líneas, ninguna colisión), y ninguno se repite — a diferencia del primer intento escaneado.
Cualquiera con acceso a `G:\EPSCAN\p{0..15}.PDF` puede recalcular estos mismos hashes y
confirmarlos.

No completar esta columna con un valor inventado: un folio o hash que no corresponda a un
documento real que efectivamente exista sería el mismo tipo de problema que el propio hallazgo del
SUS que motivó esta corrección.
