Analiza esta imagen de un documento de identidad oficial (cédula, pasaporte o DNI).
Responde EXCLUSIVAMENTE en formato JSON válido con esta estructura:
{
  "es_documento_valido": true/false,
  "tipo_documento": "cedula|pasaporte|dni|licencia|otro",
  "nombre_detectado": "string o null",
  "fecha_nacimiento": "YYYY-MM-DD o null",
  "es_mayor_de_edad": true/false,
  "pais_emision": "string o null",
  "confianza": 0.00 a 1.00,
  "razon_rechazo": "string o null"
}
Si el documento está borroso, cortado, o no es un documento de identidad, marca es_documento_valido como false.
No incluyas texto fuera del JSON.
