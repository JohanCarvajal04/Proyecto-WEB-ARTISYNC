-- =============================================================================
-- V8: sesiones_usuario guarda unicamente el jti, nunca el JWT completo.
-- (§2.5 / OBS-AUTO-06 — ver docs/observaciones/OBSERVACIONES.md)
--
-- Antes: la fila guardaba el access/refresh token INTEGRO en texto plano
-- (token_jwt TEXT). Una lectura de esta tabla (SQLi, backup filtrado, dump)
-- entregaba tomas de control de todas las sesiones activas. El jti ya se
-- generaba en cada token (JwtService) pero no se aprovechaba para esto.
--
-- Ruptura limpia deliberada: el jti de las filas existentes solo se podria
-- recuperar parseando el token almacenado, que es exactamente lo que se
-- quiere dejar de guardar. Se purgan; todas las sesiones activas quedan
-- invalidadas UNA sola vez (coincide con el despliegue de V1b: los tokens
-- emitidos antes de este cambio tampoco llevan el claim "type" ni audience
-- tipada que la nueva validacion exige — ver JwtService). Aceptable en un
-- proyecto academico sin usuarios reales en produccion continua.
-- =============================================================================

DELETE FROM sesiones_usuario;

ALTER TABLE sesiones_usuario ADD COLUMN IF NOT EXISTS jti VARCHAR(36);
ALTER TABLE sesiones_usuario ALTER COLUMN jti SET NOT NULL;
ALTER TABLE sesiones_usuario DROP COLUMN IF EXISTS token_jwt;

-- UNIQUE crea su propio indice btree: no hace falta un CREATE INDEX aparte.
ALTER TABLE sesiones_usuario ADD CONSTRAINT uq_sesiones_usuario_jti UNIQUE (jti);

-- Respaldan findByUsuarioIdUsuario / deleteByUsuarioIdUsuario (se ejecutan en
-- cada revocacion administrativa) y una futura tarea de purga por expiracion;
-- antes de este indice esas consultas hacian seq scan sobre la tabla completa.
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_id_usuario
    ON sesiones_usuario (id_usuario);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_fecha_expiracion
    ON sesiones_usuario (fecha_expiracion);
