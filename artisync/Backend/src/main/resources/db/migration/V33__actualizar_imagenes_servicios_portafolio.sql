-- V33: Actualizar URLs de miniaturas e imágenes de servicios y portafolio con arte temático acorde
UPDATE servicios
SET url_miniatura = '/images/servicios/prop-3d.jpg'
WHERE titulo_servicio ILIKE '%Prop 3D%' OR titulo_servicio ILIKE '%Videojuego%';

UPDATE servicios
SET url_miniatura = '/images/servicios/modelado-3d.jpg'
WHERE titulo_servicio ILIKE '%Modelado de Personaje 3D%' OR titulo_servicio ILIKE '%Personaje 3D%';

UPDATE servicios
SET url_miniatura = '/images/servicios/manual-identidad.png'
WHERE titulo_servicio ILIKE '%Manual de Identidad%' OR titulo_servicio ILIKE '%Identidad de Marca%';

UPDATE servicios
SET url_miniatura = '/images/servicios/diseno-logo.png'
WHERE titulo_servicio ILIKE '%Diseño de Logotipo%' OR titulo_servicio ILIKE '%Logotipo Profesional%';

UPDATE servicios
SET url_miniatura = '/images/servicios/concept-art.png'
WHERE titulo_servicio ILIKE '%Concept Art%';

UPDATE servicios
SET url_miniatura = '/images/servicios/retrato-digital.jpg'
WHERE titulo_servicio ILIKE '%Retrato Digital%' OR titulo_servicio ILIKE '%Retrato%';

-- Actualizar también obras de portafolio
UPDATE portafolio_items
SET url_archivo_multimedia = '/images/servicios/retrato-digital.jpg'
WHERE titulo_obra ILIKE '%Retrato%';

UPDATE portafolio_items
SET url_archivo_multimedia = '/images/servicios/concept-art.png'
WHERE titulo_obra ILIKE '%Concept art%' OR titulo_obra ILIKE '%Guerrera%';

UPDATE portafolio_items
SET url_archivo_multimedia = '/images/servicios/manual-identidad.png'
WHERE titulo_obra ILIKE '%Café Aroma%' OR titulo_obra ILIKE '%Identidad visual%';

UPDATE portafolio_items
SET url_archivo_multimedia = '/images/servicios/modelado-3d.jpg'
WHERE titulo_obra ILIKE '%Explorador espacial%' OR titulo_obra ILIKE '%Personaje 3D%';

