-- ==============================================================================
-- MIGRACIÓN V9: FLUJO DE TRABAJO POR CATEGORÍA (RF-19)
-- ==============================================================================
--
-- RF-19 exige que las etapas de un pedido se configuren "según la categoría del
-- servicio", pero no existía ninguna relación entre `categorias` y
-- `flujos_trabajo`: PedidoServicioImpl tomaba `findAll().get(0)`, es decir el
-- primer flujo que devolviese Postgres sin ORDER BY. Todos los pedidos
-- compartían flujo y cuál era dependía del plan de ejecución.
--
-- Aquí va solo el cambio de esquema. Los datos de flujos y etapas son insumo de
-- prueba y viven en `database/seed-medicion-referencia.sql`, fuera de las
-- migraciones versionadas, siguiendo la convención del proyecto.

-- ------------------------------------------------------------------------------
-- 1. Relación categoría → flujo
-- ------------------------------------------------------------------------------
-- Nullable a propósito: una categoría sin flujo asignado cae a un flujo de
-- respaldo en el servicio, en lugar de impedir que se cree el pedido.
ALTER TABLE categorias
    ADD COLUMN IF NOT EXISTS id_flujo BIGINT REFERENCES flujos_trabajo(id_flujo);

-- Acelera la resolución servicio → subcategoría → categoría → flujo al crear
-- un pedido.
CREATE INDEX IF NOT EXISTS idx_categorias_id_flujo ON categorias (id_flujo);

-- ------------------------------------------------------------------------------
-- 2. Backfill conservador
-- ------------------------------------------------------------------------------
-- Si la instalación ya tenía flujos, las categorías existentes se apuntan al de
-- menor id para conservar el comportamiento previo (todas compartían uno), pero
-- ahora de forma explícita y consultable.
--
-- No se referencia ningún flujo por nombre: los nombres los fija el seed, no el
-- esquema. Sobre una base sin flujos esto no hace nada y la columna queda NULL,
-- que es un estado válido.
UPDATE categorias
SET id_flujo = (SELECT MIN(id_flujo) FROM flujos_trabajo)
WHERE id_flujo IS NULL
  AND EXISTS (SELECT 1 FROM flujos_trabajo);
