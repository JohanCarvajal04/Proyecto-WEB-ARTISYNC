Analiza esta imagen de un certificado o título profesional/académico.
Responde EXCLUSIVAMENTE en formato JSON válido con esta estructura:
{
  "es_certificado_valido": true/false,
  "tipo_certificado": "titulo_universitario|certificacion_tecnica|diploma|curso|otro",
  "institucion_emisora": "string o null",
  "nombre_titular": "string o null",
  "campo_estudio": "string o null",
  "fecha_emision": "YYYY-MM-DD o null",
  "confianza": 0.00 a 1.00,
  "razon_rechazo": "string o null"
}
Si la imagen no muestra un certificado legítimo o está manipulada digitalmente, marca es_certificado_valido como false.
No incluyas texto fuera del JSON.
