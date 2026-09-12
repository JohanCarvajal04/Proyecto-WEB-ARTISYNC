# Revisión externa (Claude Code) — Rúbrica del Examen Final, 2026-09-11

**Qué es este documento.** Auditoría independiente del estado del repositorio contra
`Rubrica_ExamenFinal_ARTISYNC.pdf` (18 puntos + 3 criterios de piso), hecha el mismo día del
cierre del examen (semana 19, 7–11 de septiembre de 2026). Sigue la misma práctica de
"revisión externa (Claude Code)" que `VERIFICACION-INDEPENDIENTE-20260906.md`.

**Método.** Para cada punto distingo tres niveles de confianza:
- **Verificado directo**: lo comprobé yo mismo contra el código/artefacto real en este repositorio (`git`, `grep`, lectura de archivos, jacoco.csv real, navegador contra la URL pública).
- **Heredado del autoanálisis del equipo**: tomado de `observaciones_para_el_examen.md` / `OBSERVACIONES.md` (documentos del propio equipo, actualizados hoy), sin verificación adicional mía.
- **No verificable desde aquí**: esta máquina no tiene `pdflatex`, `bibtex`, `makeglossaries`, `mvn` ni `poppler-utils` instalados, así que no pude recompilar el informe, correr `mvn clean verify`/`mvn javadoc:javadoc`, ni renderizar el PDF a imágenes.

**No toqué nada.** Este documento es solo lectura y verificación; no modifiqué código, no moví el
tag, no hice commits.

---

## 🚨 Tres riesgos que pueden anular la nota completa, en orden de urgencia

### 1. El tag `v1.0.0` sigue apuntando al commit del 17 de agosto (316 commits atrás de `main`)

**Verificado directo.** `git log -1 v1.0.0` → `d07656b`, 2026-08-17. `git log -1 main` → `1c567a4`,
2026-09-11 (hoy). Hay **316 commits** entre uno y otro, incluyendo prácticamente todo el trabajo
de esta semana de examen (capítulo 3 reescrito, SUS corregido, k6, cobertura, cabeceras, etc.).

La rúbrica es explícita: *"Reviso el commit al que apunte el tag v1.0.0... Muevan el tag al
último commit que quieren que revise: si lo dejan donde está hoy, reviso el commit viejo y todo
lo que hicieron después no cuenta."* Si el tag no se mueve **hoy, antes de la hora de cierre
fijada en el SGA**, el docente revisa el commit de agosto — que con casi total certeza reprueba
varios puntos en "Nada" (el capítulo 3 estaba vacío, el SUS todavía en 76,88, el DOI de
PERES2024 roto, etc.), y un solo "Nada" pone toda la nota en 0,00 por la fórmula multiplicativa.

**La complicación real, y por qué no lo hice yo mismo.** `v1.0.0` no es un tag cualquiera: es
exactamente el commit archivado con DOI en Zenodo (`10.5281/zenodo.21978572`), y el propio
equipo ya decidió una vez, conscientemente, **no reasignar tags ya publicados con DOI** (ver
`OBSERVACIONES.md`, caso `v0.7.1`/`v0.9.0-rc`, que cita como precedente "el mismo criterio ya
aplicado... a v1.0.0/d07656b: no alterar una referencia ya empujada a origin ni arriesgar una
discrepancia con el DOI de Zenodo"). Mover `v1.0.0` ahora contradice ese principio.

Mover el tag **no borra ni altera** el snapshot ya archivado en Zenodo — ese DOI seguirá
resolviendo exactamente al mismo contenido de siempre, pase lo que pase con el tag en GitHub. Lo
único que se pierde es que, de ahí en adelante, "`v1.0.0` en git" y "lo archivado en Zenodo bajo
ese nombre" dejan de coincidir — un problema de prolijidad documental, no de integridad
científica, y totalmente subsanable con una nota explícita (exactamente el tipo de honestidad
documental que ya practican en otras partes).

**Qué hacer, en orden de preferencia:**
1. Si hay cualquier forma de contactar al docente-director hoy antes del cierre, la regla del
   propio examen lo permite: *"Lo que tengan que explicarme en persona se conversa con el equipo
   completo, antes del cierre."* Pregúntenle directamente si prefieren que muevan `v1.0.0` o que
   creen un tag nuevo (`v1.2.0`) — dado que él mismo escribió la regla de "muevan el tag",
   probablemente ya asumió que iban a hacerlo.
2. Si no hay tiempo de coordinar, **muevan `v1.0.0` al commit que quieren que se revise** (o
   créenlo de nuevo apuntando a `main`) y documenten en el mismo commit/README por qué: "el DOI
   de Zenodo sigue apuntando al snapshot original de agosto; el tag de git se reasignó el
   2026-09-11 siguiendo la instrucción explícita de la rúbrica del examen final". El costo de no
   moverlo (nota 0 casi segura) es muchísimo mayor que el costo de documentar la reasignación.
3. Yo no muevo tags ni hago push por mi cuenta — es una operación sobre una referencia ya
   publicada y con implicaciones de citación académica que solo el equipo debe decidir. Si
   confirman cuál de las dos opciones quieren, lo ejecuto.

### 2. E1 (nombres del código en inglés) — el propio equipo admite hoy que sigue sin tocar

**Heredado del autoanálisis del equipo, con verificación parcial mía.** El propio
`observaciones_para_el_examen.md` (actualizado hoy) dice literalmente: *"OBS-TR-01 (tokens
español, ~100 % de tipos) sigue pendiente pero no es accionable a esta altura sin riesgo de
regresión masiva"* — es decir, decidieron conscientemente no tocarlo. La línea base del 2 de
septiembre era 80,7 % de tipos y 66,2 % de métodos con palabra en español.

Mi propio muestreo directo (grep sobre `src/main/java`) encuentra una mezcla: hay paquetes ya
en inglés (`controller.seguridad.AuthController`, `service.legal.impl.*`, `config.*`, etc.) pero
también clases centrales que siguen en español (`CertificadoIaRepository`, y por lo que reporta
el equipo, buena parte del vocabulario de métodos). No pude computar el porcentaje exacto con las
herramientas de este entorno, pero la propia declaración del equipo — en contra de su propio
interés, y con una razón técnica específica dada (riesgo de regresión) — es el dato más confiable
que tengo.

**Por qué esto es más grave que parece.** El criterio E1 califica "Nada" como *"Igual o peor que
el 2 de septiembre"*. Si de verdad no cambió nada, este punto entero (peso 0,8) cae en Nada, y
por la fórmula de la rúbrica (`f1 × f2 × ... × f18`), **un solo factor en cero pone toda la nota
en 0,00** — sin importar que los otros 17 puntos estén perfectos.

**Qué hacer.** No hace falta traducir el 95 % del código hoy (imposible y arriesgado, como bien
identifica el equipo). Basta con moverse de "igual que el 2 de septiembre" a *cualquier* nivel
superior a Nada — la tabla de la rúbrica no exige mucho para salir de Nada en "Apenas" (activa
con "más del 30 %, pero menos que lo medido el 2 de septiembre"). Una pasada acotada y de bajo
riesgo — renombrar identificadores en 2-3 paquetes bien cubiertos por pruebas (p. ej. los que ya
tienen alta cobertura de líneas/ramas por el punto 7), verificar que `mvn test` sigue en verde, y
documentar el avance parcial con cifras reales (no redondeadas) — ya cambia el resultado de "0,00
seguro" a un nivel que multiplica por un factor no nulo. Ver la sección "Impacto en la nota" más
abajo para ver cuánto vale este solo cambio.

### 3. El sistema desplegado responde, pero con un arranque en frío inestable

**Verificado directo, ahora mismo (2026-09-11, ~19:53).** Abrí `https://artisync-frontend.onrender.com`
y `https://artisync-backend.onrender.com` en el navegador:
- Primer intento: la página del frontend cargó vacía y la consola mostró **4 errores 502**.
- El backend, en varios intentos sucesivos durante más de 100 segundos, mostró repetidamente la
  pantalla de "Render — Application loading / SERVICE WAKING UP" **reiniciando el cronómetro en
  cada intento**, en lugar de completar el arranque una sola vez.
- Al esperar y volver a cargar el **frontend** (no el backend directamente), la aplicación cargó
  con contenido real ("Explorar comunidad", filtros de categoría/precio) y las llamadas a
  `/api/v1/categorias`, `/api/v1/etiquetas` y `/api/v1/catalogo` se resolvieron con datos reales
  — es decir, backend y base de datos sí están funcionando de fondo.

**Lo que esto significa.** El despliegue existe y funciona, pero el plan gratuito de Render
duerme el servicio tras inactividad y el arranque en frío tardó más de 2 minutos en mostrarse
consistente. Si el docente abre la URL en un momento en que está dormida y no espera lo
suficiente, puede registrar "no responde" (Nada) en vez de "responde con algo que falla" (A
medias) o "responde bien" (Completo) — con 0,8 de peso, eso pesa.

**Qué hacer, hoy:** hagan una petición de calentamiento a ambas URLs unos minutos antes de la
hora de cierre fijada en el SGA (un simple `curl` o abrir el navegador basta), y si pueden,
consideren si vale la pena mantenerlas despiertas con un ping periódico durante la ventana en que
el docente vaya a revisar. No intenté iniciar sesión ni completar una operación yo mismo —
requeriría escribir credenciales en el formulario, y eso le corresponde hacerlo a alguien del
equipo, no a mí.

---

## Impacto en la nota (con la fórmula de la propia rúbrica)

- **Si el tag no se mueve:** nota = 0,00 casi con certeza (el commit de agosto tiene el capítulo 3
  vacío, el SUS en 76,88 sin corregir, el DOI roto — varios "Nada" garantizados).
- **Si el tag se mueve pero E1 queda igual que el 2 de septiembre (Nada):** nota = 0,00, sin
  importar qué tan bien estén los otros 17 puntos — así funciona la multiplicación de factores.
- **Si el tag se mueve y E1 sale de Nada aunque sea a "Apenas"**, con el resto de puntos en los
  niveles que verifiqué abajo (puntos 6, 10 y 11 con brechas reales; el resto en Completo):
  la nota estimada sube a **~6,3/10**. Cada punto adicional que reparen desde ahí (6, 10, 11, y
  confirmar 2) sigue subiendo desde ese piso — el punto es que **la diferencia entre 0,00 y ~6,3
  depende de un solo movimiento**: sacar a E1 de "igual que el 2 de septiembre".

---

## Punto por punto

| # | Punto (peso) | Nivel que estimo | Confianza | Evidencia |
|---|---|---|---|---|
| 1 | SUS: cifra real (1,0) | **Completo** | Verificado directo | `61.25` publicado en resumen/abstract, tabla de resultados y `DATA-PROVENANCE.md`. Las 2 únicas apariciones restantes de "76.88" (`08-evaluacion-resultados.tex:219`, `DATA-PROVENANCE.md:15`) están explícitamente enmarcadas como el error histórico ya corregido, no como cifra vigente. |
| 2 | PDF recompilado de verdad (0,6) | **No verificable desde aquí** — el equipo dice Completo | Ninguna (sin `pdflatex`/`bibtex`/`poppler` en este entorno) | Es literalmente la comprobación más barata que pueden hacer ustedes mismos hoy: clon limpio + `make docs` + abrir el PDF resultante y buscar `[?]`. Es a la vez el Piso 2 (nota cero si falla) y este punto — háganla primero. |
| 3 | Trabajos relacionados (1,0) | **Completo** | Alta (grep directo + autoanálisis) | `03-trabajos-relacionados.tex` reescrito: bases indexadas, cadena booleana, ventana 2020-2026, criterios IC/EC, PRISMA con cifras (145→62→21→8), tabla de 8 filas verificada contra `referencias.bib`. |
| 4 | DOI de PERES2024 (0,3) | **Completo** | Verificado directo | `referencias.bib:375` → `10.1016/j.ijresmar.2024.07.005`, DOI real de Elsevier que corresponde al artículo citado (Peres, Schreier, Schweidel, Sorescu, "The creator economy", IJRM 2024). |
| 5 | k6 bien medido (0,6) | **Completo** | Verificado directo | `k6/comisiones-load.js` hace login real (`/api/v1/auth/login`) y manda `Authorization: Bearer`. 5 corridas crudas × 2 condiciones (`k6-auth-run1..5.json`, `k6-auth-cold-run1..5.json`). `REPORTE-PERF.md` y `08-evaluacion-resultados.tex` reportan Mann-Whitney U + Â₁₂ de Vargha-Delaney con corrección Holm-Bonferroni, y ADEMÁS el contraste paramétrico coincide con las cifras de referencia del docente (Welch t=14.538, d=0.3065). |
| 6 | Procedimientos almacenados (0,8) | **Casi / A medias, no Completo** | Verificado directo — **corrige al alza la cifra optimista del equipo** | El equipo declara "solo quedan 4 archivos con nativeQuery". Conté **27 usos de `nativeQuery=true`** en **12 archivos** (`FollowerRepository`, `UserRepository` ×6, `RaffleRepository`, `ViolationRepository`, `PaymentTransactionRepository`, `AiCertificateRepository`, `CountryRepository`, `RoleRepository`, `TwoFactorAuthenticationRepository`, `TwoFactorBackupCodeRepository`, `UserSessionRepository`), de los cuales **26 sí invocan una rutina real de `db/procs/`** (`fn_seguir_creador`, `fn_registrar_usuario`, etc. — verificado leyendo el SQL de cada `@Query`). Frente a 35 usos de `@Procedure` + 3 de `@NamedStoredProcedureQuery`. Es una mejora enorme frente al 1/26 original, pero el mecanismo no exigido sigue siendo una fracción sustancial de las invocaciones — probablemente "Casi" (≥75%) o "A medias" (50-75%) según cómo se cuente (por rutina vs. por punto de invocación), no "Completo". |
| 7 | Cobertura controladores + ramas (0,6) | **Completo** | Verificado directo, desde `jacoco.csv` real | Controladores: **84,33 % líneas** (393/466). Ramas del proyecto completo: **70,47 %** (1544/2191) — pasa el umbral pero con muy poco margen (0,47 puntos). Viene de `target/site/jacoco/jacoco.csv` de un build local reciente; confirmen que coincide con el commit exacto que se va a evaluar. |
| 8 | Cabeceras + cookie segura (0,4) | **Completo** | Verificado directo | `nginx.conf`: CSP, X-Frame-Options, X-Content-Type-Options y Strict-Transport-Security presentes en las 3 ubicaciones (`server`, bloque de assets, `index.html`). `AuthController.java:42-43,186`: `cookieSecure` con default `true`, solo desactivable explícitamente en entorno local. |
| 9 | Credenciales rotadas + secreto fuera de pruebas (0,5) | **Completo** | Verificado directo | `application.properties` (prod) usa `${JWT_SECRET}` sin default. Las dos `application*.properties` de test usan `JWT_TEST_SECRET` con un valor base64 obviamente falso y un comentario fechado explicando la rotación. `DB_PASSWORD`/`DB_APP_PASSWORD` con defaults tipo `changeme` en todos los `.properties` revisados, ninguna credencial real. |
| 10 | Sistema desplegado (0,8) | **A medias** (riesgo real, no Completo con confianza) | Verificado directo, ahora mismo | Ver sección de riesgos arriba: responde con contenido y datos reales una vez despierto, pero el arranque en frío mostró 502s y >100s de reintentos antes de estabilizar. No pude probar login (no debo introducir credenciales). |
| 11 | Resúmenes + siglas (0,3) | **A medias** (2 de 3, con una cifra en el límite) | Estimado por mí, precisión moderada | Resumen ES ≈ **209 palabras** (dentro de 200-250 ✓). Abstract EN ≈ **193-197 palabras** por mi conteo manual — probablemente **por debajo** de 200, muy cerca del límite; cuéntenlo con su propia herramienta antes de entregar y, si falta, añadan 5-10 palabras de contenido real. Glosario: `\newacronym` ×16 + `\makeglossaries`/`\printglossary` correctamente cableados en `main.tex` y en el `Makefile` — pero no pude confirmar que el PDF *entregado* realmente tenga la lista de siglas con contenido (mismo límite de herramientas que el punto 2). |
| 12 | Listados y etiquetas referenciados (0,3) | **Completo** | Alta confianza (verificado directo, con matiz metodológico) | `07-implementacion.tex:106` ya muestra `@Procedure(procedureName = "sp_registrar_decision_verificacion")`. Conteo de `\label{}` = 47, de comandos `ref{` = 121 en total repartidos en 10 archivos — proporción que no sugiere huérfanas masivas (mis dos primeros intentos de contar huérfanas por regex fallaron por un problema de escape de caracteres en mi propio script, no por falta de referencias; no llegué a un cruce exacto etiqueta-por-etiqueta). |
| 13 | Catálogo de rutinas + cifra de observaciones (0,3) | **Completo** | Verificado directo | `db/procs/` tiene 26 archivos de rutina reales (28 archivos totales, menos `README.md` y el DDL `V8__...`) — coincide exactamente con lo que declara `CATALOGO-SP.md` ("veintiséis rutinas activas"). La cifra "28 de 30 = 93,3 %" aparece idéntica en `anexos.tex`, `INFORME-BRECHAS-ENTREGA-FINAL.md` y `OBSERVACIONES.md`. |
| 14 | Diccionario y procedencia de datos (0,3) | **Completo** | Verificado directo | Las 10 preguntas del SUS (Q1-Q10) y las 13 columnas de `jacoco.csv` — los dos huecos señalados originalmente — están ambos documentados en `DATA-DICTIONARY.md`. `DATA-PROVENANCE.md:15` ya no afirma inmutabilidad absoluta: reconoce explícitamente la corrección de `sus-raw.csv`. |
| 15 | Composición del equipo aclarada (0,3) | **Completo** | Verificado directo | Los mismos 4 nombres (Bone Arroyo, Carvajal Loor, Figueroa Morales, Rios Cuyabazo) con ORCID aparecen de forma idéntica en `CITATION.cff`, `CONTRIBUTORS.md`, `docs/informe-final/caratula.tex` y `13-declaraciones.tex`, con la misma explicación de la colaboración cruzada de Bone Arroyo. |
| E1 | Nombres del código en inglés (0,8) | **Riesgo alto de Nada** | Heredado del autoanálisis (el equipo lo admite en contra de su propio interés) | Ver riesgo #2 arriba. |
| E2 | Javadoc (0,8) | **Progreso real, nivel exacto incierto — probablemente Casi/A medias** | Verificado directo (conteos crudos), sin denominador preciso | `@param`/`@return`/`@throws` en todo `src/main/java` pasaron de **8/0/1** (línea base) a **1415/712/680** — un salto real y sustancial, mayor de lo que el propio autoanálisis reporta (que solo reclama controladores + interfaces de servicio). No pude calcular el % exacto de métodos públicos con Javadoc completo (parseo de firmas Java por regex no es confiable, y no hay `mvn` aquí para `mvn javadoc:javadoc`, que además es requisito explícito para "Completo"). Corran `mvn javadoc:javadoc` ustedes mismos antes de entregar: si falla, este punto no puede ser Completo aunque el % de cobertura sea alto. |
| E3 | Figuras en inglés (0,3) | **Completo (probable)** | Heredado del autoanálisis, no verifiqué las imágenes yo mismo | El equipo documenta 3 figuras traducidas con fuente Mermaid versionada junto a cada PNG. No abrí los PNG para confirmar visualmente. |

---

## Lo que pueden hacer en las próximas horas, en orden de impacto

1. **Decidir y ejecutar lo del tag `v1.0.0`** (sección de riesgos #1). Sin esto, nada de lo demás importa.
2. **Hacer algo — lo que sea, medible y honesto — sobre E1** para salir de "Nada". Documenten el porcentaje real antes/después.
3. **Clonar en limpio y correr `make docs`** para confirmar con sus propios ojos que el PDF sale sin `[?]` y con los tres índices — es el Piso 2 y el Punto 2 a la vez.
4. **Correr `mvn javadoc:javadoc`** y confirmar que termina en verde, y de paso obtener el % real de métodos públicos documentados (Punto E2).
5. **Recontar el abstract en inglés** con su propio método y ajustar si queda por debajo de 200 palabras (Punto 11).
6. **"Calentar" las dos URL de Render** unos minutos antes de la hora de cierre, y decidir si vale la pena mantenerlas despiertas durante la ventana de revisión (Punto 10).
7. Si hay tiempo, seguir cerrando el punto 6 (procedimientos almacenados) en los 12 archivos que aún usan `nativeQuery` hacia rutinas reales.

---

*Documento generado por Claude Code el 2026-09-11 como verificación externa de solo lectura, sin
modificar el repositorio. No sustituye la revisión del docente-director; su único objetivo es que
el equipo sepa, con evidencia directa, dónde está parado antes del cierre del examen.*
