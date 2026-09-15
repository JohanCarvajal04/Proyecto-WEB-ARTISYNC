# Revisión externa (Claude Code) — Guía del examen suspenso, 2026-09-14

Contraste independiente entre `Guia_ExamenSuspenso_ARTISYNC.pdf` (Dr. Gleiston Cicerón Guerrero
Ulloa, cierre viernes 18-sep-2026 23:55, último commit revisado por el docente: `a92629cd`) y el
estado real del repositorio en el commit `48af094b` (HEAD de `main` al momento de esta revisión).

Metodología: cada punto se comprobó de forma directa (grep, lectura de fuente, reproducción de
los scripts de `scripts/`, conteos propios) — no se repitió de memoria lo que dice `VERIFICACION.md`.
Donde reproduje un resultado ya capturado ahí, lo digo explícitamente.

---

## 0. Veredicto en una frase

**No.** El trabajo técnico de los 14 puntos está, en su mayoría, genuinamente resuelto en el
código — pero **la etiqueta `v1.1.0` sigue apuntando al 1-sep-2026, 248 commits detrás de HEAD y
detrás incluso del commit que el docente ya evaluó (`a92629cd`)**. Por el Aviso 4 de la guía
("Lo que no esté dentro de la etiqueta no existe"), si no mueven la etiqueta antes del cierre,
**nada de lo descrito abajo cuenta**: el docente calificaría contra el snapshot de hace dos
semanas. Esto no es un punto más de la tabla — es el interruptor maestro de toda la nota.

Con la etiqueta movida y los huecos reales de abajo cerrados, mi estimación es **~9,04/10** sobre
la rúbrica de la guía (ver §5). Sin mover la etiqueta: la nota se calcula contra el commit de
hace 248 commits, que ni siquiera es el que ya generó esta guía.

---

## 1. Los 4 pisos (criterios de cero)

| Piso | Estado | Evidencia |
|---|---|---|
| **1** — repo público + `v1.1.0` existe y apunta antes del cierre | ⚠️ Cumple la letra, no el espíritu | La etiqueta existe y es anterior al 18-sep (creada 2026-09-01), pero ver el problema de fondo en §0 y §2 (EV-3). No verifiqué en el navegador que el repo abra sin sesión — confírmenlo ustedes mismos en una ventana privada. |
| **2** — el informe se regenera desde el README | ✅ Sin cambios | No se tocó el pipeline de compilación desde que el docente lo verificó (sección 1 de la guía, ya resuelta). |
| **3** — ningún dato inventado | ✅ Sin hallazgos de invención | El propio `VERIFICACION.md` documenta la corrección de `RALPH2021` (P11) en cuanto se detectó, no la ocultó. La cultura del equipo de admitir huecos (SUS 61.25 vs 76.88, rutinas SP retiradas) se sostiene en esta ronda. Ver eso sí el hueco de honestidad más fino que señalo en P11 (§4). |
| **4** — sin commits ajenos ni correos falsos | ✅ Sin hallazgos | `git log` muestra correos institucionales (`@uteq.edu.ec`) consistentes en los commits de esta ronda. |

---

## 2. Los 4 entregables obligatorios (EV)

| EV | Estado | Evidencia |
|---|---|---|
| **EV-1** — `VERIFICACION.md` | ✅ Presente y bien construido | Cubre los 14 puntos, cada uno con orden + salida real pegada + ruta de archivo, tal como exige la guía. Reproduje 6 de las 14 salidas de forma independiente (P2, P3, P6, P7, P8) y coinciden con lo que el archivo declara. |
| **EV-2** — `make verify` | ✅ Ejecutado end-to-end tras esta conversación, `EXIT_CODE=0` | El target (`Makefile:443`) encadena `mvn test` → cobertura P5 → conteo P6 → DOI P11 → sync-procs → javadoc P7. **Actualización (misma tarde, 2026-09-14):** el binario `make` no estaba instalado en la shell de esta revisión, así que se corrieron los 6 comandos exactos del target a mano, en el mismo orden y con el mismo criterio de corte: 1448/1448 tests (`BUILD SUCCESS`, 2:46 min), cobertura 9/9 paquetes ≥70%, `nativeQuery=true`: 0, 28/28 DOI resueltos, `sync-procs --check` sincronizado, `mvn javadoc:javadoc` (`BUILD SUCCESS`). Log completo en [`docs/mediciones/make-verify-20260914.txt`](../mediciones/make-verify-20260914.txt), resumen en `VERIFICACION.md`. **Pendiente real:** confirmar que `make verify` también funciona invocado por el binario `make` de verdad (en esta shell no estaba instalado) y, si se quiere el máximo rigor, repetirlo desde un clon limpio nuevo en vez de este working tree. |
| **EV-3** — etiqueta + URL en el README | ❌ Incompleto en dos frentes | (a) La etiqueta no se movió (ver §0). (b) **La URL pública del despliegue no está en la primera pantalla del README** — solo aparece en la línea 181 (`make lighthouse`) y 283 (evidencia empírica), no en la introducción/cabecera donde la guía pide que esté. Es un fix de una línea. |
| **EV-4** — `CONTRIBUCIONES.md` | ❌ Explícitamente borrador | El propio archivo dice "Borrador" en la línea 3 y tiene la columna "Responsable" vacía para los tres integrantes, sin firma ni correo institucional. Además no cubre P1, P2, P3, P9, P10 ni P12 (dice que "ya estaban resueltos antes de esta ronda", lo cual es plausible pero no está declarado con el mismo detalle que P4–P8/P11/P13/P14). Sin esto firmado, la guía es explícita: la calificación individual (sección 4 de la guía, 35% "titularidad") no tiene base. |

---

## 3. Sección 1 de la guía (los 10 puntos "ya resueltos", peso cero) — re-chequeo rápido

La guía advierte: si al cerrar un pendiente rompen algo de esta lista, ese punto deja de estar
resuelto. Repasé los que tocan archivos modificados en esta ronda:

- **CATALOGO-SP.md / 28 rutinas de `db/procs`**: el archivo fue editado en el commit más reciente
  (`48af094b`, "Update CATALOGO-SP.md"). Encontré 27 archivos `.sql` en `db/procs/` (26 rutinas
  reales + 1 DDL de soporte que el propio documento aclara que no cuenta) + 2 rutinas de
  verificación asistida por IA fuera de `db/procs` = 28. **Cuadra**, no se rompió.
- **DATA-PROVENANCE.md / 7 hashes**: verifiqué que los 7 commits citados (`f05feeb`, `1b34b8d`,
  `d07656b`, `69e43b8`, `8b88512`, `e84ab50`, `a6746df`) existen en el historial (`git cat-file -e`
  sobre cada uno). **Cuadra**.
- **CORS sin comodines**: `SecurityConfig.java:36` sigue con
  `${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}` — sin `*`. **Cuadra**.
- **Resumen en español (206 palabras)**: el conteo actual sobre el `.tex` da 231 palabras (ver P10
  abajo) — cambió pero sigue dentro del rango 200–250, así que no se rompió, solo se movió dentro
  del rango permitido.

No encontré indicios de que el resto de la sección 1 (CONTRIBUTORS.md, k6, validador de
trazabilidad, HTTPS/cabeceras del despliegue) se haya tocado en esta ronda.

---

## 4. Los 14 pendientes — estado real, uno por uno

Para cada uno: lo que reproduje yo mismo (marcado **[verificado por mí]**) vs. lo que solo
contrasté contra `VERIFICACION.md` sin reproducir (marcado **[según expediente, no reproducido]**),
y mi estimación honesta del porcentaje según la rúbrica de la guía (100/70/40/15/0).

### P1 — Cookie de refresco sin `Secure` (peso 0,7)
**[verificado por mí]** `AuthController.java:190-194` crea la cookie con `.httpOnly(true)`,
`.secure(cookieSecure)` (con `cookieSecure` inyectado por `@Value("${app.security.cookie-secure:true}")`,
es decir **`true` por defecto**, solo desactivable explícitamente para pruebas HTTP locales) y
`.sameSite("Strict")`. El código está correcto.

**Hueco real:** la guía pide "la cabecera Set-Cookie capturada **del despliegue**" y
`VERIFICACION.md` lo admite sin rodeos: la captura que tienen es contra el backend **local**, no
contra `artisync-frontend.onrender.com` (que aún no había redesplegado ese commit al momento de la
corrida). Falta ese `curl -i` contra la URL pública.

**Estimación: 70%** (terminado, defecto menor de evidencia — falta repetirlo contra prod después
de mover la etiqueta, como el propio expediente ya se recuerda a sí mismo hacer).

### P2 — 133 de 184 etiquetas sin referenciar (peso 0,8)
**[verificado por mí]** Reproduje `python scripts/auditoria-rubrica.py p12`: **62 `\label` totales
en todo `docs/`, 0 huérfanas**. Cumple literalmente el criterio de cierre ("cero etiquetas
huérfanas, con script que sale con error si queda alguna").

**Riesgo a vigilar:** la cifra que encontró el docente (184 etiquetas, 133 sin referenciar) es muy
distinta de las 62 que hay ahora. Puede ser una limpieza real (menos figuras/tablas/listados
declarados) o una diferencia de metodología de conteo entre su herramienta y este script. No pude
reconciliar la diferencia en el tiempo de esta revisión — antes de confiar en el 100%, hagan una
pasada visual rápida del PDF de 79 páginas buscando alguna figura/tabla obviamente sin mencionar
en el texto.

**Estimación: 85%** (el criterio automatizado se cumple de forma reproducible; la salvedad de
arriba es la única razón para no poner 100%).

### P3 — DOI de marcador sin sustituir (peso 0,6)
**[verificado por mí]** `grep -rn "1234567" docs/informe-final/referencias.bib` → sin resultados.
El placeholder ya no existe. Además reproduje `auditoria-rubrica.py p3`: las 8 claves de la tabla
comparativa de trabajos relacionados tienen las 8 con campo `doi`.

**Estimación: 100%.**

### P4 — Contraseña del keystore en claro (peso 0,5)
**[verificado por mí]** `docker-compose.medicion.yml:18` → `TLS_KEYSTORE_PASSWORD: ${TLS_KEYSTORE_PASSWORD:-changeit}`,
y `application-medicion.properties:11` con el mismo patrón. El valor sale del entorno; `changeit`
queda solo como default de desarrollo, no como secreto fijo.

**Estimación: 100%.**

### P5 — Cobertura desigual por paquete (peso 0,9)
**[verificado por mí, actualizado]** Corrí `mvn test` completo (1448/1448 tests, `BUILD SUCCESS`)
y reproduje `verificar-cobertura-controladores.py` sobre el `jacoco.csv` recién generado: los 9
paquetes `controller.*` ≥70% en líneas y ramas (el más bajo, `security`, 84.9%/89.3%). `jacoco.csv`
no está versionado (`target/` en `.gitignore`), se regenera en el paso 1 de `make verify` — así que
es reproducible desde un clon limpio por construcción, y ahora también confirmado en la práctica.

**Estimación: 100%.**

### P6 — 27 consultas fuera del mecanismo exigido (peso 0,9)
**[verificado por mí]** Reproduje `auditoria-rubrica.py p6`: **`nativeQuery=true`: 0**, `@Procedure`: 3
(en `UserRepository`/`AiCertificateRepository`, uso legítimo), `@NamedStoredProcedureQuery`: 0. Las
23 rutinas migradas ahora usan `NamedParameterJdbcTemplate` en el patrón `*RepositoryCustom`/`*RepositoryImpl`
que el proyecto ya usaba en `ContractRepository`. `VERIFICACION.md` además documenta 1448/1448 tests
en verde y pruebas de integración reales contra Postgres del login y otras rutinas críticas.

**Estimación: 100%.**

### P7 — Javadoc al 66,7% (peso 1,2 — el punto más pesado de la tabla)
**[verificado por mí]** Reproduje `auditoria-rubrica.py e2`: **1152/1166 (98.8%)** en todas las
capas de `src/main/java`, muy por encima del 90% exigido. Por capa: `service` 99.8%, `controller`
100%, `repository` 95.1%, `config`/`security`/`exception`/`util` 100%, `scheduler` 95.0%.

**Actualizado:** también corrí `mvn javadoc:javadoc` — `BUILD SUCCESS` en 30.6s, sin errores de
doclint.

**Estimación: 100%.**

### P8 — Nombres en español en el código (peso 1,0)
**[verificado por mí]** Reproduje `auditoria-rubrica.py e1`: **553 tipos, 0% en español**; **1011
métodos, 23 con token en español (2.3%)** — ambos muy por debajo del 5% exigido. Los 23 residuales
están concentrados en las clases `*RepositoryImpl` nuevas de P6 (nombres de firma preexistentes que
se mantuvieron a propósito para no tocar llamadores fuera de alcance).

**Estimación: 100%.**

### P9 — Figuras rotuladas en español (peso 0,7)
**[verificado por mí, parcialmente]** El modelo C4 (`docs/diagramas/workspace.dsl`) tiene sus
rótulos visibles en inglés ("Comprehensive management system...", "Payment Gateway...", "Relational
Database"). El único gráfico generado que encontré (`docs/mediciones/sus/graficar-sus.py`, que
produce `boxplot-sus.png`) usa `plt.title("Distribution of SUS Scores (n=16)")` y
`plt.ylabel("Score (0-100)")` — en inglés. No inspeccioné imagen por imagen todo el árbol de
figuras (algunas secciones de resultados usan tablas LaTeX, no imágenes, para JaCoCo/Lighthouse).

**Estimación: 90%.**

### P10 — Abstract de 183 palabras (peso 0,3)
**[verificado por mí]** Reproduje `auditoria-rubrica.py p11`: Resumen 231 palabras, Abstract 213
palabras — ambos dentro de 200–250.

**Estimación: 100%.**

### P11 — Referencias sin verificar una por una (peso 0,7)
**[verificado por mí, y aquí encontré el hueco más real de toda la revisión]**
`referencias.bib` tiene **45 entradas totales**, de las cuales **solo 28 declaran campo `doi`**
(el resto — `ISO29148`, `ISO25010`, `OWASP2021`, `OWASPSQLI`, `POHL2010`, `WIEGERS2013`,
`INCOSE2023`, `COCKBURN2000`, `BROWN2018`, `FIELDING2000`, `NYGARD2011`, `BROOKE1996`, `BANGOR2009`,
`KITCHENHAM2007`, `HEVNER2004`, `BASILI1994`, `COELLO2023` — en su mayoría estándares y obras
fundacionales de ingeniería de software que legítimamente no tienen DOI). `VERIFICACION.md` solo
verificó los 28 que sí tienen DOI ("28 DOI verificados, 0 fallidos") y no menciona los otros 17 en
ningún lado.

**El problema:** la guía pide literalmente *"Las 48 referencias con su DOI resuelto"*. Ni el
conteo cuadra (45 en el `.bib`, 42 claves realmente citadas en el texto, ni una cifra llega a 48)
ni está resuelto el 100% de las que sí existen — solo el subconjunto que ya tenía DOI. Esto no es
un dato inventado (Piso 3 sigue a salvo), pero **tal como está redactado hoy el expediente, parece
más completo de lo que es** — exactamente el tipo de brecha que este proyecto normalmente prefiere
declarar en vez de disimular (ver `[[project-artisync-academic-context]]`).

**Qué hace falta para cerrarlo de verdad:** (1) reconciliar por qué son 45/42 y no 48 — revisen si
faltan referencias por citar o si el número de la guía incluye algo que ya no está; (2) para las 17
sin DOI, declarar explícitamente en el propio `.bib` o en `VERIFICACION.md` *por qué* no tienen DOI
(estándar ISO/IEEE sin DOI público, libro pre-DOI, etc.) en vez de omitirlas en silencio.

**Actualizado:** volví a correr `verificar-doi.py` completo (no solo leer el archivo de salida
guardado) — los 28 DOI que sí están declarados resuelven en vivo contra doi.org, cada uno con
título y venue reales recuperados (ver `docs/mediciones/make-verify-20260914.txt`, paso 4/6). Esto
sube la confianza en el subconjunto de 28, pero no cambia el hueco de fondo: las 17 referencias sin
campo `doi` siguen sin verificar ni justificar.

**Estimación: 45%** (parcial — lo que se verificó, se verificó bien; pero el alcance declarado no
cubre lo que la guía pidió).

### P12 — Lighthouse sin informes utilizables (peso 0,7)
**[verificado por mí]** `find docs/mediciones/lighthouse -iname "*.json" | wc -l` → **108** archivos
(muy por encima del mínimo de 6 que exige la guía: 3 corridas × 2 perfiles). Verifiqué que al menos
uno declara `"requestedUrl":"https://artisync-frontend.onrender.com/explorar"` — apunta al
despliegue público real, no a localhost.

**Estimación: 100%.**

### P13 — Cuaderno de análisis sin ejecutar (peso 0,4)
**[verificado por mí]** Parseé `docs/mediciones/reproduccion.ipynb`: **4 celdas de código, las 4
con salida guardada** (`outputs` no vacío en ninguna).

**Estimación: 100%.**

### P14 — Consentimientos informados del SUS (peso 0,6)
**Actualizado tras esta conversación (2026-09-14, más tarde el mismo día).** Las 16 filas de
`registro-consentimientos.csv` estaban vacías porque quien armó el andamiaje de P14 no revisó que
la constancia **ya existía** desde el 2026-09-04 (commit `06d1a364`, Bryan Figueroa) en
`docs/mediciones/sus/REPORTE-SUS.md`, sección "Referencias de consentimiento": los 16 códigos, la
fecha (2026-08-16) y el hash SHA-256 del consentimiento firmado y escaneado de cada participante.
Se transcribió esa evidencia al CSV (no se inventó ningún dato nuevo — ver el detalle de
procedencia en `docs/etica/consentimientos/README.md` y en `VERIFICACION.md`).

**Lo que sigue sin poder verificarse desde aquí:** los archivos físicos viven en `G:\EPSCAN\` en
el equipo de Bryan, no en el repositorio (correctamente, por privacidad) — nadie en esta revisión
pudo recalcular los 16 SHA-256 contra esos PDF para confirmar que la tabla de `REPORTE-SUS.md`
corresponde a documentos reales. Eso solo lo puede cerrar quien tenga acceso a esa carpeta,
pegando la salida de `sha256sum p0.PDF ... p15.PDF` en `VERIFICACION.md`.

**Estimación: 70%** (subió de 15%: el objetivo del punto — constancia real, no inventada, de que
cada participante aceptó — se cumple con evidencia ya existente y trazable; el defecto menor
pendiente es la re-verificación física de los hash contra los PDF, no la falta de constancia en
sí).

---

## 5. Nota estimada bajo la rúbrica de la guía

Esto es **mi estimación**, no la del docente — la guía es explícita en que el porcentaje lo decide
su propia comprobación, no la mía. Uso los números de §4:

| # | Peso | % estimado | Puntos |
|---|---:|---:|---:|
| P1 | 0,7 | 70% | 0,49 |
| P2 | 0,8 | 85% | 0,68 |
| P3 | 0,6 | 100% | 0,60 |
| P4 | 0,5 | 100% | 0,50 |
| P5 | 0,9 | 100% | 0,90 |
| P6 | 0,9 | 100% | 0,90 |
| P7 | 1,2 | 100% | 1,20 |
| P8 | 1,0 | 100% | 1,00 |
| P9 | 0,7 | 90% | 0,63 |
| P10 | 0,3 | 100% | 0,30 |
| P11 | 0,7 | 45% | 0,32 |
| P12 | 0,7 | 100% | 0,70 |
| P13 | 0,4 | 100% | 0,40 |
| P14 | 0,6 | 70% | 0,42 |
| **Total** | **10,0** | | **≈ 9,04 / 10** |

**Esto asume que `v1.1.0` se mueve al commit final antes del cierre.** Si no se mueve, la nota que
importa es la que corresponda al snapshot de hace 248 commits, no esta tabla.

---

## 6. Qué hacer antes del viernes 18-sep 23:55, en orden de impacto

1. **Mover la etiqueta `v1.1.0`** al commit que van a defender — literalmente lo único que decide
   si algo de lo de arriba cuenta o no (Aviso 4). Háganlo después de cerrar todo lo demás, como ya
   planea `CONTRIBUCIONES.md`, pero no lo dejen para último minuto: el Piso 4/criterio de "plazo
   interno" penaliza entregas concentradas en la madrugada del viernes.
2. **Cerrar P14 de verdad**: quien tenga acceso a `G:\EPSCAN\` debe recalcular los 16 SHA-256
   contra los PDF reales y pegar esa salida en `VERIFICACION.md` — la constancia y la
   transcripción ya están hechas (ver §4), solo falta esa re-verificación física.
3. **Arreglar P11**: reconciliar el conteo de referencias (45/42 vs 48) y decidir qué hacer con las
   17 sin DOI — declarar la razón en vez de omitirlas.
4. **Completar `CONTRIBUCIONES.md` (EV-4)**: nombres, correos institucionales y firma real de los
   tres integrantes — sin esto no hay base para la nota individual (35% de la sección 4).
5. **Mover la URL pública del despliegue a la primera pantalla del README (EV-3)** — un cambio de
   una línea.
6. **Recapturar la evidencia de P1 contra el despliegue público** (no localhost) después de mover
   la etiqueta y confirmar el redespliegue.
7. **EV-2 ya se corrió completo y pasó (`EXIT_CODE=0`, ver `docs/mediciones/make-verify-20260914.txt`)** —
   falta solo confirmar que el binario `make` esté instalado en la máquina desde la que van a
   defender (en esta shell no lo estaba) y, si quieren el máximo rigor, repetirlo una vez más
   desde un clon limpio nuevo, como recuerda el "Ojo con esto" de la sección 1.
8. Antes de entregar, una pasada visual rápida sobre el PDF de 79 páginas buscando alguna
   figura/tabla sin mencionar en el texto (para descartar la duda de P2 del §4).
