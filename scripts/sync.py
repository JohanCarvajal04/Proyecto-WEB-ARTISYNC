import os
import glob

# Rutas
raiz = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
origen = os.path.join(raiz, "db", "procs")
destino = os.path.join(raiz, "artisync", "Backend", "src", "main", "resources", "db", "migration", "R__procedimientos.sql")

# Archivos a procesar
patrones = ["fn_*.sql", "sp_*.sql", "V8__*.sql"]
archivos = []
for patron in patrones:
    archivos.extend(glob.glob(os.path.join(origen, patron)))

# Ordenar alfabéticamente
archivos.sort(key=lambda x: os.path.basename(x))

if not archivos:
    print(f"ERROR: {origen} no contiene ningún archivo .sql")
    exit(1)

with open(destino, "w", encoding="utf-8", newline="\n") as out:
    out.write("-- ===========================================================================\n")
    out.write("-- R__procedimientos.sql — ARCHIVO GENERADO. NO EDITAR A MANO.\n")
    out.write("-- ===========================================================================\n")
    out.write("--\n")
    out.write("-- Generado por scripts/sync-procs.sh a partir de db/procs/*.sql, que es la\n")
    out.write("-- ubicacion canonica de las rutinas (apartado A.2.1 de la guia de la Entrega\n")
    out.write("-- Final). Para modificar una rutina se edita su archivo en db/procs/ y se\n")
    out.write("-- ejecuta `make sync-procs`.\n")
    out.write("--\n")
    out.write("-- Migracion REPETIBLE: Flyway la reaplica cada vez que cambia su checksum.\n")
    out.write("-- Todas las rutinas usan CREATE OR REPLACE, por lo que reaplicarla es inocuo.\n")
    out.write("--\n")
    out.write(f"-- Rutinas incluidas ({len(archivos)}):\n")
    for f in archivos:
        out.write(f"--   - {os.path.basename(f)}\n")
    out.write("-- ===========================================================================\n\n")

    for f in archivos:
        out.write("\n")
        out.write("-- ---------------------------------------------------------------------------\n")
        out.write(f"-- Origen: db/procs/{os.path.basename(f)}\n")
        out.write("-- ---------------------------------------------------------------------------\n")
        with open(f, "r", encoding="utf-8") as fin:
            out.write(fin.read())
        out.write("\n")

print(f"Generado {destino} a partir de {len(archivos)} rutinas en db/procs/.")
