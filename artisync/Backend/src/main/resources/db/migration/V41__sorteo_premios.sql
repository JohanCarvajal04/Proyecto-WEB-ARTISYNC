-- =============================================================================
-- V41__sorteo_premios.sql
-- Categoria funcional: modelo de datos   Requisito: REQ-F-023
-- =============================================================================
-- Corrige un defecto de diseno: "premio" no existia como concepto individual.
-- sorteos.descripcion_premios era un unico campo de texto libre y
-- participantes_sorteo.es_ganador un simple flag, sin ninguna columna que
-- dijera "este ganador se llevo cual premio". Por eso todos los ganadores se
-- mostraban agrupados en una sola caja en el frontend, y no existia forma de
-- validar que la cantidad de ganadores coincidiera con la cantidad de premios
-- (no habia una "cantidad de premios" que contar).
--
-- Esta migracion introduce premios_sorteo como tabla de premios individuales
-- (uno por fila, con su propio texto y orden) y liga cada ganador a un premio
-- especifico via participantes_sorteo.id_premio.
-- =============================================================================

CREATE TABLE premios_sorteo (
    id_premio          BIGSERIAL PRIMARY KEY,
    id_sorteo          BIGINT NOT NULL REFERENCES sorteos(id_sorteo) ON DELETE CASCADE,
    descripcion_premio VARCHAR(255) NOT NULL,
    orden              INT NOT NULL CHECK (orden >= 1),
    UNIQUE (id_sorteo, orden)
);

COMMENT ON TABLE premios_sorteo IS
    'REQ-F-023 - Premios individuales de un sorteo. Reemplaza a sorteos.descripcion_premios (texto libre unico).';

ALTER TABLE participantes_sorteo
    ADD COLUMN id_premio BIGINT NULL REFERENCES premios_sorteo(id_premio) ON DELETE SET NULL;

CREATE INDEX idx_participantes_sorteo_id_premio ON participantes_sorteo(id_premio);

-- -----------------------------------------------------------------------------
-- Backfill de sorteos existentes: se genera un premio por cada "ganador"
-- configurado en el sorteo (cantidad_ganadores), reutilizando el texto de
-- descripcion_premios para las N filas. Es un compromiso deliberado para no
-- perder sorteos ya creados: el texto historico no distinguia premios entre
-- si, asi que las N filas quedan con la misma descripcion hasta que el
-- creador las edite (algo que ya no puede hacer si el sorteo tiene
-- participantes, igual que hoy no puede cambiar cantidad_ganadores).
-- -----------------------------------------------------------------------------
INSERT INTO premios_sorteo (id_sorteo, descripcion_premio, orden)
SELECT s.id_sorteo, s.descripcion_premios, gs.orden
  FROM sorteos s
  CROSS JOIN LATERAL generate_series(1, s.cantidad_ganadores) AS gs(orden);

-- Empareja a los ganadores ya sorteados con los premios recien creados, en el
-- mismo orden en que fueron notificados (o por id_participacion si empataran).
WITH ganadores_numerados AS (
    SELECT id_participacion, id_sorteo,
           ROW_NUMBER() OVER (
               PARTITION BY id_sorteo
               ORDER BY fecha_notificacion_premio NULLS LAST, id_participacion
           ) AS rn
      FROM participantes_sorteo
     WHERE es_ganador = TRUE
)
UPDATE participantes_sorteo ps
   SET id_premio = pr.id_premio
  FROM ganadores_numerados gn
  JOIN premios_sorteo pr ON pr.id_sorteo = gn.id_sorteo AND pr.orden = gn.rn
 WHERE ps.id_participacion = gn.id_participacion;

-- Cada premio ya lleva su propia descripcion; el texto unico deja de ser necesario.
ALTER TABLE sorteos DROP COLUMN descripcion_premios;
