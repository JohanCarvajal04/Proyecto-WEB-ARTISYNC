-- =============================================================================
-- V13: Sembrar una plantilla de contrato por defecto
-- =============================================================================
--
-- ContratoServicioImpl.generarContrato() exige que exista al menos una fila en
-- plantillas_contrato (toma la de id_plantilla mayor) y, si no la encuentra,
-- rechaza CUALQUIER generación de contrato con
-- "No hay plantillas de contrato disponibles en el sistema". La tabla nunca
-- tenía un INSERT ni en el esquema base ni en el seed de desarrollo, así que
-- en una base de datos nueva el flujo contrato -> firma -> chat -> pago quedaba
-- bloqueado de fábrica, sin ninguna pantalla de administración para crear una
-- plantilla desde la UI.
--
-- Los placeholders usados abajo son exactamente los que
-- ContratoServicioImpl#generarContratoHtml reemplaza por texto real al
-- renderizar el contrato de un pedido concreto.
--
-- ON CONFLICT sobre version_legal (columna UNIQUE): idempotente si esta
-- migración se reejecuta sobre un entorno que ya tiene la plantilla.

INSERT INTO plantillas_contrato (version_legal, cuerpo_html_plantilla)
VALUES (
    'v1.0',
    '<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Contrato de Prestación de Servicios</title></head>
<body>
  <h1>Contrato de Prestación de Servicios Creativos</h1>
  <p>Entre <strong>{{nombre_creador}}</strong> ("el Creador") y
     <strong>{{nombre_cliente}}</strong> ("el Cliente"), suscrito el {{fecha_actual}}.</p>

  <h2>1. Objeto del contrato</h2>
  <p>{{descripcion_servicio}}</p>

  <h2>2. Precio pactado</h2>
  <p>El Cliente pagará al Creador la suma de {{precio_pactado}} USD, retenida
     en garantía hasta la aprobación de la entrega.</p>

  <h2>3. Revisiones incluidas</h2>
  <p>El Cliente tiene derecho a {{limite_revisiones}} revisión(es) sin costo
     adicional sobre el entregable.</p>

  <h2>4. Fecha de entrega estimada</h2>
  <p>{{fecha_entrega}}</p>

  <h2>5. Firmas</h2>
  <p>Ambas partes aceptan los términos anteriores al firmar electrónicamente
     este documento dentro de la plataforma ARTISYNC.</p>
</body>
</html>'
)
ON CONFLICT (version_legal) DO NOTHING;
