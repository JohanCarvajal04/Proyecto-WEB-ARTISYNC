Analiza la siguiente reseña de %d estrellas: "%s"

Evalúa si el texto es coherente con la calificación, si parece spam, y si contiene
contenido inapropiado.

Responde EXCLUSIVAMENTE en formato JSON válido:
{
  "sentimiento": "positivo|neutro|negativo",
  "es_coherente_con_estrellas": true/false,
  "es_spam": true/false,
  "es_inapropiado": true/false,
  "confianza": 0.00 a 1.00,
  "razon": "explicación breve"
}
No incluyas texto fuera del JSON.
