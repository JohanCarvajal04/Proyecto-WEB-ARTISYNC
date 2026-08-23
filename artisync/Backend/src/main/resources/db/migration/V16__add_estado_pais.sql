-- Agrega la columna estado a la tabla pais para permitir soft-delete
ALTER TABLE pais ADD COLUMN estado BOOLEAN DEFAULT TRUE NOT NULL;
