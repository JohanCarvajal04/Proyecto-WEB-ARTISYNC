import csv
import matplotlib.pyplot as plt
from pathlib import Path

# Constantes para el calculo
ITEMS_IMPARES = {1, 3, 5, 7, 9}

def puntaje_sus(respuestas):
    total = 0.0
    for idx, valor in enumerate(respuestas, start=1):
        if idx in ITEMS_IMPARES:
            total += valor - 1
        else:
            total += 5 - valor
    return total * 2.5

ruta_csv = Path("docs/mediciones/sus/sus-raw.csv")
puntajes = []

with ruta_csv.open(newline="", encoding="utf-8") as f:
    lector = csv.reader(f)
    next(lector) # saltar encabezado
    for fila in lector:
        if not fila: continue
        respuestas = [int(x) for x in fila[1:11]]
        puntajes.append(puntaje_sus(respuestas))

plt.figure(figsize=(6, 8))
plt.boxplot(puntajes, patch_artist=True, 
            boxprops=dict(facecolor='lightblue', color='blue'), 
            medianprops=dict(color='red', linewidth=2),
            flierprops=dict(markerfacecolor='r', marker='o'))
plt.title("Distribución de Puntuaciones SUS (n=16)")
plt.ylabel("Puntuación (0-100)")
plt.ylim(0, 105)
plt.axhline(y=68, color='green', linestyle='--', label='Umbral de Aceptación (68)')
plt.legend()
plt.grid(axis='y', linestyle='--', alpha=0.7)
plt.tight_layout()

ruta_salida = Path("docs/mediciones/sus/boxplot-sus.png")
plt.savefig(ruta_salida, dpi=150)
print(f"Gráfico guardado exitosamente en {ruta_salida}")
