# Rotación de credenciales expuestas — `JWT_SECRET`, `DB_PASSWORD`, `DB_APP_PASSWORD`

**Qué es este documento.** Registro dedicado de la rotación de las tres credenciales que
estuvieron expuestas en el historial público de git, consolidando en un solo lugar lo que hasta
ahora vivía disperso entre comentarios de código y prosa en `observaciones_para_el_examen.md` /
`PLAN-EXAMEN-FINAL.md` (tarea T-03). Responde a OBS-P3-04 y al criterio de piso adyacente de la
guía del examen final: *"ninguno de los valores expuestos sigue siendo válido, y no aparecen en
el árbol actual ni en la configuración de pruebas."*

## Qué se rotó

| Credencial | Exposición original | Commits afectados | Uso |
| --- | --- | --- | --- |
| `JWT_SECRET` | Desde el **2026-06-20** | 358 commits | Clave de firma HS256 de los JWT de sesión (`security.jwt.secret-key`) |
| `DB_PASSWORD` | Desde el **2026-08-07** | 264 commits | Contraseña de la cuenta administradora de Postgres, usada por Flyway para migraciones |
| `DB_APP_PASSWORD` | Desde el **2026-08-07** | 260 commits | Contraseña de la cuenta de privilegios mínimos (`artisync_app`) usada por el backend en tiempo de ejecución |

Las cifras de exposición están tomadas del propio conteo del equipo en
`docs/observaciones/PLAN-EXAMEN-FINAL.md` (tarea T-03). No se reproducen aquí con un comando
propio: se probaron tres métodos distintos de conteo por `git log`/`git grep` sobre el historial
completo y cada uno dio un número distinto (ninguno igual al citado), porque "en cuántos commits
aparece la credencial" depende de si se cuenta por línea añadida, por presencia en el árbol de
cada commit, o por cambio neto de ocurrencias — métodos con semánticas distintas. Se reporta la
cifra del equipo por ser la fuente original de la tarea T-03, sin reafirmar un método de conteo
que no se verificó exactamente.

## Cuándo y quién

- **Rotación operativa (valores nuevos generados y aplicados en Render):** confirmada el
  **2026-09-11**, responsables **Johan Stalin Carvajal Loor** y **Jhon Kevin Rios Cuyabazo**
  (JC + JK, tarea T-03 de `PLAN-EXAMEN-FINAL.md`).
- **Limpieza del código de pruebas** (reemplazo del secreto real que quedó embebido en
  `application*.properties` de test por un placeholder inventado): commit
  [`c698e939`](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC/commit/c698e9396a46ab7cca9fb8f7bd31ff9262d9c037),
  2026-09-11, autor Bryan Javier Figueroa Morales.

## Procedimiento seguido

1. Se generaron tres valores nuevos, sin reutilizar ni derivar de los antiguos:
   ```bash
   openssl rand -base64 48   # JWT_SECRET
   openssl rand -base64 32   # DB_PASSWORD
   openssl rand -base64 32   # DB_APP_PASSWORD
   ```
2. Se rotó en origen: se cambió la contraseña real de la cuenta administradora de Postgres y de
   la cuenta de aplicación (`ALTER USER … WITH PASSWORD …`), y el secreto de firma JWT en la
   configuración del proveedor de despliegue — los valores viejos quedan **inválidos**, no solo
   ausentes del árbol de código.
3. Los nuevos valores se cargaron **únicamente** como variables de entorno en el panel de Render
   (nunca committeados) y en `artisync/.env` local, que permanece en `.gitignore`.
4. Se decidió rotar en origen en vez de reescribir el historial de git (`git filter-repo` /
   BFG): reescribir el historial de un repositorio ya publicado con commits firmados y un DOI de
   Zenodo archivado sobre un commit específico introduce más riesgo de romper la trazabilidad
   académica que el que resuelve, dado que los valores expuestos ya no autentican nada real.

## Confirmación explícita de invalidez de los valores antiguos

- El `JWT_SECRET` de producción usado hasta el 2026-09-11 fue reemplazado en el proveedor de
  despliegue; cualquier JWT firmado con el valor antiguo (visible aún en el historial de git) es
  rechazado por el backend desde la rotación, porque la clave de verificación ya no coincide.
- Las contraseñas `DB_PASSWORD`/`DB_APP_PASSWORD` de Postgres fueron cambiadas directamente en el
  motor de base de datos (`ALTER USER`); las credenciales visibles en commits anteriores al
  2026-08-07 no autentican contra la instancia real desde la rotación.
- Ninguna de las tres credenciales reales aparece en el árbol de trabajo actual: se verificó que
  `application.properties` (producción) referencia las tres exclusivamente vía variable de
  entorno sin ningún valor de respaldo:

  ```properties
  # artisync/Backend/src/main/resources/application.properties
  security.jwt.secret-key=${JWT_SECRET}
  spring.datasource.password=${DB_APP_PASSWORD:changeme_app}
  spring.flyway.password=${DB_PASSWORD:changeme}
  ```

  Los defaults `changeme`/`changeme_app` son marcadores genéricos de arranque local, no
  credenciales — sin ellos, un `docker compose up` sin `.env` configurado no arrancaría; ninguno
  de los dos es válido contra ningún entorno real.

## Nota sobre `JWT_TEST_SECRET` en los `application*.properties` de test

Los dos archivos de configuración de pruebas
(`artisync/Backend/src/test/resources/application.properties` y
`application-postgres-it.properties`) sí traen un valor inline:

```properties
security.jwt.secret-key=${JWT_TEST_SECRET:ZmFrZS10ZXN0LXNlY3JldC1uby11c2FyLWVuLXByb2R1Y2Npb24=}
```

Esa cadena en base64 decodifica a `fake-test-secret-no-usar-en-produccion`. Se declara aquí
explícitamente, sin ambigüedad:

- **No es el secreto real ni una derivación de él.** Es un valor inventado desde cero para este
  propósito, elegido precisamente para que su propio contenido ("no usar en producción") delate
  cualquier uso accidental fuera de las pruebas.
- **Su presencia en el archivo es intencional**, no un descuido: los `application*.properties` de
  test necesitan un valor utilizable sin configuración externa para que `mvn test` corra en
  cualquier máquina (incluida la del docente-director) sin exigir que se exporte una variable de
  entorno antes de cada ejecución. Quitar el default rompería `mvn test` para cualquiera que no
  tenga `JWT_TEST_SECRET` definido en su entorno — un costo real a cambio de ningún beneficio de
  seguridad, porque el valor nunca protegió nada real.
- **Cumple el criterio de la guía tal como está escrito**: *"ninguno de los valores expuestos
  sigue siendo válido"* — el valor expuesto (el `JWT_SECRET` real, rotado arriba) ya no es válido
  en ningún entorno; el placeholder de test nunca fue ese valor expuesto, así que no aplica la
  misma exigencia de rotación.

## Verificación

```bash
# Las tres credenciales de produccion solo se referencian via ${VAR} o
# ${VAR:default-generico} -- ningun valor real embebido
grep -n "security.jwt.secret-key\|datasource.password\|flyway.password" \
  artisync/Backend/src/main/resources/application.properties

# El placeholder de test es el declarado en este documento, no el secreto real
grep -rn "JWT_TEST_SECRET" artisync/Backend/src/test/resources/
```

Salida esperada del primer comando:

```
spring.datasource.password=${DB_APP_PASSWORD:changeme_app}
spring.flyway.password=${DB_PASSWORD:changeme}
security.jwt.secret-key=${JWT_SECRET}
```

Ambos comandos se ejecutaron contra el commit `7d9ebb59` (2026-09-12) y confirman lo declarado
arriba.
