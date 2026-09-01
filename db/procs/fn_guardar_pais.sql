-- =============================================================================
-- fn_guardar_pais
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A9.
-- =============================================================================
-- Crea (p_id_pais NULL) o renombra (p_id_pais con valor) un pais, validando
-- la unicidad del nombre de forma atomica respecto a la escritura.
--
-- Sustituye la parte de escritura de PaisServiceImpl.createPais y .updatePais,
-- que comprobaban paisRepository.findByNombrePais(...) y luego hacian save()
-- en sentencias separadas -- lectura fantasma no atomica: entre la
-- comprobacion y el insert/update, otra transaccion podia tomar el mismo
-- nombre. Mitigado en la practica por pais.nombre_pais UNIQUE, pero sin
-- traduccion de error (ExcepcionRecursoDuplicado nunca se lanzaba realmente
-- por una condicion de carrera; solo por la lectura previa, que era la parte
-- no atomica).
--
-- Una sola rutina cubre ambos casos porque comparten la misma tecnica: ni
-- crear ni renombrar pueden usar SELECT ... FOR UPDATE para "bloquear" el
-- nombre en conflicto (esa fila, si existe, pertenece a OTRO pais, no al que
-- se esta creando o editando); la unica defensa correcta en ambos casos es la
-- restriccion UNIQUE como predicado, capturada con un bloque EXCEPTION.
-- Renombrar un pais a su propio nombre actual no dispara la restriccion (es
-- la misma fila, mismo valor), preservando el comportamiento previo de
-- permitir un "no-op" de nombre.
--
-- Devuelve el id_pais afectado. Lanza excepcion si el nombre ya pertenece a
-- otro pais, o (al renombrar) si el id no existe.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_guardar_pais(
    p_id_pais     BIGINT,
    p_nombre_pais VARCHAR(100)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_pais      BIGINT;
    v_nombre_norm  VARCHAR(100);
BEGIN
    IF p_nombre_pais IS NULL OR btrim(p_nombre_pais) = '' THEN
        RAISE EXCEPTION 'fn_guardar_pais: p_nombre_pais es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    v_nombre_norm := btrim(p_nombre_pais);

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT/UPDATE viola uq pais.nombre_pais (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de dejar pasar el
    -- duplicado o fallar con un error generico.
    BEGIN
        IF p_id_pais IS NULL THEN
            INSERT INTO pais (nombre_pais)
            VALUES (v_nombre_norm)
            RETURNING id_pais INTO v_id_pais;
        ELSE
            UPDATE pais
               SET nombre_pais = v_nombre_norm
             WHERE id_pais = p_id_pais
            RETURNING id_pais INTO v_id_pais;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'Pais no encontrado con ID: %', p_id_pais
                    USING ERRCODE = 'P0002';
            END IF;
        END IF;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'Ya existe un pais registrado con el nombre: %', v_nombre_norm
                USING ERRCODE = '23505';
    END;

    RETURN v_id_pais;
END;
$$;

COMMENT ON FUNCTION fn_guardar_pais(BIGINT, VARCHAR)
    IS 'Fase 3 concurrencia - Crea o renombra un pais capturando unique_violation en vez de una comprobacion findByNombrePais no atomica (A9).';
