# Observaciones para el examen — ARTISYNC

**Compilación íntegra de todas las observaciones recibidas de la Entrega Final del PFC.**

| Campo | Valor |
|---|---|
| Equipo | ARTISYNC — Plataforma de comisiones y contenido digital |
| Repositorio auditado | https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC |
| Rama / commit evaluado | `main` / `09f6221` — 2026-09-01T17:11:24-05:00 |
| Estado del árbol auditado | 381 commits, 1.405 archivos, 8 ramas remotas, 6 identidades Git para 4 personas |
| Fecha de la revisión | 2 de septiembre de 2026 |
| Nota del equipo | **6,60 / 10** |
| Examen final | Semana 19 — del 7 al 11 de septiembre de 2026 |

**Fuentes de este documento (transcritas íntegramente, sin resumir):**

1. Informe global del equipo — Entrega Final.
2. Informe del aporte individual — RIOS CUYABAZO JHON KEVIN.
3. Informe del aporte individual — FIGUEROA MORALES BRYAN JAVIER.
4. Informe del aporte individual — CARVAJAL LOOR JOHAN STALIN.
5. Guía de desarrollo «Camino al examen final — ARTISYNC» (UTEQ, septiembre 2026).

> **Regla de precedencia.** La Guía de desarrollo (§1.2) declara: *«Antes de escribir esta guía volví sobre mi propia evaluación y verifiqué de nuevo cada dato contra el repositorio. Encontré errores míos y los corregí. Las cifras que aparecen aquí son las corregidas, y cuando difieren de las que les di en la evaluación, esta guía es la versión válida.»*
> Por tanto: **donde la guía y el informe difieren, manda la guía.** Las cifras rectificadas están marcadas en la sección §7 de este documento y anotadas en cada observación afectada.

---

## Índice

- [§0. Marco de calificación aplicado](#0-marco-de-calificación-aplicado)
- [§1. Criterios de piso y reglas transversales](#1-criterios-de-piso-y-reglas-transversales)
- [§2. Observaciones del Eje 1 — Producto software (35 %)](#2-observaciones-del-eje-1--producto-software-35)
- [§3. Observaciones del Eje 2 — Documento técnico académico (40 %)](#3-observaciones-del-eje-2--documento-técnico-académico-40)
- [§4. Observaciones del Eje 3 — Reproducibilidad, datos y publicabilidad (25 %)](#4-observaciones-del-eje-3--reproducibilidad-datos-y-publicabilidad-25)
- [§5. Fortalezas reconocidas (no tocar, no romper)](#5-fortalezas-reconocidas-no-tocar-no-romper)
- [§6. Los 16 puntos exigidos al equipo antes del examen](#6-los-16-puntos-exigidos-al-equipo-antes-del-examen)
- [§7. Correcciones que el docente hizo a su propia evaluación](#7-correcciones-que-el-docente-hizo-a-su-propia-evaluación)
- [§8. Protocolo de medición exigido (Guía cap. 4)](#8-protocolo-de-medición-exigido-guía-cap-4)
- [§9. Mediciones con personas (Guía cap. 5)](#9-mediciones-con-personas-guía-cap-5)
- [§10. Evidencia de autoría propia (Guía cap. 6)](#10-evidencia-de-autoría-propia-guía-cap-6)
- [§11. Calendario y forma de comprobación (Guía cap. 7)](#11-calendario-y-forma-de-comprobación-guía-cap-7)
- [§12. Observaciones individuales](#12-observaciones-individuales)
- [§13. Índice maestro de observaciones (checklist)](#13-índice-maestro-de-observaciones-checklist)

---

## §0. Marco de calificación aplicado

- Rúbrica: Guía de la Cuarta Entrega (Entrega Final) del PFC — **diecisiete criterios en tres ejes**.
- Eje 1 Producto software **35 %**; Eje 2 Documento técnico académico **40 %**; Eje 3 Reproducibilidad, datos y publicabilidad **25 %**.
- Cinco niveles por criterio: Excelente 100, Satisfactorio 75, En desarrollo 50, Insuficiente 25, Ausente 0.
- Fórmula: `Nota_100 = Σ (peso_i × nivel_i)`; `Nota_10 = Nota_100 / 10`.

**Cálculo obtenido:**

```
Eje 1: 5x75 + 7x50 + 6x50 + 7x75 + 5x100 + 5x25 = 2.175
Eje 2: 8x75 + 6x50 + 5x25 + 7x75 + 6x25 + 4x100 + 4x75 = 2.400
Eje 3: 8x75 + 6x75 + 6x100 + 5x75 = 2.025
Suma ponderada = 6.600 sobre 10.000 | Nota_100 = 66,00
NOTA DEL EQUIPO = 6,60 / 10
```

**Criterios de piso aplicados: NINGUNO. Reglas transversales de sanción aplicadas: NINGUNA.**

> Cita literal del docente: *«La guía establece que incumplir un criterio de piso implica calificación CERO en toda la entrega. En esta revisión he decidido NO aplicar esa regla a la calificación, ni las reglas transversales de sanción. Es una oportunidad, no una renuncia: **en el examen final los pisos sí se aplican**.»*

**Consecuencia operativa:** los cuatro incumplimientos de piso/transversales listados en §1 dejan de ser «puntos que bajan la nota» y pasan a ser **condiciones de aprobación**. Si el 7 de septiembre siguen incumplidos, la calificación de toda la entrega es cero.

---

## §1. Criterios de piso y reglas transversales

### PISO-01 ❌ [NO IMPLEMENTADO] — Carátula de una página con la URL del repositorio en una sola línea
**Estado: PARCIAL.**
- No hay PDF de carátula de una página en el repositorio.
- Lo que sí se verificó, «y es más de lo que logró casi nadie»: la portada del informe lleva la URL en una sola línea, los cuatro ORCID, el hash corto `d07656b` y los dos DOI de Zenodo. «Es la portada más completa del curso.»
- **Falta:** el artefacto PDF de carátula independiente, de una sola página.

### PISO-02 ✅ [IMPLEMENTADO] — El PDF se regenera clonando el repositorio y compilando el `.tex` con instrucciones del README
**Estado: EL FUENTE CUMPLE, EL ARTEFACTO ENTREGADO NO.**
- Verificación del docente sobre copia limpia: `pdflatex` + `bibtex` + dos pasadas más → **cuatro pasos con código de salida cero, 76 páginas, cero referencias sin resolver, cero citas indefinidas, once desbordes de caja**. «Su fuente está bien.»
- El PDF versionado, `Informe-Final-v1.0.0.pdf`: **69 páginas**, **NOVENTA ocurrencias de corchete con interrogación** (`[?]`), incluidas las del propio Resumen, e **índices general, de figuras y de cuadros completamente vacíos**. Se compiló con una sola pasada y sin bibtex.
- Diferencia de siete páginas: **rectificado en la guía** → dos páginas son los índices y **cinco vienen de la bibliografía resuelta y del reflujo del texto** (el informe decía «exactamente esas listas»).
- Cita: *«Es la situación más frustrante de toda la revisión: hicieron bien el trabajo y entregaron el artefacto mal compilado. Se arregla en dos minutos.»*

### PISO-03 (Regla transversal 8) — URL pública
**Estado: SE CONFIRMA EL INCUMPLIMIENTO.**
- El propio `DEPLOYMENT.md` del equipo declara que el sistema **no está desplegado**.
- Las dos URL de Render **no devolvieron un solo byte en noventa segundos** (85 s y 90 s de espera respectivamente para frontend y `/actuator/health`).

### PISO-04 (Regla transversal 9) — SRS sin firma
**Estado: SE CONFIRMA EL INCUMPLIMIENTO.**
- El SRS existe y tiene doce páginas, pero su sección 8 dice literalmente: **«Estado de la aprobación: pendiente de firma»**.

---

## §2. Observaciones del Eje 1 — Producto software (35 %)

### P0 — Aplicación de observaciones acumuladas | Peso 5 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:**
- Veintinueve observaciones únicas, **más que ningún otro equipo**, incluidas **trece autoimpuestas**.
- Veintiséis resueltas, una parcial y dos pendientes: **89,66 %**.
- Los treinta y dos tokens hexadecimales verificados con `git cat-file`: **los veintinueve que son hashes existen, ninguno inventado**. Los tres que fallan son los DOI de Zenodo y el `max-age` de HSTS, no hashes.
- Las cuatro etiquetas exigidas existen, **aunque `v0.7.1` y `v0.9.0-rc` apuntan al mismo commit**. El Anexo A está.

**OBS-P0-01 ❌ [NO IMPLEMENTADO] — No se alcanza el 100 % de observaciones resueltas.** Quedan una parcial y dos pendientes.

**OBS-P0-02 ❌ [NO IMPLEMENTADO] — Tres cifras incompatibles del mismo dato en el mismo entregable.**
- 29 sobre 26 con 89,7 % en la tabla.
- 86,2 % en una nota interna.
- 27 sobre 23 con 85,2 % en el Anexo A del PDF.

**OBS-P0-03 ❌ [NO IMPLEMENTADO] — Dos etiquetas apuntan al mismo commit** (`v0.7.1` y `v0.9.0-rc`), lo que debilita la trazabilidad de versiones.

---

### P1 — Cierre funcional, cobertura y estrategia de acceso a datos | Peso 7 % | **En desarrollo (50 %)**

**Lo verificado y correcto:**
- JaCoCo recalculado desde el XML y desde el CSV, **resultados idénticos entre ambos**: **72,02 % de líneas (2.867 de 3.981)** y **62,49 % de ramas (703 de 1.125)**. Coincide dígito a dígito con el README. **El XML es salida real de la herramienta.**
- El pipeline es real y **las ocho corridas más recientes figuran en verde**. Los tres scripts de CI se ejecutaron y **los tres terminan con código cero**.
- Veintiocho rutinas versionadas y veintiséis conectadas: «un trabajo enorme».

**OBS-P1-01 ✅ [IMPLEMENTADO] — Ninguna de las tres capas cumple las dos métricas de cobertura.** El criterio pide líneas Y ramas en las tres capas:
- Servicios: **82,62 % líneas / 70,06 % ramas**.
- Controladores: **316 líneas cubiertas de 377 = 83,82 % líneas / 72,00 % ramas**.
- Global supera el 70 % en líneas (80,29 %) y casi en ramas (66,44 %, pero el requisito principal de capas se cumple).

**OBS-P1-02 ❌ [NO IMPLEMENTADO] — El mecanismo de acceso a datos exigido no se usa.**
- Solo **UNA** rutina se invoca con `@Procedure`; **ninguna** con `@NamedStoredProcedureQuery`.
- El resto pasa por `@Query` con `nativeQuery`, «que no es lo que la guía exige».
- **Rectificado en la guía:** de veintiocho rutinas versionadas, veintiséis se invocan desde el código, **pero ninguna de ellas usa el mecanismo formal**: hay una sola anotación de ese tipo en todo el proyecto y **corresponde a una rutina que no está entre las veintiocho**.

**OBS-P1-03 ❌ [NO IMPLEMENTADO] — El catálogo de rutinas declara veintiuna activas cuando son veintiocho.**

---

### P2 — Rendimiento con evidencia estadística | Peso 6 % | **En desarrollo (50 %)**

**Lo verificado y correcto:**
- «La aritmética es impecable.» Percentiles recalculados desde los seis NDJSON, con **21.076 líneas y 1.500 puntos cada uno**: **las dieciséis cifras publicadas coinciden dígito a dígito**, incluidos los p95 de **50,17 ms** y **39,14 ms** y el **0 % de errores en las seis corridas**.
- **Las consolas archivadas son salida real de k6.**

**OBS-P2-01 ✅ [IMPLEMENTADO] — El endpoint medido NO está protegido.** *(el más serio de este criterio)*
- La URL es `/api/v1/catalogo` y el propio `SecurityConfig` la declara `permitAll`.
- «La guía pide medir contra un endpoint protegido, precisamente porque el coste de la autenticación forma parte de lo que se quiere medir.»
- Guía §4.3: *«medir un endpoint público y llamarlo rendimiento del sistema es engañarse.»*

**OBS-P2-02 ✅ [IMPLEMENTADO] — Tres corridas por escenario, no las cinco exigidas.**

**OBS-P2-03 ✅ [IMPLEMENTADO] — No hay test inferencial ni tamaño de efecto,** y el propio equipo lo admite en el capítulo 8.
- El docente los calculó: **Welch t = 14,538** y **d de Cohen = 0,3065**.
- **Matiz de la guía:** *«aunque para datos de latencia les recomiendo el estadístico no paramétrico y su tamaño de efecto ordinal, según el capítulo 4»* → Mann-Whitney/Wilcoxon + **Â₁₂ de Vargha–Delaney** o **δ de Cliff**, con corrección por comparaciones múltiples si hay más de una comparación.

---

### P3 — Seguridad OWASP + ZAP + auditoría de acceso a datos | Peso 7 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:**
- **Todos los artefactos de herramienta son genuinos**, verificados uno a uno: **ZAP 2.17.0** con su bloque de insights, **SpotBugs 4.8.6 sobre 488 clases en 80,21 segundos**, y **seis informes completos de Lighthouse 12.6.1**.
- **Higiene de SQL impecable:** cero `EXECUTE IMMEDIATE`, cero `createNativeQuery`, cero `String.format` sobre SQL, cero `StringBuilder` de SQL y cero `@Query` con concatenación. El único acceso dinámico es **Criteria API tipada**. El script propio de auditoría lo confirma y está en la integración continua.
- Anotaciones de autorización presentes en la mayoría de controladores *(cifra rectificada, ver §7)*.

**OBS-P3-01 ❌ [NO IMPLEMENTADO] — Evidencia OWASP incompleta.** De los seis controles OWASP **solo uno tiene el comando `curl` literalmente transcrito**, y el propio equipo marca la evidencia de **A07 como OBSOLETA**.

**OBS-P3-02 ❌ [NO IMPLEMENTADO] — Dos alertas medias de ZAP:** **CSP ausente** y **falta de anti-clickjacking**, porque el frontend —el único punto público— **no emite ninguna cabecera de seguridad** aunque el backend sí las configure.

**OBS-P3-03 ❌ [NO IMPLEMENTADO] — La cookie de refresco sale con `secure` en `false`.**

**OBS-P3-04 ❌ [NO IMPLEMENTADO] — Credenciales en el historial público.** *(cifras rectificadas por la guía)*
- `JWT_SECRET`: presente en **358 commits**, expuesto **desde el 20 de junio**.
- `DB_PASSWORD` y `DB_APP_PASSWORD`: en **264 y 260 commits** respectivamente, expuestas **desde el 7 de agosto**.
- (El informe original decía «al menos diez commits» y «desde el 10 de agosto» — cifras superadas por la guía.)
- **El árbol actual está limpio, pero el historial no**, y **el mismo `JWT_SECRET` sigue en el `application.properties` de pruebas**.

---

### P4 — Calidad web Lighthouse | Peso 5 % | **Excelente (100 %)**

**Lo verificado y correcto:**
- Seis informes genuinos de **Lighthouse 12.6.1**, tres por perfil.
- Móvil: rendimiento **81, 81, 80**. Escritorio: **100, 100, 100**.
- **Accesibilidad 93, buenas prácticas 96 y SEO 100 en los seis.**
- Los cuatro umbrales se cumplen en los dos perfiles y en las tres corridas, y coinciden con lo que declara el README.

**OBS-P4-01 ❌ [NO IMPLEMENTADO] — Única reserva:** se auditó **localhost** y **solo la portada**.
- Guía §4.5 exige: tres corridas por perfil **contra el despliegue público**, **sobre más de una ruta**, con `requestedUrl` apuntando a la URL pública y el **JSON completo** (más de 150 auditorías), no solo las cuatro puntuaciones de categoría.

---

### P5 — Puesta en producción accesible | Peso 5 % | **Insuficiente (25 %)**

**OBS-P5-01 ✅ [IMPLEMENTADO] — No hay despliegue.**
- El propio `DEPLOYMENT.md` lo declara.
- Comprobado: ni el frontend ni `/actuator/health` devolvieron un solo byte en 85 y 90 segundos.
- El nivel no es *Ausente* porque los cuatro documentos existen y tienen contenido real: **DEPLOYMENT (100 líneas), RUNBOOK (96), BACKUP (71)** y un **ADR-007 aceptado**. Hay además un `render.yaml` y una rama de despliegue con un **RENDER.md de 219 líneas**.
- Cita: **«Falta ejecutarlo.»**

---

## §3. Observaciones del Eje 2 — Documento técnico académico (40 %)

### D0R — Culminación de la ingeniería de requisitos | Peso 8 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:**
- **Veintitrés historias** con Connextra y Gherkin y **veintitrés casos de uso** con plantilla de Cockburn completa.
- De **veintiséis requisitos Must, veinticuatro verificados = 92,3 %**, con las dos excepciones razonadas por escrito.

**OBS-D0R-01 ❌ [NO IMPLEMENTADO] — Una clase de prueba referenciada no existe por error de nombre:** `SeguidorServiceImplTest` frente a `SeguidorServicioImplTest`.
- *(Cifra rectificada: son **34 clases distintas referenciadas por la matriz, de las que 33 existen** — no 38 de las que existen 36. El hallazgo de fondo, que una está mal escrita, es correcto.)*

**OBS-D0R-02 ❌ [NO IMPLEMENTADO] — El SRS no está firmado** (ver PISO-04).

**OBS-D0R-03 ❌ [NO IMPLEMENTADO] — INVEST aparece en una sola de las veintitrés historias.**

**OBS-D0R-04 ❌ [NO IMPLEMENTADO] — El equipo declara que solo un caso de uso puede trazarse a diagrama de secuencia.**

---

### D1 — Estructura IMRaD, secciones obligatorias y capítulo de RE | Peso 6 % | **En desarrollo (50 %)**

**Lo verificado y correcto:**
- Las **dieciocho subsecciones** están presentes estructuralmente: introducción con exactamente cinco subsecciones, **las nueve declaraciones**, **diez anexos**, amenazas con las cuatro categorías y **tabla ISO/IEC 25010**.

**OBS-D1-01 ✅ [IMPLEMENTADO] — El artefacto entregado está roto:** noventa citas sin resolver e índices vacíos. «Eso pesa aquí tanto como en el piso 2.»

**OBS-D1-02 ✅ [IMPLEMENTADO] — Los resúmenes se pasan mucho:** **390 palabras el español y 335 el inglés**, frente a un máximo de 250. Rango exigido: **200–250 palabras**.

**OBS-D1-03 ❌ [NO IMPLEMENTADO] — No hay ningún archivo `.dsl`:** el DSL de Structurizr está embebido en archivos Markdown.

**OBS-D1-04 ❌ [NO IMPLEMENTADO] — Los cuatro listados tienen caption y etiqueta pero ninguno se referencia con `\ref`.**

**OBS-D1-05 ❌ [NO IMPLEMENTADO] — El listado que muestra el procedimiento almacenado muestra un `@Query` nativo, no un `@Procedure`.** El listado del documento debe mostrar el código que existe.

**OBS-D1-06 ✅ [IMPLEMENTADO] — La lista de siglas sale vacía** porque ni el `Makefile` ni el `README` ejecutan `makeglossaries`. *(La guía añade: el propio documento de compilación del equipo la prescribe, pero ni el Makefile ni el README de la raíz la ejecutan.)*

**OBS-D1-07 ❌ [NO IMPLEMENTADO] — No hay índice de listados.**

**OBS-D1-08 ❌ [NO IMPLEMENTADO] — De 42 etiquetas, 26 están huérfanas** (sin referencia en el texto).

---

### D2 — Trabajos relacionados y brecha | Peso 5 % | **Insuficiente (25 %)**

**Lo reconocido:** el capítulo dice literalmente: *«Este capítulo no presenta una revisión sistemática. Presentar aquí una tabla comparativa de ocho filas constituiría una afirmación de completitud falsa.»* El docente: *«Es honesto y prefiero mil veces eso a una tabla inventada.»* Y en la guía: *«les reconozco un gesto poco común… Prefiero eso, con diferencia, a un capítulo inventado.»*

**OBS-D2-01 ❌ [NO IMPLEMENTADO] — No hay cadena booleana de búsqueda.**
**OBS-D2-02 ❌ [NO IMPLEMENTADO] — No hay bases indexadas declaradas.**
**OBS-D2-03 ❌ [NO IMPLEMENTADO] — No hay ventana temporal.**
**OBS-D2-04 ❌ [NO IMPLEMENTADO] — No hay criterios de inclusión y exclusión.**
**OBS-D2-05 ❌ [NO IMPLEMENTADO] — No hay diagrama PRISMA 2020 con números.**
**OBS-D2-06 ❌ [NO IMPLEMENTADO] — La tabla comparativa tiene cero filas frente a las ocho exigidas.**
**Lo único presente:** el párrafo de brecha, «y está bien argumentado».

- Marco exigido: **PRISMA 2020** (Page et al., BMJ 2021) y **directrices de estudios de mapeo** (Petersen, Vakkalanka y Kuzniarz, IST vol. 64, 2015).
- Cita: *«La salida no es rebajar el capítulo, es hacerlo. Tienen una semana.»*

---

### D3 — Metodología y protocolo experimental | Peso 7 % | **Satisfactorio (75 %)**

**Lo verificado y correcto (sin observaciones negativas propias):**
- **DSR de Peffers** instanciado en una tabla con las seis actividades.
- **GQM completo**: meta, tres preguntas y tres métricas.
- **Baltes aparece cuatro veces** y sustenta el muestreo por conveniencia, **admitiendo por escrito que no hay justificación de tamaño basada en poder estadístico**.
- **Ralph aparece siete veces** *(rectificado; el informe decía catorce)*.
- Demografía recalculada desde el CSV de participantes: **n = 16, edades de 21 a 62, media 34,56, reparto 50/50 por sexo**. Todas las cifras coinciden con el documento.
- **Las semillas están documentadas** y la estadística descriptiva se declara a priori.

---

### D4 — Análisis estadístico riguroso | Peso 6 % | **Insuficiente (25 %)**

> **«Este es el hallazgo más serio de toda la revisión del curso.»**

**Lo verificado y correcto:** el cálculo del SUS es perfecto. Aplicada la fórmula de Brooke sobre `sus-raw.csv` se obtiene exactamente **76,88 de media, 77,50 de mediana, 14,48 de desviación e intervalo [69,16 ; 84,59]**, y **hasta el mismo SHA-256 del archivo de entrada**. «El análisis es correcto.»

**OBS-D4-01 ✅ [IMPLEMENTADO] — El dato de entrada está modificado.** *(hallazgo central)*
- En el repositorio está también el export real del formulario, con dieciséis respuestas y su marca temporal.
- **Las filas 1 a 11 coinciden. Las cinco últimas no:**

| Participante | Export del formulario | `sus-raw.csv` |
|---|---|---|
| P12 | 57,5 | 75,0 |
| P13 | 50,0 | 92,5 |
| P14 | 30,0 | 85,0 |
| P15 | 35,0 | 92,5 |
| P16 | 15,0 | 92,5 |

- **Cinco de dieciséis filas alteradas, las cinco al alza**, y son **precisamente las cinco de puntuación más baja del formulario**.
- **P12 es además un duplicado exacto de P11.**
- Con los datos crudos reales: **media 61,25, intervalo [49,49 ; 73,01]** → **por debajo del umbral de aceptación de 68**.
- Con los datos alterados: **76,88** → «cómodamente por encima».
- Los dos archivos crecieron en el mismo commit, **`6f8c09c` del 16 de agosto**.
- **El propio `DATA-PROVENANCE` afirma que ninguno de los archivos crudos fue editado a mano** — afirmación que el hallazgo desmiente.

**OBS-D4-02 ✅ [IMPLEMENTADO] — No hay ningún test inferencial ni tamaño de efecto en todo el trabajo.**

**Postura del docente (literal):**
> *«No estoy aplicando ninguna sanción a la nota por esto, como anuncié al principio. Pero no puedo puntuar como riguroso un análisis cuyo dato de entrada está modificado, y necesito que hablemos de ello antes del examen.»*
> *«Un 61,25 declarado vale infinitamente más que un 76,88 que no se sostiene.»*
> Guía §5.4: *«Ajustar los datos para que el número quede bonito es la única falta de este curso que no tiene arreglo posterior.»*

---

### D5 — Amenazas a la validez y discusión crítica | Peso 4 % | **Excelente (100 %)**

**Sin observaciones negativas.** Las cuatro categorías con sección propia y mitigación explícita; la discusión responde RQ1 a RQ4 una a una con respuestas matizadas. La de RQ1 se cita como ejemplar: *«Parcialmente: cumple en líneas con 72,0 por ciento pero no en ramas con 62,5.»* → **«Así se responde una pregunta de investigación.»**

---

### D6 — Bibliografía y calidad de citas | Peso 4 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:** **37 entradas, las 37 citadas, 90 comandos de cita, cero huérfanas y cero citas sin entrada.** Estilo `ieeetr` consistente. Los **veinte DOI declarados resuelven**.

**OBS-D6-01 ❌ [NO IMPLEMENTADO] — La entrada `PERES2024` declara un DOI que resuelve a un artículo distinto**, sobre estrategias de marketing en crowdfinanciación. El DOI correcto de ese trabajo existe y fue verificado contra Crossref; **lleva al volumen y las páginas que el equipo cita**. Esa referencia está citada en el cuerpo y contada como de alto impacto.

**OBS-D6-02 ❌ [NO IMPLEMENTADO] — Una segunda referencia resuelve con el venue correcto pero un título distinto.** *(añadido por la guía: los identificadores con título coincidente son **18 de 20**, no 19 — hay por tanto **dos** referencias defectuosas, no una.)*

**OBS-D6-03 ❌ [NO IMPLEMENTADO] — El alto impacto queda en dieciséis de treinta y dos, por debajo del mínimo de veinte**, cosa que el propio equipo declara en el Anexo J.

---

## §4. Observaciones del Eje 3 — Reproducibilidad, datos y publicabilidad (25 %)

### R1 — Reproducción end-to-end en un solo comando | Peso 8 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:**
- **Catorce objetivos** y un `make all` que encadena compose up, pruebas, k6, SpotBugs, ZAP, Lighthouse en los dos perfiles, SUS, SRS y **tres pasadas de LaTeX con copia del PDF**.
- **«Es la cadena más completa del curso»**; `make -n all` ejecutado con código de salida cero.
- Los permisos no rompen nada: los cuatro scripts están en `100644` pero se invocan con `bash`; ejecutados, **los tres terminan bien**.

**OBS-R1-01 ❌ [NO IMPLEMENTADO] — Exige un `.env` que no está versionado:** desde un clon limpio **falla sin un paso manual**.
**OBS-R1-02 ❌ [NO IMPLEMENTADO] — `make sus` invoca `python` en vez de `python3`.**
**OBS-R1-03 ❌ [NO IMPLEMENTADO] — `make all` no llama a `audit-sql-dynamic`.**
**OBS-R1-04 ❌ [NO IMPLEMENTADO] — Hay que renombrar a mano los informes de Lighthouse.**
**OBS-R1-05 ❌ [NO IMPLEMENTADO] — No hay ningún cuaderno Jupyter.**
**OBS-R1-06 ❌ [NO IMPLEMENTADO] — El digest `sha256` no está en ninguno de los tres sitios exigidos.**

---

### R2 — Datos con DOI, diccionario y provenance | Peso 6 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:**
- **Los dos DOI son reales, separados y correctos**, y resuelven: el **software con MIT** y el **dataset de mediciones empíricas con CC BY 4.0**, ambos con los cuatro autores y coherentes con el README, con `CITATION.cff` y con la portada. «Eso es exactamente lo que pide el criterio.»
- `DATA-PROVENANCE` cita seis hashes y **los seis existen**.

**OBS-R2-01 ✅ [IMPLEMENTADO] — El diccionario documenta veintiuna variables agregadas y deja fuera unas treinta y tres crudas**, incluidas **Q1 a Q10 del SUS** y **las trece columnas de `jacoco.csv`**.

**OBS-R2-02 ✅ [IMPLEMENTADO] — `DATA-PROVENANCE` afirma que ningún archivo crudo fue editado a mano, lo que el hallazgo del SUS desmiente.**

---

### R3 — Metadatos, CRediT y ORCID | Peso 6 % | **Excelente (100 %)**

> **«Este es su mejor criterio y es el mejor del curso.»**

- `CITATION.cff` 1.2.0 completo, con los cuatro autores, sus ORCID, los DOI y `preferred-citation`.
- **Los cuatro ORCID validados con ISO 7064 MOD 11-2: los cuatro correctos**, y además coinciden en `CITATION.cff`, en `CONTRIBUTORS.md`, en `.zenodo.json` y en la portada del PDF. **«Nadie más logró esa coherencia.»**
- **Los catorce roles CRediT están todos asignados** y las nueve declaraciones están presentes.
- El cruce entre el historial de git y los autores declarados **no arroja ninguna discrepancia**.

**OBS-R3-01 ❌ [NO IMPLEMENTADO] — Composición del equipo sin aclarar** (no afecta a la nota de este criterio, pero es exigencia formal).
- El repositorio tiene **cuatro personas con commits** y el padrón del curso registra **tres**.
- **Detalle de la guía:** la cuarta identidad tiene **quince commits y el setenta y cinco por ciento del trabajo de base de datos**, y **no figura en la lista del curso**.
- Cita: *«no es un problema técnico, es un problema de autoría.»*
- **OBS-R3-02 (guía):** hay **un rol de contribución que hoy figura sin persona asignada** — completar.

---

### R4 — Cumplimiento de estándares de reporte | Peso 5 % | **Satisfactorio (75 %)**

**Lo verificado y correcto:** los checklists **no son plantillas**: **Ralph con trece ítems, FAIR con dieciséis e INCOSE con quince**, todos con párrafo de evidencia concreta, y varios sin marcar **con un contraejemplo real citado**. El de PRISMA se resuelve como «no aplica» con justificación, coherente con la decisión sobre el capítulo 3.

**OBS-R4-01 ❌ [NO IMPLEMENTADO] — El checklist de Ralph está fechado el 17 de agosto y afirma en tres ítems que el documento académico y el capítulo de amenazas «no existen», cuando existen desde hace dos semanas.**
- Nota: al escribirse el capítulo 3 (D2), el checklist PRISMA deja de poder resolverse como «no aplica» y debe rehacerse.

---

## §5. Fortalezas reconocidas (no tocar, no romper)

Estas son las ocho fortalezas que el docente reconoce explícitamente. **Ninguna corrección debe degradarlas.**

1. **Los metadatos de citación son los mejores del curso:** cuatro ORCID válidos por ISO 7064, coherentes en cuatro archivos distintos, dos DOI de Zenodo que resuelven con el tipo y la licencia correctos, catorce roles CRediT y cero discrepancias entre el historial y la autoría declarada.
2. **El bloque de rendimiento reproduce a la perfección:** dieciséis cifras verificadas desde nueve mil observaciones crudas, con consolas de k6 que confirman la ejecución real.
3. **Higiene de SQL impecable:** cero concatenaciones, veintiocho rutinas versionadas, veintiséis conectadas de punta a punta con parámetros nombrados.
4. **Todos los artefactos de herramienta son genuinos:** JaCoCo, ZAP 2.17.0, SpotBugs 4.8.6 sobre 488 clases y seis informes de Lighthouse completos. **«No encontré ni uno fabricado.»**
5. **Veintinueve observaciones registradas, trece autoimpuestas**, y los veintinueve hashes existen.
6. **Honestidad documental sistemática:** declaran con cifra exacta cada brecha propia, y el capítulo 3 renuncia a fabricar una revisión sistemática en vez de simularla.
7. **El fuente LaTeX compila limpio**, sin una sola referencia ni cita sin resolver.
8. **Anotaciones de autorización extensas** y **una cadena de `make all` que es la más completa del curso**.

Y en la guía, además: **la portada es la más completa del curso.**

---

## §6. Los 16 puntos exigidos al equipo antes del examen

Transcripción literal de la lista «LO QUE ESPERO DE USTEDES ANTES DEL EXAMEN FINAL».

| # | Exigencia | Criterios que desbloquea |
|---|---|---|
| 1 ✅ | **Hablar del SUS antes del examen.** Cinco de dieciséis respuestas modificadas al alza; el resultado pasa de 61,25 (bajo umbral) a 76,88 (sobre umbral). Traer el instrumento original y explicarlo. Si no hay explicación, **publicar la cifra real**: «un 61,25 declarado vale infinitamente más que un 76,88 que no se sostiene». | D4, R2 |
| 2 ✅ | **Recompilar el PDF con bibtex y tres pasadas y volver a subirlo.** Hoy tiene noventa citas sin resolver y los tres índices vacíos cuando el fuente compila limpio. «Es la corrección más barata y la que más daño les está haciendo.» | PISO-02, D1 |
| 3 ❌ | **Escribir el capítulo de trabajos relacionados de verdad:** bases indexadas, cadena booleana, ventana temporal, criterios, diagrama PRISMA 2020 con números y tabla comparativa de al menos ocho filas. | D2, R4 |
| 4 ❌ | **Corregir el DOI de la referencia `PERES2024`**, que apunta a otro artículo. | D6 |
| 5 ✅ | **Medir k6 contra un endpoint protegido**, subir a cinco corridas por escenario y añadir el test inferencial con tamaño de efecto (Welch t = 14,538; d de Cohen = 0,3065 — preferible el no paramétrico ordinal). | P2 |
| 6 ❌ | **Conectar los procedimientos con `@Procedure` o `@NamedStoredProcedureQuery`.** Hoy solo uno de veintiséis usa el mecanismo exigido. | P1, D1 |
| 7 ✅ | **Subir la cobertura de controladores** (hoy 29,17 % de líneas) **y la de ramas en general**. | P1 |
| 8 ❌ | **Poner las cabeceras de seguridad en el nginx del frontend**, que es el único punto público, **y activar `secure` en la cookie de refresco**. | P3, P5 |
| 9 ❌ | **Rotar las credenciales del historial público** y **sacar el `JWT_SECRET` del `application.properties` de pruebas**. | P3 |
| 10 ✅ | **Desplegar.** Tienen el `render.yaml`, la rama y un `RENDER.md` de 219 líneas: solo falta ejecutarlo. Es el criterio P5 completo. | P5, PISO-03, P4 |
| 11 ✅ | **Bajar los resúmenes a 200-250 palabras** (hoy 390 y 335) **y ejecutar `makeglossaries`** para que la lista de siglas deje de salir vacía. | D1 |
| 12 ❌ | **Referenciar los cuatro listados y las 26 etiquetas huérfanas**, y **cambiar el listado del procedimiento para que muestre el `@Procedure`**. | D1 |
| 13 ❌ | **Actualizar el catálogo de rutinas** (declara veintiuna cuando son veintiocho) **y unificar las tres cifras de observaciones**. | P0, P1 |
| 14 ✅ | **Completar `DATA-DICTIONARY` con las variables crudas** y **corregir la afirmación de inmutabilidad de `DATA-PROVENANCE`**. | R2 |
| 15 ❌ | **Aclarar formalmente la composición del equipo:** hay cuatro personas con commits y tres en el padrón. | R3 |
| 16 ❌ | **Las tres exigencias fijadas siguen incumplidas:** (a) el **80,7 % de los tipos** y el **66,2 % de los métodos** llevan token español; (b) de **543 métodos públicos solo veintiocho tienen Javadoc** y en todo el backend hay **ocho `@param`, cero `@return` y un `@throws`**; (c) **tres de las cuatro figuras están en español**. | D1, transversal |

---

## §7. Correcciones que el docente hizo a su propia evaluación

Recuadro literal de la Guía §2.2. **Estas cifras sustituyen a las del informe.**

| # | Lo que se dijo en el informe | Lo que es correcto (guía) |
|---|---|---|
| COR-01 | Las siete páginas de diferencia entre el PDF entregado y el recompilado «son exactamente los índices». | **Los índices explican dos páginas; las otras cinco vienen de la bibliografía resuelta y del reflujo del texto.** |
| COR-02 | Un autor (Ralph) aparece **catorce** veces en el documento. | **Aparece siete.** |
| COR-03 | **182** anotaciones de autorización en **32 de 39** controladores. | **150** anotaciones, presentes en **33 de 39** controladores. |
| COR-04 | **38** clases de prueba referenciadas por la matriz, de las que existen **36**. | **34** clases distintas referenciadas, de las que **33 existen**. El hallazgo de fondo (una mal escrita) es correcto. |
| COR-05 | **19** de 20 DOI devuelven el título y venue declarados. | **18 de 20.** Hay por tanto **dos** referencias defectuosas: `PERES2024` (resuelve a otra obra) y una segunda con venue correcto pero **título distinto**. |
| COR-06 | Credenciales expuestas «desde el 10 de agosto», «en al menos diez commits». | **`JWT_SECRET` desde el 20 de junio, en 358 commits. Contraseñas de BD desde el 7 de agosto, en 264 y 260 commits.** |

---

## §8. Protocolo de medición exigido (Guía cap. 4)

### §8.1 — La regla que gobierna todo lo demás

> **«Todo número que aparezca en su documento debe poder recalcularse desde un archivo crudo versionado en el repositorio, con un comando que ustedes documenten. Si un número no se puede recalcular, no es un resultado: es una afirmación.»**

Tres consecuencias prácticas obligatorias:
1. **El dato crudo se versiona, aunque ocupe.**
2. **El script que lo transforma se versiona también, y debe ejecutarse sin edición manual.**
3. **El documento cita el archivo y el commit del que sale cada cifra.**

> Advertencia: *«El error más caro que puede cometer un equipo no es medir poco. Es publicar un número que su propio repositorio desmiente. Cuando eso ocurre, un lector deja de creer también los números que sí eran correctos.»*

### §8.2 — Cobertura de pruebas
- **Qué capturar:** `jacoco.csv` y `jacoco.xml` de la ejecución de cierre. **No el informe HTML** («el HTML es para leer; el CSV y el XML son los que permiten recalcular»).
- **Cómo:** una sola ejecución de `mvn clean verify` **sobre el commit que van a defender**, con la puerta de calidad configurada en el `pom.xml`. La cifra publicada debe ser la de ese archivo, **no la de una corrida anterior**.
- **Qué reportar:** cobertura de **líneas y de ramas, global y desglosada por paquete**. Las dos métricas siempre. El **umbral que declare el texto debe ser el mismo que fija el `pom.xml`**.
- **Aceptación:** un tercero ejecuta `mvn clean verify`, abre el `jacoco.csv` resultante, suma las columnas y obtiene **exactamente** la cifra publicada.

### §8.3 — Rendimiento
- **Qué capturar:** salida cruda de k6 en **NDJSON** o el resumen JSON de cada corrida, **uno por archivo, versionados**. **La consola de k6 no es evidencia suficiente**: no permite recalcular percentiles.
- **Cómo:** **cinco corridas independientes por escenario**, no tres. Mismo perfil de carga, declarado en el script y versionado. **El escenario debe medir un endpoint autenticado y representativo del uso real.**
- **Qué reportar:** por escenario **media, mediana, desviación típica, percentiles 90, 95 y 99, tasa de error y número de peticiones**. Comparación entre escenarios con **test no paramétrico, tamaño de efecto y corrección por comparaciones múltiples** cuando haya más de una comparación.
- **Referencias exigidas:** Arcuri y Briand (STVR 24(3), 2012) para el porqué del no paramétrico; **Â₁₂ de Vargha y Delaney** (JEBS 25(2), 2000) o **δ de Cliff** (Psychological Bulletin 114(3), 1993) para el tamaño de efecto ordinal.
- Cita: *«Un percentil calculado sobre una sola corrida no tiene incertidumbre asociada y por tanto no es comparable con nada.»*
- **Aceptación:** cinco archivos crudos por escenario **y un script versionado** que, ejecutado sobre ellos, **imprime exactamente las cifras del documento**, incluidos el estadístico del test, **el valor p en notación científica** y el tamaño de efecto.

### §8.4 — Seguridad
- **Qué capturar:** salida real de las herramientas, **no un resumen escrito a mano**. *«Un archivo de un kilobyte con tres cifras dentro»* no es ni un informe de análisis estático ni uno dinámico.
- **Cómo:** análisis estático con el plugin configurado en el `pom.xml`, **en la fase `verify`, sobre todo el código de producción**. **Escaneo dinámico contra la aplicación desplegada y autenticada**, no contra la raíz sin sesión: *«un escaneo que solo alcanza la portada y el `robots.txt` no ha probado nada del sistema»*. Auditoría de acceso **con peticiones reales transcritas, con su fecha en UTC y el hash del commit auditado**.
- **Qué reportar:** conteo por nivel de severidad **tal como lo emite la herramienta, sin reinterpretarlo**.
- **Aceptación:** los archivos de salida **abren con la herramienta que los produjo**, y el conteo por severidad del documento coincide con el del archivo, **hallazgo por hallazgo**.

### §8.5 — Calidad web
- **Qué capturar:** **el JSON completo** de cada corrida de Lighthouse, con sus más de **ciento cincuenta auditorías**, no solo las cuatro puntuaciones.
- **Cómo:** **tres corridas por perfil** (móvil y escritorio) **contra el despliegue público, no contra localhost**, y **sobre más de una ruta** («la portada no representa a la aplicación»).
- **Qué reportar:** **media de las tres corridas por perfil y por ruta**, con la **versión exacta de la herramienta y la marca de tiempo de cada corrida**. *«Si la versión que declara el documento no es la que aparece en el JSON, el dato queda invalidado.»*
- **Aceptación:** al menos **seis archivos JSON completos**, con **`requestedUrl` apuntando a la URL pública**, y las puntuaciones del documento son la **media aritmética** de los que están en el repositorio.

### §8.6 — Trazabilidad de los datos
- Cada figura y cada tabla debe poder rastrearse hasta el archivo del que sale, en un archivo de procedencia que asocia **fila a fila**: elemento del documento ↔ archivo crudo ↔ script que lo procesa ↔ **hash del commit** en que se generó.
- **Obligación:** *«Comprueben con `git cat-file -t` cada hash que escriban en ese archivo antes de comitearlo. Un archivo de procedencia que cuelga de un commit inexistente es peor que no tenerlo, porque promete verificabilidad y no la da.»*

### §8.7 — Publicación del conjunto de datos
- Datos crudos y scripts depositados con **identificador permanente propio, separado del depósito del software**, y **licencia que permita la reutilización**.
- Marco: **principios FAIR** (Wilkinson et al., Scientific Data 3, 2016) y **diez reglas de investigación computacional reproducible** (Sandve et al., PLoS Comp Biol 9(10), 2013).
- El documento debe incluir **declaración de disponibilidad de datos** (dónde están, con qué identificador, bajo qué condiciones) **y una equivalente para el código**.
- **Aceptación:** el identificador del dataset **resuelve en línea**, apunta a un depósito **de tipo dataset distinto del software**, lleva **licencia abierta** y **los autores coinciden con los del repositorio**.

---

## §9. Mediciones con personas (Guía cap. 5)

> **«Este capítulo es el que peor resuelto está en todo el curso, sin excepción. Léanlo entero antes de volver a recoger un solo dato.»**

### §9.1 — Por qué es distinto
Cuando se mide latencia el sujeto es el servidor; cuando se mide usabilidad **el sujeto es una persona**: hace falta **permiso para recoger el dato, permiso para publicarlo, y una manera de demostrar que la persona existió**.

### §9.2 — Lo mínimo que deben tener (cinco requisitos)

1. **Aprobación previa.** Documento del comité o instancia académica, **con fecha anterior a la recogida de datos**, número o código de expediente, y título del estudio. **Emitido antes, no después.**
2. **Consentimiento informado individual.** Un formulario **por participante, firmado**, que explique qué se recoge, para qué, cuánto tiempo se conserva, quién lo verá y cómo se retira el consentimiento. **Un consentimiento colectivo, una constancia de regularización o un permiso genérico de la institución NO sustituyen al consentimiento individual.**
3. **Registro de sesiones.** Fecha, hora, duración y modalidad de cada sesión. **Sin nombres en el archivo público**: un identificador por participante y la correspondencia guardada aparte.
4. **Anonimización.** El archivo publicado no debe permitir reidentificar a nadie — **incluidas combinaciones de edad, sexo, rol y dispositivo cuando la población es pequeña**.
5. **Declaración en el documento.** Sección que diga **qué comité aprobó el estudio, con qué número**, y que **se obtuvo consentimiento informado de todos los participantes**.

### §9.3 — Tamaño y selección de la muestra
- **Mínimo de la asignatura: quince participantes.** Por debajo, el intervalo de confianza del SUS es tan ancho que no distingue un sistema aceptable de uno que no lo es (Bangor, Kortum y Miller, IJHCI 24(6), 2008).
- El **muestreo por conveniencia es admisible**, pero hay que **declararlo como tal y explicar sus consecuencias sobre la validez externa**: cómo se reclutó, de qué población, qué sesgos introduce y a qué población se extrapola (Baltes y Ralph, EMSE 27(4), 2022).

### §9.4 — El instrumento
- SUS: **diez enunciados, alternando polaridad, escala de cinco puntos**. Puntuación = restar 1 a los impares, restar los pares de 5, sumar y **multiplicar por 2,5**. **El resultado va de cero a cien y NO es un porcentaje.**
- Reportar **media, mediana, desviación típica e intervalo de confianza calculado con la t de Student y los grados de libertad correctos, NO con 1,96** (que corresponde a muestra grande). Lewis, IJHCI 34(7), 2018.
- Cita: *«Una puntuación baja bien medida vale infinitamente más que una alta que no se sostiene. Si el resultado les desfavorece, publíquenlo y discútanlo: eso es un hallazgo, y es exactamente lo que la asignatura evalúa.»*

### §9.5 — Las seis señales que delatan un conjunto de datos que no se sostiene

Comprobaciones que el docente hace y que el equipo debe hacer primero:

1. **Un ítem con varianza exactamente cero** (todos responden lo mismo). Con quince personas es improbable; con tres ítems a la vez, no ocurre.
2. **Variables demográficas que alternan con regularidad perfecta** (sexo alternando por fila, dispositivo según paridad del número de participante).
3. **Filas duplicadas exactas entre participantes distintos.** ← *ARTISYNC incurre: P12 duplica P11.*
4. **Diferencias entre el archivo exportado del formulario y el archivo que se analiza.** ← *ARTISYNC incurre: cinco filas.*
5. **Un archivo de procedencia que declara un commit de origen que no existe en el repositorio.**
6. **Una suma que no cuadra con los números que la componen.**

**Aceptación:** existe la aprobación fechada, existen los consentimientos individuales, el archivo de respuestas tiene **al menos quince filas**, el recálculo desde ese archivo **reproduce exactamente la puntuación publicada**, y **ninguna de las seis señales aparece**.

---

## §10. Evidencia de autoría propia (Guía cap. 6)

### §10.1 — El planteamiento
> *«Usar herramientas de asistencia no está prohibido y no es lo que se juzga. Lo que se juzga es si ustedes entienden, controlan y pueden defender lo que entregan.»*
> *«No existe ningún detector fiable de contenido generado automáticamente, ni para texto ni para código. Por eso la acreditación no se hace con un detector: se hace con el rastro que deja el proceso de trabajo. Ese rastro se produce mientras se trabaja y no se puede fabricar después.»*

### §10.2 — Lo que SÍ acredita el trabajo propio

**a) El historial como bitácora.** Muchos commits pequeños repartidos en muchos días, no pocos commits enormes en dos jornadas. Mensajes que dicen qué se hizo y por qué. Y sobre todo **estados intermedios equivocados**: una prueba que falla, el arreglo, la prueba que pasa.

Concretamente, **para cada integrante**:
- Commits en **al menos diez días distintos** repartidos a lo largo del periodo.
- Mensajes con **convención de commits y descripción real**; nada de una palabra suelta, un código sin significado o la palabra `update`.
- **Una identidad Git única, con el correo institucional, en todo el historial.**
- **Commits que introducen un fallo y commits posteriores que lo corrigen**, con el mensaje explicando qué pasaba.

**b) Revisión entre ustedes.** Ramas por funcionalidad, pull requests, y **comentarios de revisión de un integrante sobre el trabajo de otro, con respuesta y cambios derivados de esa conversación**. *«Una revisión con observaciones sustantivas es de las evidencias más difíciles de simular y de las más valoradas.»*

**c) Los registros de decisión.** Cada ADR con **las alternativas consideradas y por qué se descartó cada una**, y cuando corresponda **la fecha en que cambiaron de opinión y el commit que materializó el cambio**. *«Un registro que solo describe la decisión final no acredita nada; uno que muestra el camino, sí.»*

**d) Los artefactos previos al código.** Bocetos de interfaz, esquemas de datos dibujados a mano, notas de reunión fechadas, fotografías de pizarra. **Súbanlos al repositorio con su fecha.**

**e) La correspondencia entre historial y sistema.** Que la funcionalidad descrita aparezca en el historial en el orden en que se construyó, y **que las mediciones estén fechadas después de la funcionalidad que miden**.

**f) La declaración de uso de asistencia.** Sección del documento que diga **sin ambigüedad qué herramientas se usaron, en qué partes concretas, con qué grado de supervisión y qué verificó cada integrante**. *«En esta asignatura la voy a exigir. Redáctenla en positivo: describe cómo trabajaron, no es una confesión.»*

**g) La defensa.** Evidencia decisiva, no delegable. En el examen final se pedirá:
- **Explicar por qué una decisión concreta del código es como es y qué alternativa se descartó.**
- **Localizar, sin buscador, dónde vive una funcionalidad determinada.**
- **Modificar algo en vivo:** añadir una validación, cambiar una consulta, **hacer pasar una prueba que falla**.
- **Justificar una cifra del documento y decir de qué archivo sale.**
- **Responder por partes del sistema que no escribió personalmente.**

> *«Ese último punto es importante y afecta a varios de ustedes: si su participación se concentró en una capa, tienen esta semana para que sus compañeros les expliquen el resto.»*

### §10.3 — Lo que NO acredita nada
- Una declaración firmada de autoría por sí sola (es un requisito, no una prueba).
- Que un detector automático no marque el texto.
- El estilo del código (ni la elegancia ni el desorden).
- Un historial con muchos commits hechos el mismo día sobre archivos completos: **«el volumen no es cadencia»**.
- Que el sistema funcione: **«un sistema que funciona y que nadie del equipo sabe explicar es exactamente el caso que preocupa»**.

### §10.4 — Lo que SÍ levanta sospecha
- Archivos extensos y completos que entran **en un solo commit sin estados intermedios**.
- Documentación que **describe un sistema distinto del que existe**.
- **Referencias bibliográficas cuyo identificador no resuelve o resuelve a otra obra.** ← *ARTISYNC incurre (D6).*
- Artefactos de evidencia que **no son salida de la herramienta que dicen ser**.
- **Cifras que el propio repositorio desmiente.** ← *ARTISYNC incurre (D4/SUS).*

**Aceptación:** cada integrante puede mostrar commits propios en **al menos diez días distintos**, con **correo institucional** y mensajes descriptivos; existe **al menos una revisión entre integrantes con observaciones sustantivas**; los ADR incluyen **alternativas descartadas**; existe **la declaración de uso de asistencia**; y **cada integrante puede modificar en vivo una parte del sistema que no escribió**.

---

## §11. Calendario y forma de comprobación (Guía cap. 7)

### §11.1 — Marco temporal
El examen final es la **semana 19, del 7 al 11 de septiembre**. **Todo lo que entre en el repositorio hasta el día del examen cuenta.** *«Trabajen la semana completa: la diferencia entre los equipos que subieron y los que no, en las entregas anteriores, fue exactamente esa.»*

### §11.2 — Orden de trabajo recomendado por el docente

> *«El orden no es arbitrario. Está construido por relación entre lo que cuesta y lo que resuelve: primero lo que se arregla en minutos y desbloquea un criterio completo, después lo que exige trabajo sostenido, y al final lo que depende de que lo anterior esté hecho.»*

| Día | Bloque | Tareas |
|---|---|---|
| **Lunes** | Lo urgente | Recompilar y subir el PDF · Rotar las tres credenciales del historial · Traer el instrumento del estudio de usabilidad · Aclarar por escrito la composición del equipo |
| **Martes y miércoles** | El producto | Desplegar y declarar la dirección pública · Poner las cabeceras en el frontend y cerrar la cookie · Conectar los procedimientos por el mecanismo formal · Subir la cobertura de controladores |
| **Jueves** | Las mediciones | Repetir la carga contra un endpoint protegido con cinco corridas · Añadir el test inferencial y el tamaño de efecto · Actualizar el catálogo de rutinas |
| **Viernes** | El documento | Escribir el capítulo de trabajos relacionados · Corregir las dos referencias · Referenciar las etiquetas huérfanas · Ajustar los resúmenes · Documentar los métodos públicos |

### §11.3 — Cómo se va a comprobar

> *«En el examen final voy a clonar su repositorio en una máquina limpia y ejecutar lo que su documentación diga que hay que ejecutar. **Lo que no funcione desde ese clon, no cuenta.**»*
> *«La comprobación más barata y la que más equipos suspende es esta: clonar el repositorio en un directorio vacío y ejecutar el comando que documenta su `README.md`. Háganla hoy.»*

### §11.4 — Los diez criterios de aceptación del plan de correcciones (Guía cap. 3)

| Tarea | Criterio de aceptación literal |
|---|---|
| 3.1 Usabilidad | El archivo que se analiza **coincide fila a fila** con el export del instrumento, el recálculo reproduce la cifra publicada, y **existen aprobación y consentimientos** según el capítulo 5. |
| 3.2 PDF | El PDF versionado tiene **el mismo número de páginas que el recompilado**, **cero interrogaciones** y **los tres índices con contenido**. |
| 3.3 Despliegue | Una petición a la dirección pública declarada **devuelve la aplicación**, y **las cabeceras de seguridad están presentes en la respuesta**. |
| 3.4 Credenciales | **Ninguno de los valores expuestos sigue siendo válido**, y **no aparecen en el árbol actual ni en la configuración de pruebas**. |
| 3.5 Procedimientos | Las rutinas de negocio principales **se invocan con la anotación formal**, el catálogo **declara el número real** y el listado del documento **muestra el código que existe**. |
| 3.6 Mediciones | **Cinco archivos crudos por escenario contra un endpoint autenticado**, y el documento reporta **test, valor p en notación científica y tamaño de efecto**. |
| 3.7 Trabajos relacionados | El capítulo declara **bases consultadas, cadena de búsqueda, ventana temporal, criterios y diagrama de flujo con cifras**, y la tabla comparativa **cita cada trabajo por su clave bibliográfica**. |
| 3.8 Bibliografía | **Los veinte identificadores resuelven a la obra que el archivo bibliográfico declara**, y **ninguna etiqueta queda sin referencia en el texto**. |
| 3.9 Equipo | **La composición del equipo es la misma en el repositorio, en los archivos de autoría, en el documento y en el padrón.** |
| 3.10 Javadoc | **Los métodos públicos de servicios y controladores documentan parámetros, retorno y excepciones.** |

---

## §12. Observaciones individuales

### §12.1 — FIGUEROA MORALES BRYAN JAVIER
**Rol declarado:** Documento, mediciones y pruebas · **Calificación final: 7.46 / 10**

**Evidencia de autoría:** 144 commits sin merges, **+73.471 / −27.973 líneas** propias sobre **843 archivos distintos**, en **31 días distintos** entre el 4 de junio y el 1 de septiembre de 2026. Identidad `Bryan Figueroa / bfigueroam@uteq.edu.ec` — **correo institucional y una sola identidad en todo el historial**.

| Dimensión | Peso | Nivel |
|---|---|---|
| I1 — Volumen y sustancia de la contribución | 30 | **4/4** |
| I2 — Alcance técnico sobre los módulos de la guía | 25 | **4/4** |
| I3 — Trazabilidad y calidad del historial | 20 | **4/4** |
| I4 — Aporte a la evidencia medida y al documento | 25 | **2/4** |

`Suma = 30x4 + 25x4 + 20x4 + 25x2 = 350` → individual **8.75** · `6.60 x 0,60 + 8.75 x 0,40 = 7.46`

**Reconocimientos:**
- Aporte **mayor del equipo en volumen, mayor del curso entero medido en líneas propias**, y el más amplio en cobertura de capas. **31 días activos = segunda ventana de trabajo más larga del curso.**
- Reparto: **21.401 líneas de documentación (84 % del equipo)**, **13.823 de backend (46 %)**, **13.692 de pruebas (72 %)**, **11.731 de frontend (32 %)**, **10.743 de base de datos (21 %)**, **860 de infraestructura y CI (96 %)**.
- **Primer contribuyente en documentación, pruebas, backend y CI.** «El eje documental y el eje de reproducibilidad de esta entrega son suyos.»
- Historial correcto: **109 de 144 mensajes siguen la convención, mediana de 73 caracteres (la más alta del equipo)**, solo tres telegramas.
- **Correo institucional e identidad única** → autoría verificable sin inferencia. «En un equipo con seis identidades Git para cuatro personas, eso vale.»

**Observación principal (literal):**
> Es el autor de **todos** los artefactos del estudio SUS: el export del formulario, `sus-raw.csv`, `analisis-sus.py`, `graficar-sus.py`, `REPORTE-SUS.md`, `boxplot-sus.png` y `salida-sus.txt`. Todos entraron en sus commits del **30 de julio** y del **16 de agosto**.
> Comparados participante por participante: las once primeras filas coinciden exactamente, las cinco últimas no. **«Y no varían al azar. Las cinco filas modificadas son precisamente las cinco de puntuación más baja del formulario: 57,5, 50,0, 30,0, 35,0 y 15,0. En `sus-raw.csv` esas mismas cinco figuran como 75,0, 92,5, 85,0, 92,5 y 92,5.»**
> **«Eso es lo que fija su dimensión I4 en 2: el eje documental es suyo, y la pieza de evidencia central de ese eje no se sostiene contra su propia fuente.»**
> **«Traiga el instrumento original y explíquemelo.»**

**Sus ocho exigencias:**
1. **Antes que nada: traer el instrumento original del SUS.** Si hay explicación, se quiere oír. Si no, **publicar el 61,25 y declarar en el documento que el sistema no alcanza el umbral de aceptabilidad**. «Eso es lo correcto y no le va a costar la nota.»
2. Escribir el capítulo de trabajos relacionados de verdad (PRISMA 2020, ocho filas).
3. Corregir el DOI de `PERES2024`.
4. Medir k6 contra endpoint protegido, cinco corridas, test inferencial con tamaño de efecto.
5. Bajar los resúmenes a 200-250 palabras y ejecutar `makeglossaries`.
6. Referenciar los cuatro listados y las 26 etiquetas huérfanas; cambiar el listado del procedimiento para que muestre `@Procedure`.
7. Actualizar el catálogo de rutinas (21 → 28) y unificar las tres cifras de observaciones.
8. Completar `DATA-DICTIONARY` con las variables crudas y corregir la afirmación de inmutabilidad de `DATA-PROVENANCE`.

---

### §12.2 — CARVAJAL LOOR JOHAN STALIN
**Rol declarado:** Propietario del repositorio, frontend y backend · **Calificación final: 7.71 / 10**

**Evidencia de autoría:** 164 commits sin merges, **+35.882 / −4.209 líneas** sobre **486 archivos**, en **13 días distintos** entre el 4 de junio y el 1 de septiembre de 2026. Dos identidades bajo el alias `Johan_Loor`: `91645452+johancarvajal04@users.noreply.github.com` (146 commits) y `carvajalstalin.10@gmail.com` (18). **Ninguno de los dos es institucional.**

| Dimensión | Peso | Nivel |
|---|---|---|
| I1 | 30 | **4/4** |
| I2 | 25 | **4/4** |
| I3 | 20 | **4/4** |
| I4 | 25 | **3/4** |

`Suma = 30x4 + 25x4 + 20x4 + 25x3 = 375` → individual **9.38** · `6.60 x 0,60 + 9.38 x 0,40 = 7.71`

**Reconocimientos:**
- **«Su aporte es el que construyó la aplicación, en el sentido literal de la palabra.»**
- Reparto: **19.912 líneas de frontend (54 % del equipo, mayor contribución individual a esa capa)**, **7.986 de backend (27 %)**, **4.209 de pruebas (22 %)**, **1.856 de base de datos**, **1.802 de documentación**.
- Propietario de la cuenta donde vive el repositorio y **quien mantuvo la integración de las ocho ramas remotas**.
- Ventana hasta el **1 de septiembre**, último día de la entrega.
- **La portada del informe es la más completa del curso.** «Ningún otro equipo llegó a eso.»

**Observaciones:**
- **IND-JC-01 — Trazabilidad del historial.** De sus 164 mensajes, 129 siguen la convención pero **once no dicen absolutamente nada**: `fixeds`, `update`, `xd`, y **ocho que son solo la palabra `feat` o la palabra `fix` sin descripción**. **Mediana de 44 caracteres, la más baja del equipo.** *«Un mensaje de commit es documentación, no un trámite, y en un repositorio con 381 commits y ocho ramas es lo único que permite reconstruir qué se hizo cuándo.»*
- **IND-JC-02 — Correo no institucional en ninguna de sus dos identidades**, lo que obliga a inferir la autoría en vez de verificarla.
- **IND-JC-03 — El PDF mal compilado (fija I4 en 3).** *«Su fuente compila limpio, lo comprobé yo mismo… Entregaron mal compilado un documento que está bien hecho. Como propietario del repositorio y responsable de la integración, ese artefacto es el que usted publicó.»*
- **IND-JC-04 — Dato informativo:** su cuenta registra **dos commits el 10 de agosto en el repositorio de otro equipo del curso, BIOPET**. «Corresponden a otra actividad y no afectan a esta calificación, pero quedan en el historial público.»

**Sus ocho exigencias:**
1. Recompilar el PDF con bibtex y tres pasadas y volver a subirlo.
2. **Desplegar** — «Es el criterio P5 completo y hoy vale cero».
3. Conectar los procedimientos con `@Procedure` / `@NamedStoredProcedureQuery`.
4. Cabeceras de seguridad en el nginx del frontend y `secure` en la cookie de refresco.
5. Rotar las credenciales del historial y sacar el `JWT_SECRET` del `application.properties` de pruebas.
6. Subir la cobertura de controladores (hoy 29,17 %).
7. Traducir los identificadores (80,7 % de tipos y 66,2 % de métodos con token español) y escribir Javadoc.
8. **«Y desde hoy mismo, escriba mensajes de commit que digan qué hizo. Es gratis y le cuesta un punto entero de trazabilidad.»**

---

### §12.3 — RIOS CUYABAZO JHON KEVIN
**Rol declarado:** Backend · **Calificación final: 6.46 / 10**

**Evidencia de autoría:** 31 commits sin merges, **+7.022 / −514 líneas** sobre **145 archivos**, en **9 días distintos** entre el 4 de junio y el 16 de agosto de 2026. Dos identidades (`Jhon-Kevin-Rios-Cuyabazo` y `Jk-RiosC`) y dos correos: `jhonrios_180@hotmail.com` (29 commits) y el de su cuenta de GitHub (2). **Ninguno de los dos es institucional.**

| Dimensión | Peso | Nivel |
|---|---|---|
| I1 | 30 | **3/4** |
| I2 | 25 | **3/4** |
| I3 | 20 | **3/4** |
| I4 | 25 | **1/4** |

`Suma = 30x3 + 25x3 + 20x3 + 25x1 = 250` → individual **6.25** · `6.60 x 0,60 + 6.25 x 0,40 = 6.46`

**Reconocimientos:** «Lo que hizo está bien orientado: el backend es la capa más exigente del sistema y sus cinco mil líneas ahí son trabajo real. Nueve días activos tampoco es una cifra despreciable.»
- Reparto: **5.017 líneas de backend (17 % del equipo)**, **979 de pruebas (5 %)**, **860 de frontend (2 %)**, **93 de documentación (0 %)**, **70 de base de datos**.

**Observaciones (en orden de gravedad, según el docente):**
- **IND-JK-01 — Ausencia en los ejes evaluados.** Presencia en el eje documental: **93 líneas sobre 25.551 = 0 %**. Presencia en el eje de reproducibilidad (CI, mediciones, evidencia): **tres líneas**. *«Esos dos ejes son el 65 por ciento de la rúbrica de su equipo. Eso es lo que fija su dimensión I4 en 1: no es que su aporte sea pequeño, es que no toca la parte que esta entrega evalúa.»*
- **IND-JK-02 — Último commit el 16 de agosto.** *«Los diecisiete días finales, en los que se cerró el documento, se hicieron las mediciones y se publicó el PDF, no tienen ni una línea suya.»*
- **IND-JK-03 — Historial más débil del equipo en calidad.** **Solo 13 de sus 29 mensajes principales siguen la convención** (menos de la mitad), y sus dos commits de la segunda identidad tienen **mediana de 20 caracteres**. **Ninguno de sus dos correos es institucional.**

> *«Le digo lo que le corresponde: usted sabe escribir backend y eso se ve. Lo que no hizo fue participar en la entrega que se estaba evaluando.»*

**Sus seis exigencias:**
1. **Volver al repositorio esta semana y trabajar en el eje documental**, que es donde no tiene nada. «Cualquier cosa que escriba ahí le suma más que otra clase de servicio.»
2. Hacerse cargo de dos criterios completos, ambos de backend: **conectar los procedimientos con `@Procedure`/`@NamedStoredProcedureQuery`** y **subir la cobertura de controladores** (hoy 29,17 %).
3. **Escribir el Javadoc del backend**: de 543 métodos públicos solo 28 lo tienen; ocho `@param`, cero `@return`, un `@throws`.
4. **Registrar su ORCID y usar el correo institucional de aquí en adelante.**
5. Escribir mensajes de commit convencionales y descriptivos.
6. **Para la defensa: pedir a Figueroa que le explique el capítulo de mediciones y a Carvajal el despliegue.** «Va a tener que responder por partes del entregable que no escribió.»

---

## §13. Índice maestro de observaciones (checklist)

Total: **48 observaciones accionables**. Estado inicial de todas: `PENDIENTE`.

### Bloqueantes absolutos (pisos — cero en toda la entrega si siguen incumplidos)

| ID | Observación | Estado |
|---|---|---|
| PISO-01 | Carátula PDF de una página con URL en una línea | ☐ |
| PISO-02 | PDF versionado mal compilado (90 `[?]`, 3 índices vacíos, 69 vs 76 pág.) | ☐ |
| PISO-03 | Sistema no desplegado / sin URL pública viva | ☐ |
| PISO-04 | SRS sin firma («pendiente de firma» en su §8) | ☐ |

### Eje 1 — Producto software

| ID | Observación | Estado |
|---|---|---|
| OBS-P0-01 | 3 observaciones sin resolver (26/29) | ☐ |
| OBS-P0-02 | Tres cifras incompatibles del mismo dato (89,7 / 86,2 / 85,2) | ☐ |
| OBS-P0-03 | `v0.7.1` y `v0.9.0-rc` apuntan al mismo commit | ☐ |
| OBS-P1-01 | Cobertura: ninguna capa cumple líneas Y ramas (ctrl. 29,17 / 30,56) | ☐ |
| OBS-P1-02 | Ninguna de las 26 rutinas usa `@Procedure`/`@NamedStoredProcedureQuery` | ☐ |
| OBS-P1-03 | Catálogo declara 21 rutinas activas cuando son 28 | ☐ |
| OBS-P2-01 | Endpoint medido `/api/v1/catalogo` es `permitAll` | ☐ |
| OBS-P2-02 | Tres corridas por escenario en vez de cinco | ☐ |
| OBS-P2-03 | Sin test inferencial ni tamaño de efecto | ☐ |
| OBS-P3-01 | Solo 1 de 6 controles OWASP con `curl` transcrito; A07 OBSOLETA | ☐ |
| OBS-P3-02 | Frontend sin CSP ni anti-clickjacking (2 alertas medias ZAP) | ☐ |
| OBS-P3-03 | Cookie de refresco con `secure=false` | ☐ |
| OBS-P3-04 | `JWT_SECRET` en 358 commits (20-jun) y BD_PASSWORD en 264/260 (7-ago); secreto sigue en `application.properties` de pruebas | ☐ |
| OBS-P4-01 | Lighthouse contra localhost y solo la portada | ☐ |
| OBS-P5-01 | No hay despliegue ejecutado | ☐ |

### Eje 2 — Documento técnico académico

| ID | Observación | Estado |
|---|---|---|
| OBS-D0R-01 | `SeguidorServiceImplTest` → `SeguidorServicioImplTest` (nombre mal escrito) | ☐ |
| OBS-D0R-02 | SRS sin firma | ☐ |
| OBS-D0R-03 | INVEST en 1 de 23 historias | ☐ |
| OBS-D0R-04 | Solo 1 caso de uso trazable a diagrama de secuencia | ☐ |
| OBS-D1-01 | Artefacto PDF roto (90 citas sin resolver, índices vacíos) | ☐ |
| OBS-D1-02 | Resúmenes de 390 y 335 palabras (máx. 250; rango 200-250) | ☐ |
| OBS-D1-03 | No hay archivo `.dsl` (Structurizr embebido en Markdown) | ☐ |
| OBS-D1-04 | Los cuatro listados sin `\ref` en el texto | ☐ |
| OBS-D1-05 | El listado del procedimiento muestra `@Query` nativo, no `@Procedure` | ☐ |
| OBS-D1-06 | Lista de siglas vacía: falta `makeglossaries` en Makefile y README | ☐ |
| OBS-D1-07 | No hay índice de listados | ☐ |
| OBS-D1-08 | 26 de 42 etiquetas huérfanas | ☐ |
| OBS-D2-01..06 | Capítulo 3 sin cadena booleana, bases, ventana, criterios, PRISMA ni tabla de 8 filas | ☐ |
| OBS-D4-01 | 5 filas del SUS alteradas al alza; P12 duplica P11; 61,25 → 76,88 | ✅ (2026-09-03, ver `docs/mediciones/sus/PLAN-MEJORA-SUS.md`) |
| OBS-D4-02 | Sin test inferencial ni tamaño de efecto en todo el trabajo | ☐ |
| OBS-D6-01 | `PERES2024` con DOI que resuelve a otra obra | ☐ |
| OBS-D6-02 | Segunda referencia con venue correcto y título distinto (18/20) | ☐ |
| OBS-D6-03 | Alto impacto 16 de 32 (mínimo 20) | ☐ |

### Eje 3 — Reproducibilidad, datos y publicabilidad

| ID | Observación | Estado |
|---|---|---|
| OBS-R1-01 | `.env` no versionado → `make all` falla desde clon limpio | ☐ |
| OBS-R1-02 | `make sus` invoca `python` en vez de `python3` | ☐ |
| OBS-R1-03 | `make all` no llama a `audit-sql-dynamic` | ☐ |
| OBS-R1-04 | Renombrado manual de informes de Lighthouse | ☐ |
| OBS-R1-05 | No hay cuaderno Jupyter | ☐ |
| OBS-R1-06 | Digest `sha256` ausente en los tres sitios exigidos | ☐ |
| OBS-R2-01 | `DATA-DICTIONARY` omite ~33 variables crudas (Q1-Q10 SUS, 13 col. jacoco.csv) | ☐ |
| OBS-R2-02 | `DATA-PROVENANCE` afirma inmutabilidad que el SUS desmiente | ✅ (2026-09-03) |
| OBS-R3-01 | 4 personas con commits vs 3 en el padrón (la 4ª: 15 commits, 75 % de BD) | ☐ |
| OBS-R3-02 | Un rol CRediT sin persona asignada | ☐ |
| OBS-R4-01 | Checklist de Ralph desactualizado (17-ago, 3 ítems dicen «no existe») | ☐ |

### Transversales (exigencias fijadas y no cumplidas)

| ID | Observación | Estado |
|---|---|---|
| OBS-TR-01 | 80,7 % de tipos y 66,2 % de métodos con token español | ☐ |
| OBS-TR-02 | 28 de 543 métodos públicos con Javadoc; 8 `@param`, 0 `@return`, 1 `@throws` | ☐ |
| OBS-TR-03 | 3 de las 4 figuras en español | ☐ |
| OBS-TR-04 | Falta declaración de uso de asistencia (guía §6.2.6) | ☐ |
| OBS-TR-05 | Falta aprobación ética fechada y consentimientos individuales del SUS (guía §5.2) | ☐ |
| OBS-TR-06 | Falta al menos una revisión entre integrantes con observaciones sustantivas | ☐ |
| OBS-TR-07 | ADR sin alternativas descartadas documentadas | ☐ |
| OBS-TR-08 | Identidades Git múltiples y correos no institucionales (Carvajal ×2, Rios ×2) | ☐ |

---

**Documento de referencia para el plan de trabajo:** [`PLAN-EXAMEN-FINAL.md`](PLAN-EXAMEN-FINAL.md)
