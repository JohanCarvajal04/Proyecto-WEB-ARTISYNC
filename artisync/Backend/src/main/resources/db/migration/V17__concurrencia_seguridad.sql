-- =============================================================================
-- V14: base de esquema para el control de concurrencia del modulo de seguridad
-- (Fase 0 de docs/basedatos/PLAN-CONCURRENCIA-SP.md)
-- =============================================================================
-- Esta migracion NO contiene logica: crea las restricciones e indices de los
-- que dependen las rutinas de la Fase 1. Se aplica antes que ellas porque
-- fn_sincronizar_roles_usuario usa ON CONFLICT (id_usuario, id_rol), y esa
-- clausula no compila sin una restriccion unica que la respalde.
--
-- Dos motivaciones distintas conviven aqui:
--
--   1. Restricciones que convierten un invariante de negocio en una garantia
--      ESTRUCTURAL. PostgreSQL no ofrece bloqueo de rango bajo READ COMMITTED
--      (el nivel por defecto y el que usa este proyecto), de modo que el patron
--      "comprobar si existe y luego insertar" siempre deja una ventana de
--      lectura fantasma. La unica defensa correcta es un indice unico: el motor
--      lo evalua como predicado en el momento del INSERT.
--
--   2. Indices de clave foranea. PostgreSQL NO crea indice automatico para una
--      FK (a diferencia de MySQL). Sin ellos varias consultas de la ruta de
--      autenticacion degradan a seq scan, y un SELECT ... FOR UPDATE sobre un
--      seq scan AGRAVA la contencion en vez de resolverla: bloquea muchas mas
--      filas de las necesarias.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. usuario_roles: unicidad de la pareja (usuario, rol)
-- -----------------------------------------------------------------------------
-- Cierra la anomalia A2 del plan. Hoy AdminUserServiceImpl.actualizarRoles hace
-- deleteAll + bucle de save() sin atomicidad: dos administradores editando el
-- mismo usuario a la vez producen filas duplicadas, porque nada en el esquema
-- lo impide (la tabla solo tiene PK sobre id_usuario_rol).
--
-- Se desduplica primero lo ya existente conservando la fila mas antigua de cada
-- pareja; de lo contrario el ALTER TABLE fallaria sobre una base con datos.
DELETE FROM usuario_roles a
      USING usuario_roles b
      WHERE a.id_usuario_rol > b.id_usuario_rol
        AND a.id_usuario = b.id_usuario
        AND a.id_rol = b.id_rol;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_usuario_rol') THEN
        ALTER TABLE usuario_roles
            ADD CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol);
    END IF;
END
$$;

COMMENT ON CONSTRAINT uq_usuario_rol ON usuario_roles
    IS 'Impide roles duplicados por usuario. Respalda el ON CONFLICT de fn_sincronizar_roles_usuario.';


-- -----------------------------------------------------------------------------
-- 2. codigos_respaldo_2fa: unicidad del hash por usuario
-- -----------------------------------------------------------------------------
-- Apoya fn_consumir_codigo_respaldo_2fa (anomalia A1): permite que el UPDATE
-- que consume un codigo de respaldo localice la fila por indice en vez de
-- recorrer todos los codigos del usuario, y garantiza que "un codigo" designe
-- siempre a lo sumo una fila.
--
-- Se desduplica primero por el mismo motivo que en el punto 1.
DELETE FROM codigos_respaldo_2fa a
      USING codigos_respaldo_2fa b
      WHERE a.id_codigo > b.id_codigo
        AND a.id_usuario = b.id_usuario
        AND a.codigo_hash = b.codigo_hash;

CREATE UNIQUE INDEX IF NOT EXISTS uq_codigo_respaldo_usuario_hash
    ON codigos_respaldo_2fa (id_usuario, codigo_hash);

COMMENT ON INDEX uq_codigo_respaldo_usuario_hash
    IS 'Un codigo de respaldo identifica a lo sumo una fila por usuario. Respalda fn_consumir_codigo_respaldo_2fa.';


-- -----------------------------------------------------------------------------
-- 3. Indices de clave foranea ausentes
-- -----------------------------------------------------------------------------
-- idx_usuario_roles_id_usuario es el mas critico de los cuatro: lo recorren
-- CustomUserDetailsService.loadUserByUsername (una vez por CADA peticion
-- autenticada, via JwtAuthenticationFilter), UsuarioMapper.toUserResponse (una
-- vez por CADA fila del listado de administracion) y fn_resolver_estado_login.
-- Hasta esta migracion, todos ellos hacian seq scan sobre usuario_roles.
CREATE INDEX IF NOT EXISTS idx_usuario_roles_id_usuario
    ON usuario_roles (id_usuario);

-- Recorrido inverso: existsByRolIdRol, fn_eliminar_rol y
-- findIdsUsuarioByNombreRol (revocacion de sesiones al sincronizar permisos).
CREATE INDEX IF NOT EXISTS idx_usuario_roles_id_rol
    ON usuario_roles (id_rol);

-- fn_restablecer_contrasena (REQ-F-005) hace SELECT ... FOR UPDATE filtrando
-- por hash_token. Su bloqueo era correcto, pero sobre un seq scan el motor
-- visita y bloquea filas que no son la buscada, multiplicando la contencion
-- entre restablecimientos concurrentes de usuarios distintos.
CREATE INDEX IF NOT EXISTS idx_tokens_recuperacion_hash
    ON tokens_recuperacion (hash_token);

-- UsuarioRepository.existsByPaisIdPais, invocado antes de desactivar un pais.
CREATE INDEX IF NOT EXISTS idx_usuarios_id_pais
    ON usuarios (id_pais);

-- Nota: rol_permisos(id_rol) NO necesita indice propio. Ya queda cubierto por
-- el prefijo izquierdo de uk_rol_permiso (id_rol, id_permiso), que V1 crea.
