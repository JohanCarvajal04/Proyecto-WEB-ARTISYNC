# Plan de rutinas almacenadas con garantías ACID y control de concurrencia — Módulo de Seguridad

> Análisis del módulo de seguridad (10 entidades, 10 repositorios, 6 servicios, 7 controladores)
> y plan por fases para trasladar al motor la lógica que hoy vive en Java sin protección
> frente a anomalías de concurrencia.
>
> Complementa `docs/adr/adr-006-estrategia-acceso-datos.md` y `docs/basedatos/CATALOGO-SP.md`.

---

## 0. Marco de trabajo: qué significa aquí "BEGIN / COMMIT / ROLLBACK"

Antes del catálogo hay que fijar una precisión técnica de PostgreSQL, porque condiciona
cómo se escribe **cada** rutina de este plan.

### 0.1 Una FUNCTION no puede abrir ni cerrar transacciones

En PostgreSQL, una `FUNCTION` se ejecuta **siempre dentro de la transacción del llamante**.
Escribir `BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK` dentro de un `CREATE FUNCTION ... LANGUAGE plpgsql`
es un **error en tiempo de ejecución** (`2D000 invalid_transaction_termination`). No es una
cuestión de estilo: el motor lo rechaza.

Esto **no** significa que no haya control transaccional. Significa que el control transaccional
está repartido en **tres niveles**, y los tres se usan en este plan:

| Nivel | Quién lo emite | Sentencia real | Qué garantiza |
|---|---|---|---|
| **1. Demarcación externa** | El llamante (Spring `@Transactional` → JDBC) | `BEGIN;` … `COMMIT;` / `ROLLBACK;` | Atomicidad de todo el caso de uso |
| **2. Subtransacción interna** | El bloque `BEGIN … EXCEPTION … END` de plpgsql | `SAVEPOINT` / `ROLLBACK TO SAVEPOINT` implícitos | Atomicidad parcial y traducción de errores |
| **3. Atomicidad de sentencia** | El propio motor | implícita en cada `UPDATE`/`INSERT` | Ninguna sentencia deja estado a medias |

La regla operativa de este plan es:

> **Toda rutina de escritura se invoca dentro de una transacción explícita, y su cuerpo
> declara un bloque `EXCEPTION` que actúa como punto de rollback interno.**

### 0.2 Cómo se ve la demarcación explícita en cada capa

**En SQL puro** (así se prueba y se documenta cada rutina):

```sql
BEGIN;                                              -- inicio explícito
  SELECT fn_sincronizar_roles_usuario(42, ARRAY['CREADOR','CLIENTE']);
COMMIT;                                             -- confirma
-- si la función lanza RAISE EXCEPTION, la sesión queda en estado abortado
-- y la única salida válida es:
ROLLBACK;
```

**En Java** — `@Transactional` **es** ese `BEGIN`/`COMMIT`. Spring emite
`connection.setAutoCommit(false)` al entrar y `commit()` al salir, o `rollback()` si sale una
`RuntimeException`. No hay que (ni se puede) escribirlo a mano:

```java
@Override
@Transactional(isolation = Isolation.READ_COMMITTED)   // ← BEGIN
public UserResponse assignRoles(Long id, AssignRolesRequest request) {
    try {
        usuarioRolRepository.sincronizarRoles(id, request.getRoles().toArray(new String[0]));
    } catch (RuntimeException e) {
        throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST); // ← ROLLBACK
    }
    ...
}                                                       // ← COMMIT
```

**Dentro de la rutina** — el bloque `EXCEPTION` es el punto de rollback:

```sql
BEGIN                                    -- abre subtransacción (SAVEPOINT implícito)
    UPDATE ...;
    INSERT ...;
EXCEPTION
    WHEN unique_violation THEN           -- ROLLBACK TO SAVEPOINT automático
        RAISE EXCEPTION '...' USING ERRCODE = '23505';
END;
```

### 0.3 Cuándo sí se usa CREATE PROCEDURE con COMMIT real

Un `PROCEDURE` invocado con `CALL` **fuera** de un bloque de transacción sí puede emitir
`COMMIT` y `ROLLBACK`. Es el caso de las **purgas por lotes** (§7): confirmar cada lote evita
una transacción de larga duración que bloquearía `VACUUM` y haría crecer los *dead tuples*.

Advertencia para la capa Java: un procedimiento con `COMMIT` **falla** si se le llama desde un
método `@Transactional`, porque Spring ya abrió la transacción. Debe invocarse con
`@Transactional(propagation = Propagation.NOT_SUPPORTED)` o vía `JdbcTemplate` fuera de toda
transacción.

### 0.4 Nivel de aislamiento vigente en el proyecto

Verificado: **no hay ninguna configuración de aislamiento** en `application.yml`, ni en el
`DataSource`, ni en ninguna anotación `@Transactional`. El proyecto opera por tanto en el
**valor por defecto de PostgreSQL: `READ COMMITTED`**.

Todo el análisis de anomalías de este documento se hace **bajo READ COMMITTED**, que es el
escenario real. Bajo ese nivel PostgreSQL garantiza:

| Anomalía | ¿Ocurre en READ COMMITTED? | Mitigación aplicable |
|---|---|---|
| Lectura sucia (*dirty read*) | **No** — PostgreSQL nunca la permite, en ningún nivel | — |
| **Lectura no repetible** | **Sí** | `SELECT … FOR UPDATE` / `FOR SHARE`, o leer una sola vez |
| **Lectura fantasma** | **Sí** | Restricción `UNIQUE`, `pg_advisory_xact_lock`, o `SERIALIZABLE` |
| **Actualización perdida** | **Sí** | `FOR UPDATE`, o `UPDATE … WHERE <predicado>` atómico |
| Sesgo de escritura (*write skew*) | **Sí** | Solo `SERIALIZABLE` lo elimina; si no, bloqueo explícito |

Un matiz de PostgreSQL que este plan explota de forma deliberada: bajo READ COMMITTED, cuando
un `UPDATE` encuentra una fila que otra transacción modificó y confirmó, **re-evalúa la cláusula
`WHERE` sobre la versión nueva** (*EvalPlanQual*). Por eso un
`UPDATE … WHERE usado = FALSE RETURNING …` es **inmune a la actualización perdida sin
necesidad de bloqueo previo**: si la otra transacción ya puso `usado = TRUE`, la re-evaluación
falla y la fila no se actualiza. Es la técnica central de las rutinas §2 y §6.

### 0.5 Convenciones comunes a todas las rutinas

1. `CREATE OR REPLACE`, un archivo por rutina en `db/procs/`, propagado con `make sync-procs`.
2. Cero SQL dinámico; solo parámetros formales tipados (verificado por `scripts/audit-sql-dynamic.sh`).
3. `RAISE EXCEPTION … USING ERRCODE` con los códigos que ya mapea
   `StoredProcedureExceptionTranslator`: `23505`→409, `23514`/`22004`/`23503`→400, `P0002`→404.
4. **Orden de bloqueo canónico** para evitar interbloqueos: siempre
   `usuarios` → `usuario_roles` → `roles` → `rol_permisos`, y dentro de cada tabla por
   **clave primaria ascendente** (`ORDER BY id … FOR UPDATE`).
5. Las rutinas de escritura son `VOLATILE` (por defecto); las de consulta, `STABLE`.

---

## 1. Anomalías detectadas hoy en el módulo (línea base)

Antes de proponer, el inventario de lo que está roto **ahora mismo** bajo READ COMMITTED:

| # | Ubicación | Anomalía | Consecuencia real |
|---|---|---|---|
| A1 | `TwoFactorServiceImpl.validarCodigoOBackup` | **Actualización perdida** | Un código de respaldo 2FA de un solo uso se consume **dos veces**: ambas peticiones leen `usado = FALSE` y ambas escriben `TRUE`. Bypass de segundo factor. |
| A2 | `AdminUserServiceImpl.actualizarRoles` | **Lectura fantasma** + escritura no atómica | `deleteAll` + bucle de `save`. Sin `UNIQUE` en `usuario_roles(id_usuario, id_rol)`, dos peticiones concurrentes dejan **roles duplicados**; si falla a mitad del bucle, el usuario queda **sin ningún rol**. |
| A3 | `AdminUserServiceImpl.createUser` | **Lectura fantasma** | `existsByCorreo` y el `save` no son atómicos. Se salva porque `usuarios.correo` es `UNIQUE`, pero el usuario recibe un **500 crudo** en vez del 409 esperado. |
| A4 | `TwoFactorServiceImpl.setup2Fa` | Escritura no atómica (10 viajes) | Fallo a mitad ⇒ secreto TOTP nuevo con **códigos de respaldo incompletos**. |
| A5 | `AuthServiceImpl.forgotPassword` | Acumulación no controlada | No invalida tokens previos: **N tokens de recuperación válidos** simultáneamente por usuario. |
| A6 | `SessionRevocationService.revocarSesionesUsuario` | **Lectura no repetible** | `findByUsuarioIdUsuario` y `deleteByUsuarioIdUsuario` son dos sentencias: una sesión creada entre ambas **se borra de la base sin revocarse en Redis** ⇒ JWT vivo y no rastreable. |
| A7 | `UserServiceImpl.changePassword` | **Actualización perdida** | Lee el hash, compara en Java, escribe. Dos cambios concurrentes: **el segundo pisa al primero** sin detectarlo. |
| A8 | `RolePermissionServiceImpl.createRole` | **Lectura fantasma** | Igual que A3, mitigado por `roles.nombre_rol UNIQUE` pero con error 500. |
| A9 | `PaisServiceImpl.updatePais` | **Lectura fantasma** | La comprobación de nombre duplicado no es atómica respecto al `save`. |
| A10 | *transversal* | Sin purga | `sesiones_usuario`, `tokens_recuperacion` y `codigos_respaldo_2fa` **crecen sin límite**. |

Además, **rendimiento** (no es anomalía, pero motiva las mismas rutinas):

- `CustomUserDetailsService.loadUserByUsername` se ejecuta **en cada petición autenticada**
  (`JwtAuthenticationFilter:79`) y cuesta **4–8 consultas** (N+1 por `Rol.permisos` con `FetchType.EAGER`).
- `UsuarioMapper.toUserResponse` se invoca por cada fila de la página ⇒
  **~90 consultas** para `GET /api/v1/admin/usuarios?size=20`.

---

## 2. `fn_consumir_codigo_respaldo_2fa` — corrige A1 (actualización perdida)

**Prioridad: P0 (defecto de seguridad explotable).**

### Problema

```java
List<CodigoRespaldo2Fa> codigos = repo.findByUsuarioIdUsuarioAndUsadoFalse(id);  // T1 y T2 leen usado=FALSE
for (CodigoRespaldo2Fa r : codigos) {
    if (r.getCodigoHash().equals(hashIngresado)) {
        r.setUsado(true); repo.save(r);                                           // T1 y T2 escriben TRUE
        return true;                                                              // ambas devuelven true
    }
}
```

Es el patrón *read-modify-write* de libro. Además saca **todos** los hashes de respaldo del motor.

### Solución

Una única sentencia `UPDATE … WHERE usado = FALSE RETURNING`. Bajo READ COMMITTED, la
re-evaluación EvalPlanQual (§0.4) garantiza que la segunda transacción **no encuentre fila**.
No hace falta `FOR UPDATE` previo: la propia sentencia toma el bloqueo de fila.

```sql
CREATE OR REPLACE FUNCTION fn_consumir_codigo_respaldo_2fa(
    p_id_usuario  BIGINT,
    p_codigo_hash VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_codigo BIGINT;
BEGIN
    IF p_id_usuario IS NULL OR p_codigo_hash IS NULL THEN
        RAISE EXCEPTION 'fn_consumir_codigo_respaldo_2fa: parametros obligatorios'
            USING ERRCODE = '22004';
    END IF;

    -- Atomicidad de sentencia + EvalPlanQual: si otra transaccion concurrente ya
    -- marco este codigo como usado, el predicado usado = FALSE se re-evalua sobre
    -- la version confirmada y NO devuelve fila. Actualizacion perdida imposible.
    UPDATE codigos_respaldo_2fa
       SET usado = TRUE
     WHERE id_usuario = p_id_usuario
       AND codigo_hash = p_codigo_hash
       AND usado = FALSE
    RETURNING id_codigo INTO v_id_codigo;

    RETURN v_id_codigo IS NOT NULL;
END;
$$;
```

| Aspecto | Cómo se cumple |
|---|---|
| **Atomicidad** | Sentencia única; el `RETURNING` sólo ve el efecto confirmado |
| **Consistencia** | Invariante "un código de respaldo se usa una sola vez" pasa a ser estructural |
| **Aislamiento** | Bloqueo de fila implícito del `UPDATE` + EvalPlanQual |
| **Durabilidad** | WAL del `COMMIT` del llamante |
| **Actualización perdida** | **Eliminada** por el predicado `usado = FALSE` |
| **Lectura no repetible** | No aplica: no hay lectura previa que releer |

**Índice de apoyo requerido** (además convierte el fallo en un no-op barato):

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_codigo_respaldo_usuario_hash
    ON codigos_respaldo_2fa (id_usuario, codigo_hash);
```

**Demarcación en el llamante:**

```java
@Transactional   // BEGIN … COMMIT
public boolean validarCodigoOBackup(String correo, String codigo) {
    ...
    return codigoRespaldo2FaRepository.consumirCodigoRespaldo(idUsuario, hashSha256(codigo));
}
```

---

## 3. `fn_sincronizar_roles_usuario` — corrige A2 (fantasma + no atomicidad)

**Prioridad: P1.**

### Problema

`actualizarRoles()`: `findByUsuarioIdUsuario` → `deleteAll` → `flush` → por cada rol
(`findByNombreRol` + `save` + consulta de perfil + `save` de perfil). **~10 viajes**, y con dos
administradores editando el mismo usuario a la vez el resultado son roles duplicados o ninguno.

### Solución

Gemela exacta de `fn_sincronizar_permisos_rol` (REQ-F-003), que ya resolvió este patrón para
roles↔permisos. Tres defensas apiladas:

1. **`SELECT … FOR UPDATE` sobre `usuarios`** — serializa a los dos administradores. El segundo
   espera; nunca lee un estado intermedio (elimina la lectura no repetible).
2. **`UNIQUE (id_usuario, id_rol)`** — cierra el fantasma de forma estructural: aunque alguien
   inserte por fuera de la rutina, el índice único rechaza el duplicado.
3. **Bloque `EXCEPTION`** — punto de rollback interno que traduce el error al código HTTP correcto.

```sql
CREATE OR REPLACE FUNCTION fn_sincronizar_roles_usuario(
    p_id_usuario  BIGINT,
    p_nombres_rol TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_existe        BOOLEAN;
    v_nombre_rol    TEXT;
    v_total         INTEGER := 0;
BEGIN
    IF p_id_usuario IS NULL OR p_nombres_rol IS NULL OR array_length(p_nombres_rol, 1) IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_roles_usuario: se requiere al menos un rol'
            USING ERRCODE = '22004';
    END IF;

    -- (1) Bloqueo del agregado raiz. Orden canonico: usuarios primero (§0.5.4).
    --     Serializa dos sincronizaciones concurrentes del MISMO usuario y evita
    --     la lectura no repetible del conjunto de roles que viene a continuacion.
    SELECT TRUE INTO v_existe
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    -- Validacion completa ANTES de borrar: si un rol no existe, se aborta sin
    -- haber dejado al usuario sin roles (la version Java borraba primero).
    FOREACH v_nombre_rol IN ARRAY p_nombres_rol LOOP
        IF NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre_rol) = UPPER(v_nombre_rol)) THEN
            RAISE EXCEPTION 'El rol especificado no existe en el sistema: %', UPPER(v_nombre_rol)
                USING ERRCODE = '23514';
        END IF;
    END LOOP;

    DELETE FROM usuario_roles WHERE id_usuario = p_id_usuario;

    -- (2) ON CONFLICT DO NOTHING contra uq_usuario_rol: idempotente aunque el
    --     array traiga repetidos o una transaccion concurrente se adelante.
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT p_id_usuario, r.id_rol
      FROM unnest(p_nombres_rol) AS n(nombre)
      JOIN roles r ON UPPER(r.nombre_rol) = UPPER(n.nombre)
    ON CONFLICT (id_usuario, id_rol) DO NOTHING;

    GET DIAGNOSTICS v_total = ROW_COUNT;

    -- Alta perezosa del perfil de creador, tambien idempotente.
    IF EXISTS (SELECT 1 FROM unnest(p_nombres_rol) AS n(nombre) WHERE UPPER(n.nombre) = 'CREADOR') THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia)
        SELECT p_id_usuario, 'Hola! Soy un creador en ARTISYNC.'
         WHERE NOT EXISTS (SELECT 1 FROM perfiles_creadores WHERE id_usuario = p_id_usuario);
    END IF;

    RETURN v_total;
END;
$$;
```

**Migración de esquema previa e imprescindible:**

```sql
-- Sin esto, la clausula ON CONFLICT no compila y el fantasma sigue abierto.
DELETE FROM usuario_roles a USING usuario_roles b          -- desduplicar lo existente
 WHERE a.id_usuario_rol > b.id_usuario_rol
   AND a.id_usuario = b.id_usuario AND a.id_rol = b.id_rol;

ALTER TABLE usuario_roles ADD CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol);
```

| Anomalía | Defensa |
|---|---|
| Lectura fantasma | `UNIQUE (id_usuario, id_rol)` + `ON CONFLICT DO NOTHING` |
| Lectura no repetible | `SELECT … FOR UPDATE` sobre `usuarios` |
| Actualización perdida | El `FOR UPDATE` serializa; el segundo escritor ve el estado ya confirmado |
| Interbloqueo | Orden canónico `usuarios` → `usuario_roles` → `roles` (§0.5.4) |

---

## 4. `fn_crear_usuario_admin` y `fn_crear_rol` — corrigen A3 y A8 (fantasma)

**Prioridad: P2.**

El patrón `if (existsByCorreo(...)) throw 409; save(...)` es una comprobación no atómica.
PostgreSQL **no ofrece bloqueo de rango** bajo READ COMMITTED ni REPEATABLE READ: no existe forma
de "bloquear el correo que aún no existe". La única defensa correcta es la **restricción `UNIQUE`
como predicado**, capturando la violación:

```sql
CREATE OR REPLACE FUNCTION fn_crear_usuario_admin(
    p_nombres          VARCHAR(100),
    p_apellidos        VARCHAR(100),
    p_correo           VARCHAR(150),
    p_contrasena_hash  VARCHAR(255),
    p_fecha_nacimiento DATE,
    p_id_pais          BIGINT,
    p_estado_cuenta    BOOLEAN,
    p_nombres_rol      TEXT[]
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
BEGIN
    IF p_id_pais IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pais WHERE id_pais = p_id_pais) THEN
        RAISE EXCEPTION 'Pais no encontrado' USING ERRCODE = '23503';
    END IF;

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT implicito.
    -- Si el INSERT viola uq usuarios.correo (fantasma materializado), se hace
    -- ROLLBACK TO SAVEPOINT automatico y se traduce a 409 en vez de un 500 crudo.
    BEGIN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash,
                              fecha_nacimiento, id_pais, estado_cuenta)
        VALUES (p_nombres, p_apellidos, p_correo, p_contrasena_hash,
                p_fecha_nacimiento, p_id_pais, COALESCE(p_estado_cuenta, TRUE))
        RETURNING id_usuario INTO v_id_usuario;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'El correo ya esta registrado: %', p_correo
                USING ERRCODE = '23505';          -- → 409 CONFLICT
    END;

    PERFORM fn_sincronizar_roles_usuario(
        v_id_usuario,
        COALESCE(NULLIF(p_nombres_rol, '{}'), ARRAY['CLIENTE']));

    RETURN v_id_usuario;
END;
$$;
```

Este es el ejemplo canónico del **nivel 2** de control transaccional descrito en §0.1: el
`BEGIN … EXCEPTION … END` **es** el `SAVEPOINT` / `ROLLBACK TO SAVEPOINT`. `fn_crear_rol` y la
validación de nombre duplicado de `fn_guardar_pais` (A9, implementada como una sola rutina para crear y renombrar) sigue exactamente el mismo molde
contra `roles.nombre_rol UNIQUE` y `pais.nombre_pais UNIQUE`.

---

## 5. `fn_revocar_sesiones_usuario` — corrige A6 (lectura no repetible)

**Prioridad: P1.**

### Problema

```java
List<SesionUsuario> sesiones = repo.findByUsuarioIdUsuario(idUsuario);  // (1) lectura
for (...) revocarJtiEnRedis(...);                                        // (2) Redis
repo.deleteByUsuarioIdUsuario(idUsuario);                                // (3) borrado
```

Una sesión creada entre (1) y (3) —un login concurrente del mismo usuario, exactamente lo que
pasa cuando un administrador desactiva una cuenta que está en uso— **se borra de la base sin
haberse revocado en Redis**: el JWT sigue siendo válido y ya no queda rastro de él.

### Solución

`DELETE … RETURNING`: leer y borrar en **una sola sentencia**, sobre el **mismo snapshot**. Es
imposible que aparezca una fila entre ambas operaciones porque no hay "ambas".

```sql
CREATE OR REPLACE FUNCTION fn_revocar_sesiones_usuario(
    p_id_usuario BIGINT
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
    -- DELETE ... RETURNING: lectura y borrado en la MISMA sentencia y el MISMO
    -- snapshot. Elimina la ventana de la version en dos pasos, en la que una
    -- sesion creada entremedias se borraba sin llegar nunca a revocarse en Redis.
    RETURN QUERY
    DELETE FROM sesiones_usuario s
     WHERE s.id_usuario = p_id_usuario
    RETURNING s.jti,
              GREATEST(0, EXTRACT(EPOCH FROM (s.fecha_expiracion - CURRENT_TIMESTAMP))::INTEGER);
END;
$$;
```

La escritura en Redis **permanece en Java** deliberadamente: Redis no es transaccional respecto
a PostgreSQL y un fallo suyo no debe revertir el borrado en la base (ni al revés). Lo que la
rutina garantiza es que Java recibe **exactamente** el conjunto de `jti` que se borró, ni uno más
ni uno menos.

### `fn_cambiar_estado_cuenta` — la composición

`changeEstado`, `deleteUser`, `deleteOwnAccount` y la rama de `updateUser` repiten el mismo par
"cambiar estado + revocar sesiones". Se unifican:

```sql
CREATE OR REPLACE FUNCTION fn_cambiar_estado_cuenta(
    p_id_usuario BIGINT,
    p_estado     BOOLEAN
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_anterior BOOLEAN;
BEGIN
    -- FOR UPDATE: sin el, dos administradores concurrentes (uno activando, otro
    -- desactivando) producen una actualizacion perdida y, peor, el que desactiva
    -- puede no revocar sesiones por haber leido estado_cuenta = FALSE obsoleto.
    SELECT estado_cuenta INTO v_estado_anterior
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    UPDATE usuarios SET estado_cuenta = p_estado WHERE id_usuario = p_id_usuario;

    IF v_estado_anterior AND NOT p_estado THEN
        RETURN QUERY SELECT * FROM fn_revocar_sesiones_usuario(p_id_usuario);
    END IF;

    RETURN;
END;
$$;
```

---

## 6. `fn_cambiar_contrasena` y `fn_solicitar_recuperacion` — corrigen A7 y A5

**Prioridad: P2.**

### `fn_cambiar_contrasena` (A7 — actualización perdida)

BCrypt vive fuera del motor, así que la **comparación** sigue en Java. Lo que se traslada es la
escritura condicionada: el `UPDATE` sólo se aplica **si el hash sigue siendo el que Java
verificó**. Es control de concurrencia optimista con el propio hash como testigo de versión.

```sql
CREATE OR REPLACE FUNCTION fn_cambiar_contrasena(
    p_id_usuario     BIGINT,
    p_hash_esperado  VARCHAR(255),
    p_hash_nuevo     VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    -- El predicado contrasena_hash = p_hash_esperado convierte esto en un
    -- compare-and-swap: si otra transaccion cambio la contrasena entre la
    -- verificacion BCrypt en Java y este UPDATE, EvalPlanQual re-evalua el
    -- WHERE sobre la version nueva, no coincide, y ROW_COUNT = 0.
    UPDATE usuarios
       SET contrasena_hash = p_hash_nuevo
     WHERE id_usuario = p_id_usuario
       AND contrasena_hash = p_hash_esperado;

    GET DIAGNOSTICS v_afectadas = ROW_COUNT;

    IF v_afectadas = 0 THEN
        RAISE EXCEPTION 'La contrasena fue modificada por otra sesion. Vuelve a intentarlo.'
            USING ERRCODE = '23514';
    END IF;

    RETURN TRUE;
END;
$$;
```

### `fn_solicitar_recuperacion` (A5)

Invalida los tokens previos e inserta el nuevo en una sola transacción, cerrando la ventana en
la que un usuario acumula N enlaces de recuperación válidos:

```sql
CREATE OR REPLACE FUNCTION fn_solicitar_recuperacion(
    p_correo     VARCHAR(150),
    p_hash_token VARCHAR(255)
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
    v_nombres    VARCHAR(100);
BEGIN
    SELECT id_usuario, nombres INTO v_id_usuario, v_nombres
      FROM usuarios
     WHERE correo = p_correo AND estado_cuenta = TRUE
       FOR UPDATE;                      -- serializa solicitudes concurrentes

    -- NULL, no excepcion: preserva la respuesta indistinguible de forgotPassword
    -- (no revelar si el correo existe).
    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    UPDATE tokens_recuperacion
       SET usado = TRUE
     WHERE id_usuario = v_id_usuario AND usado = FALSE;

    INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado)
    VALUES (v_id_usuario, p_hash_token, FALSE);

    RETURN jsonb_build_object('idUsuario', v_id_usuario, 'nombres', v_nombres);
END;
$$;
```

Nota sobre `fn_restablecer_contrasena` (REQ-F-005, ya existente): su `SELECT … FOR UPDATE` es
**correcto**, pero se ejecuta sobre `tokens_recuperacion.hash_token` **sin índice** ⇒ *seq scan*
con `FOR UPDATE`, lo que bloquea muchas más filas de las necesarias y multiplica la contención.
Ver §9.

---

## 7. `fn_configurar_2fa` (A4) y `sp_purgar_datos_seguridad` (A10)

### `fn_configurar_2fa` — P2

Sustituye 10 viajes no atómicos por uno. `autenticacion_dos_factores.id_usuario` ya es `UNIQUE`,
lo que permite un `ON CONFLICT` limpio:

```sql
CREATE OR REPLACE FUNCTION fn_configurar_2fa(
    p_id_usuario    BIGINT,
    p_llave_secreta VARCHAR(255),
    p_hashes        VARCHAR[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_total INTEGER;
BEGIN
    -- Upsert atomico sobre la restriccion UNIQUE existente: sin ventana entre
    -- "existe?" y "inserta/actualiza".
    INSERT INTO autenticacion_dos_factores (id_usuario, llave_secreta, esta_habilitado)
    VALUES (p_id_usuario, p_llave_secreta, FALSE)
    ON CONFLICT (id_usuario)
    DO UPDATE SET llave_secreta = EXCLUDED.llave_secreta, esta_habilitado = FALSE;

    -- Borrado + alta de los 8 codigos en la MISMA transaccion: es imposible
    -- observar un secreto nuevo con codigos de respaldo del secreto anterior.
    DELETE FROM codigos_respaldo_2fa WHERE id_usuario = p_id_usuario;

    INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado)
    SELECT p_id_usuario, h, FALSE FROM unnest(p_hashes) AS h;

    GET DIAGNOSTICS v_total = ROW_COUNT;
    RETURN v_total;
END;
$$;
```

`fn_desactivar_2fa(p_id_usuario)` es su contraparte: `UPDATE … esta_habilitado = FALSE` +
`DELETE` de códigos en una transacción, y **unifica** el código hoy duplicado entre
`TwoFactorServiceImpl.disable2Fa` y la rama de `AdminUserServiceImpl.updateUser`.

### `sp_purgar_datos_seguridad` — P3, el único PROCEDURE con COMMIT real

Aquí sí aplica el §0.3. Una purga que borre en una sola transacción millones de filas mantendría
abierta una transacción de larga duración que **impide a `VACUUM` recuperar espacio** en toda la
base. Se confirma por lotes — implementación final:
[`db/procs/sp_purgar_datos_seguridad.sql`](../../db/procs/sp_purgar_datos_seguridad.sql),
documentada en el catálogo §18a.

> **Corrección tras el primer borrador de este documento:** la versión original de este boceto
> envolvía los tres lotes en un bloque `EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE; END;` que
> también contenía los `COMMIT` de cada lote. **Eso es estructuralmente inválido en PL/pgSQL**:
> un bloque con cláusula `EXCEPTION` se implementa internamente como una subtransacción respaldada
> por un `SAVEPOINT`, y PostgreSQL prohíbe ejecutar `COMMIT`/`ROLLBACK` mientras ese savepoint
> sigue abierto — habría fallado con `2D000 invalid_transaction_termination` en el primer `COMMIT`
> del primer lote, no solo en el caso de error. El manual de PostgreSQL resuelve esto separando
> ambos: la excepción se atrapa en un bloque interno *sin* `COMMIT`, y el `COMMIT` ocurre en el
> bloque externo, fuera de cualquier `EXCEPTION`. La implementación final **no lleva bloque
> `EXCEPTION`**: si una sentencia falla a mitad de un lote, PostgreSQL aborta automáticamente la
> transacción en curso (sin necesidad de `ROLLBACK` explícito), los lotes de tablas anteriores que
> ya confirmaron permanecen intactos, y el error se propaga tal cual a través del `CALL` hasta
> `SeguridadPurgaScheduler` en Java, que ya lo captura y registra (best-effort, no tumba el
> proceso). Verificado con `SpPurgarDatosSeguridadIT` contra Postgres real.
>
> El alcance de la purga de `codigos_respaldo_2fa` también cambió respecto a este boceto: la
> implementación final purga **solo los códigos ya consumidos** (`usado = TRUE`), no los no usados
> de cuentas con 2FA deshabilitado — la tabla no tiene columna de fecha que distinga un código
> huérfano de uno recién generado por `fn_configurar_2fa` a la espera de `confirm2Fa`; purgar por
> `esta_habilitado = FALSE` habría borrado códigos de una configuración en curso sin confirmar.

`FOR UPDATE SKIP LOCKED` es lo que hace la purga **compatible con el tráfico en vivo**: en lugar
de esperar a una fila que un login concurrente tiene bloqueada, la salta y la recoge en la
siguiente ejecución. El índice `idx_sesiones_usuario_fecha_expiracion` que ya crea V8 —y que hoy
**no usa nadie**— fue pensado exactamente para esto.

**Invocación obligatoria fuera de transacción** (§0.3) —
[`SeguridadPurgaScheduler`](../../artisync/Backend/src/main/java/uteq/edu/ec/artisync/scheduler/SeguridadPurgaScheduler.java):

```java
@Scheduled(cron = "0 30 3 * * *")
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // sin esto: 2D000
public void purgarDatosSeguridad() {
    jdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", 1000);
}
```

---

## 8. Rutinas de sólo lectura: `fn_permisos_efectivos_usuario` y `fn_listar_usuarios_admin`

**Prioridad: P1 (rendimiento).**

No corrigen anomalías de escritura, pero **eliminan lecturas no repetibles internas**: hoy las
4–8 consultas de `loadUserByUsername` se ejecutan en snapshots distintos bajo READ COMMITTED, de
modo que una sincronización de permisos concurrente puede producir un `UserDetails` con los roles
**antiguos** y los permisos **nuevos** (o al revés). Resolverlas en una sola sentencia `STABLE`
garantiza una **lectura consistente** de las cuatro tablas.

```sql
CREATE OR REPLACE FUNCTION fn_permisos_efectivos_usuario(p_correo VARCHAR(150))
RETURNS JSONB
LANGUAGE sql
STABLE                       -- un solo snapshot: roles y permisos siempre coherentes entre si
AS $$
    SELECT jsonb_build_object(
        'idUsuario',     u.id_usuario,
        'correo',        u.correo,
        'contrasenaHash',u.contrasena_hash,
        'estadoCuenta',  u.estado_cuenta,
        'authorities',   COALESCE((
            SELECT jsonb_agg(DISTINCT a.autoridad)
              FROM (
                    SELECT 'ROLE_' || UPPER(r.nombre_rol) AS autoridad
                      FROM usuario_roles ur JOIN roles r ON r.id_rol = ur.id_rol
                     WHERE ur.id_usuario = u.id_usuario
                    UNION
                    SELECT UPPER(p.nombre_permiso)
                      FROM usuario_roles ur
                      JOIN rol_permisos rp ON rp.id_rol = ur.id_rol
                      JOIN permisos p ON p.id_permiso = rp.id_permiso
                     WHERE ur.id_usuario = u.id_usuario
                   ) a), '[]'::jsonb))
      FROM usuarios u
     WHERE u.correo = p_correo;
$$;
```

`fn_listar_usuarios_admin(p_busqueda, p_estado, p_limit, p_offset)` sigue el molde de
`fn_catalogo_filtrado` (REQ-F-013): agrega roles y permisos por usuario y resuelve el flag 2FA
por `LEFT JOIN`, con `count(*) OVER()` para la paginación. **De ~90 consultas a 1.**

---

## 9. Índices requeridos

PostgreSQL **no** crea índice automático para las claves foráneas. Sin estos, varias rutinas del
plan degradan a *seq scan*, y un `FOR UPDATE` sobre *seq scan* **agrava la contención** en vez de
resolverla:

```sql
-- Ruta caliente de autenticacion (cada peticion) — hoy seq scan
CREATE INDEX IF NOT EXISTS idx_usuario_roles_id_usuario ON usuario_roles (id_usuario);
CREATE INDEX IF NOT EXISTS idx_usuario_roles_id_rol     ON usuario_roles (id_rol);

-- fn_restablecer_contrasena hace SELECT ... FOR UPDATE sobre esta columna
CREATE INDEX IF NOT EXISTS idx_tokens_recuperacion_hash ON tokens_recuperacion (hash_token);

CREATE INDEX IF NOT EXISTS idx_usuarios_id_pais ON usuarios (id_pais);

-- Restricciones que convierten invariantes de negocio en garantias estructurales
ALTER TABLE usuario_roles ADD CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol);
CREATE UNIQUE INDEX IF NOT EXISTS uq_codigo_respaldo_usuario_hash
    ON codigos_respaldo_2fa (id_usuario, codigo_hash);
```

`rol_permisos(id_rol)` ya queda cubierto por el prefijo de `uk_rol_permiso (id_rol, id_permiso)`.

---

## 10. Plan de ejecución paso a paso

### Fase 0 — Base de esquema y concurrencia *(requisito de todas las demás)*

| Paso | Acción | Entregable |
|---|---|---|
| 0.1 | Desduplicar `usuario_roles` y añadir `uq_usuario_rol` | `V17__concurrencia_seguridad.sql` |
| 0.2 | Añadir `uq_codigo_respaldo_usuario_hash` | idem |
| 0.3 | Añadir los 4 índices FK de §9 | idem |
| 0.4 | Fijar `spring.jpa.properties.hibernate.connection.isolation` explícitamente a `READ_COMMITTED` | `application.yml` |
| 0.5 | Prueba de humo: `EXPLAIN ANALYZE` antes/después sobre `loadUserByUsername` | evidencia en `docs/mediciones/` |

> El paso 0.4 no cambia el comportamiento (ya es el valor efectivo), pero lo hace **explícito y
> versionado**: hoy el nivel de aislamiento del sistema depende de un valor por defecto no
> declarado en ningún sitio.

### Fase 1 — Corrección de los defectos de concurrencia *(P0–P1)*

| Paso | Rutina | Anomalía que cierra | Reemplaza |
|---|---|---|---|
| 1.1 | `fn_consumir_codigo_respaldo_2fa` | A1 actualización perdida | `TwoFactorServiceImpl.validarCodigoOBackup` |
| 1.2 | `fn_sincronizar_roles_usuario` | A2 fantasma + no atomicidad | `AdminUserServiceImpl.actualizarRoles` |
| 1.3 | `fn_revocar_sesiones_usuario` | A6 lectura no repetible | `SessionRevocationService` (parte SQL) |
| 1.4 | `fn_cambiar_estado_cuenta` | A6 compuesta | `changeEstado` / `deleteUser` / `deleteOwnAccount` |

Para **cada** paso, el mismo ciclo cerrado:

1. Escribir `db/procs/fn_x.sql` con su cabecera documental (categoría funcional + requisito).
2. Escribir **primero la prueba de concurrencia** (§11) y verla **fallar** contra el código actual.
3. Implementar la rutina; `make sync-procs` para propagar a `R__procedimientos.sql`.
4. Cambiar el repositorio (`@Query` nativa, patrón de `UsuarioRepository`) y el servicio.
5. Confirmar que la prueba **pasa** y que `scripts/audit-sql-dynamic.sh` sigue en verde.
6. Registrar la rutina en `docs/basedatos/CATALOGO-SP.md` y en la tabla de `db/procs/README.md`.

### Fase 2 — Rendimiento *(P1)* — ✅ completada (22 de agosto de 2026)

| Paso | Rutina | Ganancia | Estado |
|---|---|---|---|
| 2.1 | `fn_permisos_efectivos_usuario` | 4–8 consultas → 1, **por cada petición autenticada** | ✅ Implementada. `CustomUserDetailsService.loadUserByUsername` la invoca vía `UsuarioRepository.permisosEfectivos`. |
| 2.2 | `fn_listar_usuarios_admin` | ~42 consultas → 4 por pantalla (página de 20) | ⚠️ **Reemplazada por una solución sin rutina almacenada.** Al implementarla se detectó un conflicto real con el `sortBy` arbitrario que ya soporta `GET /api/v1/admin/usuarios` (resuelto hoy por `Pageable`/`Sort` de Spring Data): reproducir un `ORDER BY` por columna arbitraria en una función SQL exige `EXECUTE format(...)`, que viola la regla de cero SQL dinámico. Se optó por `UsuarioMapper.toUserResponseList` — batchea roles/permisos/2FA de toda la página en 2 consultas `IN (...)` en Java, conservando `findAll(pageable)` y el `Sort` dinámico intactos. Ver `docs/basedatos/CATALOGO-SP.md` §16 para el detalle completo de la decisión. |
| 2.3 | Escenario k6 antes/después sobre `/api/v1/auth/login` y `/api/v1/admin/usuarios` | evidencia en `k6/` | ⏳ Pendiente — requiere el stack levantado (`make up`) y `k6` instalado; no ejecutable en este entorno. |

**Pieza pendiente pero fuera de alcance de esta fase:** `UsuarioMapper` sigue accediendo a
`usuario.getPais()` (`FetchType.LAZY`) fila a fila sin batch — un N+1 menor preexistente, no
introducido por esta fase, sobre una tabla pequeña. Candidato a `JOIN FETCH` si el catálogo de
países creciera.

### Fase 3 — Atomicidad restante *(P2)* — ✅ completada (22 de agosto de 2026)

| Paso | Rutina | Anomalía | Estado |
|---|---|---|---|
| 3.1 | `fn_configurar_2fa` + `fn_desactivar_2fa` | A4 | ✅ Implementadas. `TwoFactorServiceImpl.setup2Fa`/`disable2Fa` y `AdminUserServiceImpl.updateUser` las invocan; unifican el código antes duplicado entre estos dos últimos. |
| 3.2 | `fn_solicitar_recuperacion` | A5 | ✅ Implementada. `AuthServiceImpl.forgotPassword` la invoca vía `UsuarioRepository.solicitarRecuperacion`. |
| 3.3 | `fn_cambiar_contrasena` | A7 | ✅ Implementada. `UserServiceImpl.changePassword` la invoca; ERRCODE `40001` (serialization_failure) se traduce a `409 CONFLICT`. |
| 3.4 | `fn_crear_usuario_admin` | A3 | ✅ Implementada. `AdminUserServiceImpl.createUser` la invoca; compone con `fn_sincronizar_roles_usuario` (Fase 1). |
| 3.5 | `fn_crear_rol` + `fn_guardar_pais` (renombrada desde `fn_actualizar_pais`) | A8, A9 | ✅ Implementadas. `fn_crear_rol` compone con `fn_sincronizar_permisos_rol` (#9); `fn_guardar_pais` cubre **create y update** de país con una sola rutina (mismo `EXCEPTION WHEN unique_violation`, sin `FOR UPDATE` posible en ningún caso porque la fila en conflicto pertenece a otro país). `PaisServiceImpl` traduce el resultado de vuelta a `ExcepcionRecursoDuplicado`/`ExcepcionRecursoNoEncontrado` para no romper su contrato de excepciones existente. |

**Verificación:** 571 tests unitarios en verde (H2), incluyendo las reescrituras de
`AdminUserServiceImplTest`, `AuthServiceImplTest`, `PaisServiceImplTest`, `RolePermissionServiceImplTest`,
`TwoFactorServiceImplTest` y `UserServiceImplTest` para las nuevas rutinas (simulando `SQLState`
23505/23503/23514/40001 vía el helper `excepcionSql`, patrón ya establecido en el proyecto).
`sync-procs --check` y `audit-sql-dynamic.sh` en verde (26 archivos, 25 rutinas reales).

### Fase 4 — Mantenimiento *(P3)* — ✅ completada (23 de agosto de 2026)

| Paso | Acción | Estado |
|---|---|---|
| 4.1 | `sp_purgar_datos_seguridad` (PROCEDURE con COMMIT por lotes) | ✅ Implementada. Único `PROCEDURE` de `db/procs/`; `FOR UPDATE SKIP LOCKED` + `COMMIT` por lote sobre `sesiones_usuario`, `tokens_recuperacion` y `codigos_respaldo_2fa`. Alcance de `codigos_respaldo_2fa` acotado deliberadamente a `usado = TRUE` (ver cabecera del archivo): purgar por `esta_habilitado = FALSE` habría borrado códigos de un `setup2Fa` en curso sin confirmar, al no existir columna de fecha que distinga ambos casos. |
| 4.2 | `@Scheduled` con `Propagation.NOT_SUPPORTED` | ✅ Implementado. `SeguridadPurgaScheduler` (`0 30 3 * * *`, vía `JdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", ...)`), fallo capturado con logging best-effort (no tumba el proceso). |
| 4.3 | Evidencia de `EXPLAIN ANALYZE`/k6 sobre `idx_tokens_recuperacion_hash` (creado en Fase 0) | ⏳ Pendiente — requiere el stack levantado (`make up`); no ejecutable en este entorno, igual que el resto de evidencia de medición pendiente en Fases 1–2. |

**Verificación:** `sync-procs --check` y `audit-sql-dynamic.sh` en verde (27 archivos, 26 rutinas
reales). **573 tests unitarios pasan** (H2), incluyendo `SeguridadPurgaSchedulerTest` (invocación
correcta del `CALL` + no propagación de fallos). Añadido
[`SpPurgarDatosSeguridadIT`](../../artisync/Backend/src/test/java/uteq/edu/ec/artisync/repository/seguridad/SpPurgarDatosSeguridadIT.java)
(Postgres real, mismo patrón `*IT` del proyecto) que verifica las tres reglas de alcance: purga
sesiones expiradas conservando las vigentes, purga tokens muertos conservando uno reciente, y —el
invariante crítico— purga códigos de respaldo consumidos **sin tocar nunca** los no usados.

### Fase 5 — Documentación de la entrega

| Paso | Acción |
|---|---|
| 5.1 | Ampliar `docs/basedatos/CATALOGO-SP.md` con las 12 rutinas nuevas |
| 5.2 | Añadir sección "Concurrencia y aislamiento" al ADR-006 remitiendo a este documento |
| 5.3 | Actualizar la matriz de trazabilidad con los nuevos REQ asociados |

---

## 11. Cómo se prueba la concurrencia (obligatorio en cada paso de la Fase 1)

Una prueba unitaria normal **no detecta ninguna** de estas anomalías: hacen falta dos
transacciones solapadas. Dos niveles:

### 11.1 Prueba determinista en SQL (dos sesiones psql)

Demuestra la anomalía y su corrección de forma reproducible y auditable:

```sql
-- Sesion A                                  -- Sesion B
BEGIN;
SELECT fn_consumir_codigo_respaldo_2fa(1,'h');
--> true
                                             BEGIN;
                                             SELECT fn_consumir_codigo_respaldo_2fa(1,'h');
                                             --> BLOQUEADA esperando a A
COMMIT;
                                             --> se desbloquea, devuelve FALSE  ✔
                                             COMMIT;
```

Contra el código Java actual, la sesión B devolvería `true`: ahí está A1 demostrada.

### 11.2 Prueba en JUnit con Testcontainers

`CountDownLatch` + `ExecutorService` con N hilos ejecutando la misma operación sobre la misma
fila, y aserción sobre el **invariante**, no sobre el orden:

```java
@Test
void soloUnHiloConsumeElCodigoDeRespaldo() throws Exception {
    var barrera = new CountDownLatch(1);
    var exitos  = new AtomicInteger();
    var pool    = Executors.newFixedThreadPool(10);

    for (int i = 0; i < 10; i++) {
        pool.submit(() -> {
            barrera.await();                       // arranque simultaneo
            if (twoFactorService.validarCodigoOBackup(CORREO, CODIGO)) exitos.incrementAndGet();
            return null;
        });
    }
    barrera.countDown();
    pool.shutdown();
    pool.awaitTermination(30, TimeUnit.SECONDS);

    assertThat(exitos.get()).isEqualTo(1);         // invariante: un solo uso
}
```

Análogos para: `fn_sincronizar_roles_usuario` (invariante: sin roles duplicados y nunca cero
roles), `fn_cambiar_contrasena` (invariante: exactamente un hilo tiene éxito) y
`fn_revocar_sesiones_usuario` (invariante: `jti` devueltos = `jti` borrados).

---

## 12. Resumen

| Fase | Rutinas | Efecto principal |
|---|---|---|
| 0 | — (esquema) | Cierra estructuralmente 2 fantasmas; quita *seq scans* de la ruta de auth |
| 1 | 4 rutinas | Elimina **A1, A2, A6** — las tres con impacto de seguridad |
| 2 | 2 rutinas | ~6→1 y ~90→1 consultas; además lecturas consistentes |
| 3 | 6 rutinas | Elimina **A3, A4, A5, A7, A8, A9** |
| 4 | 1 procedure | Detiene el crecimiento ilimitado de 3 tablas (**A10**) |

**Total: 13 rutinas nuevas + 6 objetos de esquema**, todas dentro de las categorías funcionales
del apartado A.2.2 ya usadas por el catálogo y respetando el contrato de `db/procs/`
(un archivo por rutina, `CREATE OR REPLACE`, cero SQL dinámico, parámetros formales tipados).
