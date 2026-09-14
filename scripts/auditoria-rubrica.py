#!/usr/bin/env python3
"""Audita el repositorio con el mismo criterio que el examen final del PFC
(docs/observaciones/REVISION-EXTERNA-EXAMEN-FINAL-20260911.md), para los 9
puntos que quedaron por debajo de "Completo": P3, P6, P7, P8, P11, P12, E1,
E2, E3.

A diferencia de scripts/medir-metodos-es.py y scripts/medir-javadoc.py (que
miden un alcance ya acordado, mas estrecho), este script mide TODO el
repositorio -- incluidos comentarios, interfaces, repositorios y .tex
historicos -- porque asi es como conto el evaluador. El objetivo de cada
seccion es llegar a 0 incumplimientos.

Es de solo lectura: no modifica ningun archivo, solo imprime.

Uso:
    python scripts/auditoria-rubrica.py             # todas las secciones
    python scripts/auditoria-rubrica.py p12 e1 p7   # solo estas secciones

Secciones disponibles: p3 p6 p7 p8 p11 p12 e1 e2 e3
"""
import collections
import csv
import os
import re
import subprocess
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(RAIZ, "artisync", "Backend")
SRC_MAIN = os.path.join(BACKEND, "src", "main", "java", "uteq", "edu", "ec", "artisync")
SRC_TEST = os.path.join(BACKEND, "src", "test", "java", "uteq", "edu", "ec", "artisync")
DOCS = os.path.join(RAIZ, "docs")

SEP = "=" * 78


def titulo(t):
    print(f"\n{SEP}\n{t}\n{SEP}")


# ---------------------------------------------------------------------------
# Utilidades comunes
# ---------------------------------------------------------------------------

def todos_los_tex():
    for dirpath, _dirnames, filenames in os.walk(DOCS):
        for fn in filenames:
            if fn.endswith(".tex"):
                yield os.path.join(dirpath, fn)


def quitar_comentarios_tex(texto):
    """Quita comentarios de linea (% sin escapar) para no contar \\label ni
    \\ref dentro de un comentario. No maneja verbatim/lstlisting a proposito:
    el conteo de labels de la rubrica los incluye (ver docstring del modulo)."""
    out_lines = []
    for linea in texto.split("\n"):
        m = re.search(r'(?<!\\)%', linea)
        out_lines.append(linea[:m.start()] if m else linea)
    return "\n".join(out_lines)


def todos_los_java(root):
    for dirpath, _dirnames, filenames in os.walk(root):
        for fn in filenames:
            if fn.endswith(".java"):
                yield os.path.join(dirpath, fn)


def paquete_de(ruta, base):
    rel = os.path.relpath(ruta, base)
    partes = rel.split(os.sep)[:-1]
    return partes


# ---------------------------------------------------------------------------
# P12 -- Etiquetas referenciadas en TODOS los .tex del repositorio
# ---------------------------------------------------------------------------

REF_CMDS = re.compile(
    r'\\(?:ref|autoref|cref|Cref|pageref|nameref|eqref)\{([^}]+)\}'
    r'|\\hyperref\[([^\]]+)\]'
)
LABEL_CMD = re.compile(r'\\label\{([^}]+)\}')


def seccion_p12():
    titulo("P12 -- Etiquetas referenciadas (todos los .tex del repositorio)")

    labels_por_archivo = {}
    refs = collections.Counter()

    for ruta in todos_los_tex():
        with open(ruta, encoding="utf-8", errors="ignore") as f:
            texto = quitar_comentarios_tex(f.read())
        etiquetas = LABEL_CMD.findall(texto)
        if etiquetas:
            labels_por_archivo[ruta] = etiquetas
        for m in REF_CMDS.finditer(texto):
            key = m.group(1) or m.group(2)
            for k in key.split(","):
                refs[k.strip()] += 1

    total_labels = sum(len(v) for v in labels_por_archivo.values())
    print(f"Archivos .tex con \\label: {len(labels_por_archivo)}")
    print(f"Total \\label: {total_labels}")
    print(f"Total comandos de referencia (\\ref/\\autoref/\\cref/\\pageref/\\nameref/\\hyperref): {sum(refs.values())}")

    huerfanas_por_archivo = collections.Counter()
    huerfanas_detalle = []
    for ruta, etiquetas in labels_por_archivo.items():
        for lab in etiquetas:
            if refs[lab] == 0:
                huerfanas_por_archivo[ruta] += 1
                huerfanas_detalle.append((ruta, lab))

    total_huerfanas = len(huerfanas_detalle)
    print(f"\nEtiquetas SIN ninguna referencia: {total_huerfanas} de {total_labels}")
    if huerfanas_por_archivo:
        print("\nPor archivo:")
        for ruta, n in huerfanas_por_archivo.most_common():
            rel = os.path.relpath(ruta, RAIZ)
            print(f"  {n:4d}  {rel}")
    if total_huerfanas:
        print("\nDetalle (primeras 40):")
        for ruta, lab in huerfanas_detalle[:40]:
            rel = os.path.relpath(ruta, RAIZ)
            print(f"  {rel}: {lab}")


# ---------------------------------------------------------------------------
# E1 -- Nombres del codigo en ingles (tipos, TODOS los metodos, paquetes)
# ---------------------------------------------------------------------------

SPANISH_TOKENS = set("""
obtener obten crear crea actualizar actualiza eliminar elimina borrar guardar guarda
buscar busca listar lista validar valida calcular calcula generar genera enviar envia
registrar registra cancelar cancela aprobar aprueba rechazar rechaza asignar asigna
verificar verifica establecer establece agregar agrega quitar remover mover copiar
convertir convierte transformar comprobar comprueba procesar procesa notificar notifica
cargar carga descargar descarga subir sube bajar baja mostrar muestra ocultar
insertar inserta modificar modifica revisar revisa contar cuenta sumar suma restar
resta filtrar filtra ordenar ordena reiniciar reinicia iniciar inicia finalizar finaliza
cerrar cierra abrir abre exportar importar renovar renueva ejecutar ejecuta
consultar consulta responder responde solicitar solicita permitir permite denegar
niega editar edita duplicar duplica archivar archiva restaurar restaura programar
programa sincronizar sincroniza seguir dejar reintentar desactivar
nombre correo contrasena usuario cliente proveedor pedido producto servicio
solicitud pago factura reporte informe respaldo copia seguridad archivo carpeta
imagen video comentario calificacion resena mensaje notificacion rol permiso
sesion fecha hora estado tipo categoria etiqueta direccion telefono
contrato plantilla firma entrega entregable retiro garantia comision perfil
portafolio certificado verificacion identidad documento etapa flujo
propuesta terminos seguimiento historial sorteo ganador participante
pais
""".split())
# Nota: "token" e "item"/"ticket" se excluyen a proposito (ver
# scripts/medir-metodos-es.py) para no marcar prestamos del ingles como
# falsos positivos (generateToken, uploadItem, createTicket...).

PAQUETES_ESPANOL = {
    "auditoria", "catalogo", "comunicacion", "pedido", "perfil", "respaldo",
    "seguridad", "peticion", "respuesta", "almacenamiento", "ia", "imagen",
    "reporte",
}

# Cualquier declaracion de metodo con visibilidad Java, CON o SIN cuerpo
# (para cubrir interfaces y repositorios ademas de clases). Exige que el
# nombre este seguido de "(" y que la linea no sea una llamada.
METHOD_DECL_ANY = re.compile(
    r'(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?(?:default\s+)?'
    r'(?:<[^>]+>\s+)?[\w\<\>\[\]\., ?]+?\s+(\w+)\s*\([^;{]*\)\s*(?:throws[^{;]+)?\s*[{;]'
)

TYPE_DECL = re.compile(
    r'(?:public|private|protected)?\s*(?:abstract\s+|final\s+|static\s+)*'
    r'(?:class|interface|enum|record)\s+(\w+)'
)


def split_camel(name):
    parts = re.findall(r'[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)', name)
    return [p.lower() for p in parts]


def es_espanol(name):
    return any(w in SPANISH_TOKENS for w in split_camel(name))


def seccion_e1():
    titulo("E1 -- Nombres del codigo en ingles (tipos + TODOS los metodos + paquetes)")

    total_tipos = 0
    tipos_es = []
    total_metodos = 0
    metodos_es_por_pkg = collections.Counter()
    metodos_total_por_pkg = collections.Counter()
    metodos_es_detalle = []
    paquetes_es = set()

    for f in todos_los_java(SRC_MAIN):
        partes = paquete_de(f, SRC_MAIN)
        for p in partes:
            if p in PAQUETES_ESPANOL:
                paquetes_es.add("/".join(partes))
        pkg_bucket = "/".join(partes[:2]) if len(partes) >= 2 else "/".join(partes) or "_top"

        text = f.read_text(encoding="utf-8", errors="ignore") if hasattr(f, "read_text") else open(f, encoding="utf-8", errors="ignore").read()

        for m in TYPE_DECL.finditer(text):
            name = m.group(1)
            total_tipos += 1
            if es_espanol(name):
                tipos_es.append((os.path.relpath(f, RAIZ), name))

        for m in METHOD_DECL_ANY.finditer(text):
            name = m.group(1)
            if name in ("if", "for", "while", "switch", "catch", "synchronized", "new"):
                continue
            total_metodos += 1
            metodos_total_por_pkg[pkg_bucket] += 1
            if es_espanol(name):
                metodos_es_por_pkg[pkg_bucket] += 1
                metodos_es_detalle.append((os.path.relpath(f, RAIZ), name))

    total_metodos_es = sum(metodos_es_por_pkg.values())

    print(f"Paquetes con segmento en espanol: {len(paquetes_es)}")
    for p in sorted(paquetes_es):
        print(f"  {p}")

    print(f"\nTipos detectados: {total_tipos}")
    print(f"Tipos con nombre en espanol: {len(tipos_es)}")
    for ruta, name in tipos_es:
        print(f"  {ruta}: {name}")

    print(f"\nMetodos detectados (con y sin cuerpo, TODAS las capas): {total_metodos}")
    print(f"Con token en espanol: {total_metodos_es} ({100 * total_metodos_es / total_metodos:.1f}%)")
    print("\nPor paquete (espanol/total):")
    for pkg, tot in metodos_total_por_pkg.most_common():
        n = metodos_es_por_pkg[pkg]
        if n:
            print(f"  {pkg:30s} {n:4d} / {tot:4d}  ({100 * n / tot:.1f}%)")

    if "--verbose" in sys.argv:
        print("\nDetalle de metodos en espanol:")
        for ruta, name in metodos_es_detalle:
            print(f"  {ruta}: {name}")


# ---------------------------------------------------------------------------
# E2 -- Javadoc en TODOS los metodos publicos (incl. impl/**, shared/**,
#        repositorios e interfaces)
# ---------------------------------------------------------------------------

FIRMA_CLASE = re.compile(
    r'^public\s+(?!class\b|interface\b|enum\b|record\b)(?:static\s+|final\s+)*'
    r'[\w<>\[\],.?\s]+?\s+([A-Za-z_]\w*)\s*\('
)
FIRMA_INTERFAZ = re.compile(
    r'^(?!.*\b(?:class|interface|enum|record)\b)(?!@)(?!default\b)(?!static\b)'
    r'[A-Za-z_][\w<>\[\],.?\s]*?\s+([A-Za-z_]\w*)\s*\('
)


def analizar_javadoc(ruta, es_interfaz):
    with open(ruta, encoding="utf-8", errors="ignore") as f:
        lineas = f.read().split("\n")

    total = 0
    documentados = 0
    faltantes = []

    for i, linea in enumerate(lineas):
        cuerpo = linea.strip()
        if not cuerpo or cuerpo.startswith(("//", "*", "/*")):
            continue

        patron = FIRMA_INTERFAZ if es_interfaz else FIRMA_CLASE
        m = patron.match(cuerpo)
        if not m:
            continue
        if es_interfaz and (cuerpo.startswith("@") or cuerpo.startswith("default") or cuerpo.startswith("static")):
            continue

        nombre_metodo = m.group(1)
        total += 1

        j = i - 1
        while j >= 0 and (lineas[j].strip().startswith("@") or lineas[j].strip() == ""):
            j -= 1
        if j >= 0 and lineas[j].strip().endswith("*/"):
            documentados += 1
        else:
            faltantes.append((i + 1, nombre_metodo))

    return total, documentados, faltantes


def seccion_e2():
    titulo("E2 -- Javadoc en TODOS los metodos publicos (sin excluir impl/** ni shared/**)")

    resultados = []
    for f in todos_los_java(SRC_MAIN):
        rel = os.path.relpath(f, SRC_MAIN)
        with open(f, encoding="utf-8", errors="ignore") as fh:
            src = fh.read()

        capa = rel.split(os.sep)[0]
        es_interfaz_archivo = bool(re.search(r"public interface ", src)) and not re.search(r"public (?:abstract )?class ", src)
        if "class " not in src and "interface " not in src:
            continue

        total, doc, faltantes = analizar_javadoc(f, es_interfaz_archivo)
        if total == 0:
            continue
        resultados.append({
            "capa": capa,
            "archivo": rel.replace(os.sep, "/"),
            "total": total,
            "documentados": doc,
            "faltantes": faltantes,
        })

    por_capa = collections.Counter()
    doc_por_capa = collections.Counter()
    for r in resultados:
        por_capa[r["capa"]] += r["total"]
        doc_por_capa[r["capa"]] += r["documentados"]

    total = sum(por_capa.values())
    doc = sum(doc_por_capa.values())
    print(f"TOTAL (todas las capas de src/main/java): {doc}/{total} ({100 * doc / total:.1f}%)" if total else "Sin metodos detectados.")
    print("\nPor capa:")
    for capa, tot in por_capa.most_common():
        d = doc_por_capa[capa]
        print(f"  {capa:15s} {d:4d} / {tot:4d}  ({100 * d / tot:.1f}%)")

    con_deficit = [r for r in resultados if r["documentados"] < r["total"]]
    print(f"\n{len(con_deficit)} archivo(s) con deficit:")
    for r in sorted(con_deficit, key=lambda r: r["total"] - r["documentados"], reverse=True)[:40]:
        faltan = r["total"] - r["documentados"]
        print(f"  - {r['archivo']}: {r['documentados']}/{r['total']} ({faltan} sin documentar)")
        if "--verbose" in sys.argv:
            for ln, nombre in r["faltantes"]:
                print(f"      L{ln}: {nombre}")


# ---------------------------------------------------------------------------
# P6 -- Acceso uniforme con procedimientos almacenados
# ---------------------------------------------------------------------------

def seccion_p6():
    titulo("P6 -- Procedimientos almacenados: nativeQuery / @Procedure / @NamedStoredProcedureQuery")

    patrones = {
        "nativeQuery = true (real, fuera de comentario)": re.compile(r'nativeQuery\s*=\s*true'),
        "@Procedure (real)": re.compile(r'^\s*@Procedure\b', re.MULTILINE),
        "@NamedStoredProcedureQuery (real)": re.compile(r'^\s*@NamedStoredProcedureQuery\b', re.MULTILINE),
    }

    conteos = collections.Counter()
    detalle = collections.defaultdict(list)

    for f in todos_los_java(SRC_MAIN):
        with open(f, encoding="utf-8", errors="ignore") as fh:
            lineas = fh.readlines()

        # Quita bloques /* ... */ y lineas // para no contar menciones en comentarios/Javadoc.
        en_bloque = False
        limpio = []
        for linea in lineas:
            l = linea
            if en_bloque:
                fin = l.find("*/")
                if fin == -1:
                    limpio.append("")
                    continue
                l = l[fin + 2:]
                en_bloque = False
            while True:
                ini = l.find("/*")
                if ini == -1:
                    break
                fin = l.find("*/", ini)
                if fin == -1:
                    l = l[:ini]
                    en_bloque = True
                    break
                l = l[:ini] + l[fin + 2:]
            l = re.sub(r'//.*', '', l)
            limpio.append(l)
        texto_limpio = "".join(limpio)

        for etiqueta, patron in patrones.items():
            n = len(patron.findall(texto_limpio))
            if n:
                conteos[etiqueta] += n
                detalle[etiqueta].append((os.path.relpath(f, RAIZ), n))

    for etiqueta in patrones:
        print(f"\n{etiqueta}: {conteos[etiqueta]}")
        for ruta, n in sorted(detalle[etiqueta], key=lambda x: -x[1]):
            print(f"  {n:3d}  {ruta}")

    print(f"\nObjetivo: nativeQuery=true -> 0 (todas las rutinas via @Procedure / @NamedStoredProcedureQuery).")


# ---------------------------------------------------------------------------
# P7 -- Cobertura JaCoCo por paquete
# ---------------------------------------------------------------------------

def seccion_p7(umbral=0.70):
    titulo(f"P7 -- Cobertura JaCoCo por paquete (umbral {umbral:.0%} lineas y ramas)")

    candidatos = [
        os.path.join(BACKEND, "target", "site", "jacoco", "jacoco.csv"),
        os.path.join(DOCS, "mediciones", "jacoco", "html", "jacoco.csv"),
    ]
    ruta_csv = next((c for c in candidatos if os.path.isfile(c)), None)
    if not ruta_csv:
        print("No se encontro jacoco.csv. Ejecuta `mvn test` en artisync/Backend primero.")
        print("Rutas buscadas:")
        for c in candidatos:
            print(f"  {c}")
        return

    print(f"Usando: {os.path.relpath(ruta_csv, RAIZ)}\n")

    por_paquete = {}
    with open(ruta_csv, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            pkg = row["PACKAGE"]
            d = por_paquete.setdefault(pkg, collections.Counter())
            d["line_covered"] += int(row["LINE_COVERED"])
            d["line_missed"] += int(row["LINE_MISSED"])
            d["branch_covered"] += int(row["BRANCH_COVERED"])
            d["branch_missed"] += int(row["BRANCH_MISSED"])

    total_line_c = total_line_m = total_branch_c = total_branch_m = 0
    incumplen = []
    for pkg, d in sorted(por_paquete.items()):
        lt = d["line_covered"] + d["line_missed"]
        bt = d["branch_covered"] + d["branch_missed"]
        line_pct = d["line_covered"] / lt if lt else 1.0
        branch_pct = d["branch_covered"] / bt if bt else 1.0
        total_line_c += d["line_covered"]; total_line_m += d["line_missed"]
        total_branch_c += d["branch_covered"]; total_branch_m += d["branch_missed"]
        if line_pct < umbral or branch_pct < umbral:
            incumplen.append((pkg, line_pct, d["line_covered"], lt, branch_pct, d["branch_covered"], bt))

    lt = total_line_c + total_line_m
    bt = total_branch_c + total_branch_m
    print(f"Global: lineas {total_line_c}/{lt} ({100 * total_line_c / lt:.2f}%)"
          f"  ramas {total_branch_c}/{bt} ({100 * total_branch_c / bt:.2f}%)")

    print(f"\nPaquetes por debajo del {umbral:.0%}: {len(incumplen)}")
    for pkg, lp, lc, ltt, bp, bc, btt in sorted(incumplen, key=lambda x: x[1]):
        print(f"  {pkg:55s} lineas {lc:4d}/{ltt:4d} ({lp*100:5.1f}%)   ramas {bc:4d}/{btt:4d} ({bp*100:5.1f}%)")


# ---------------------------------------------------------------------------
# P11 -- Palabras del resumen y del abstract
# ---------------------------------------------------------------------------

def contar_palabras(texto):
    limpio = re.sub(r'\\[a-zA-Z]+\{[^}]*\}', ' ', texto)  # \textbf{...} etc.
    limpio = re.sub(r'\\cite\{[^}]*\}', ' ', limpio)
    limpio = re.sub(r'\\[a-zA-Z]+', ' ', limpio)
    limpio = re.sub(r'[{}%]', ' ', limpio)
    palabras = re.findall(r"[A-Za-zÁÉÍÓÚÑáéíóúñü][\wÁÉÍÓÚÑáéíóúñü'-]*", limpio)
    return len(palabras)


def seccion_p11():
    titulo("P11 -- Palabras del resumen y del abstract")

    ruta_tex = os.path.join(DOCS, "informe-final", "secciones", "00-portada-resumen.tex")
    if not os.path.isfile(ruta_tex):
        print(f"No se encontro {ruta_tex}")
        return

    with open(ruta_tex, encoding="utf-8") as f:
        texto = f.read()

    def extraer(bloque_inicio_regex):
        m = re.search(bloque_inicio_regex, texto)
        if not m:
            return None
        inicio = m.end()
        # corta en el siguiente \chapter, \section o \subsection
        m2 = re.search(r'\\(chapter|(sub)?section)\*?\{', texto[inicio:])
        fin = inicio + m2.start() if m2 else len(texto)
        return texto[inicio:fin]

    resumen = extraer(r'\\chapter\*\{Resumen[^}]*\}')
    abstract = extraer(r'\\chapter\*\{Abstract[^}]*\}')

    if resumen:
        n = contar_palabras(resumen)
        marca = "OK" if 200 <= n <= 250 else "FUERA DE RANGO"
        print(f"Resumen (.tex): {n} palabras  [{marca}, objetivo 200-250]")
    else:
        print("No se encontro la seccion 'Resumen' en el .tex")

    if abstract:
        n = contar_palabras(abstract)
        marca = "OK" if 200 <= n <= 250 else "FUERA DE RANGO"
        print(f"Abstract (.tex): {n} palabras  [{marca}, objetivo 200-250]")
    else:
        print("No se encontro la seccion 'Abstract' en el .tex")

    # Conteo sobre el PDF compilado, si existe y hay pdftotext.
    for nombre_pdf in ("main.pdf", "Informe-Final-v1.0.0.pdf"):
        ruta_pdf = os.path.join(DOCS, "informe-final", nombre_pdf)
        if not os.path.isfile(ruta_pdf):
            continue
        try:
            salida = subprocess.run(
                ["pdftotext", "-f", "1", "-l", "3", ruta_pdf, "-"],
                capture_output=True, text=True, timeout=30,
            )
            print(f"\n(informativo) primeras paginas de {nombre_pdf} extraidas con pdftotext;"
                  " revisar a mano si el resumen/abstract no se detecto arriba por diferencias de plantilla.")
        except FileNotFoundError:
            print("\npdftotext no disponible; no se pudo verificar el PDF compilado.")
            break


# ---------------------------------------------------------------------------
# P3 -- Tabla comparativa de trabajos relacionados (filas + DOI)
# ---------------------------------------------------------------------------

def seccion_p3():
    titulo("P3 -- Tabla comparativa de trabajos relacionados: filas y DOI")

    ruta_tex = os.path.join(DOCS, "informe-final", "secciones", "03-trabajos-relacionados.tex")
    ruta_bib = None
    for dirpath, _dirnames, filenames in os.walk(os.path.join(DOCS, "informe-final")):
        for fn in filenames:
            if fn.endswith(".bib"):
                ruta_bib = os.path.join(dirpath, fn)
    if not os.path.isfile(ruta_tex):
        print(f"No se encontro {ruta_tex}")
        return

    with open(ruta_tex, encoding="utf-8") as f:
        texto = f.read()

    claves = re.findall(r'\\cite[tp]?\{([^}]+)\}', texto)
    claves_tabla = set()
    m = re.search(r'\\label\{tab:comparativa-relacionados\}(.*?)\\end\{table', texto, re.DOTALL)
    if m:
        claves_tabla = set(re.findall(r'\\cite[tp]?\{([^}]+)\}', m.group(1)))
        filas = m.group(1).count(r'\\')
    print(f"Claves citadas en la tabla comparativa: {len(claves_tabla)}")
    for c in sorted(claves_tabla):
        print(f"  {c}")

    if ruta_bib and claves_tabla:
        with open(ruta_bib, encoding="utf-8", errors="ignore") as f:
            bib = f.read()
        sin_doi = []
        for clave in claves_tabla:
            m2 = re.search(re.escape(clave) + r'\s*,(.*?)\n@', bib + "\n@", re.DOTALL)
            bloque = m2.group(1) if m2 else ""
            if "doi" not in bloque.lower():
                sin_doi.append(clave)
        print(f"\nClaves de la tabla SIN campo doi en {os.path.relpath(ruta_bib, RAIZ)}: {len(sin_doi)}")
        for c in sin_doi:
            print(f"  {c}")
    else:
        print("\nNo se pudo verificar DOI (no se encontro el .bib o la tabla).")


SECCIONES = {
    "p3": seccion_p3,
    "p6": seccion_p6,
    "p7": seccion_p7,
    "p11": seccion_p11,
    "p12": seccion_p12,
    "e1": seccion_e1,
    "e2": seccion_e2,
}


def main():
    args = [a.lower() for a in sys.argv[1:] if not a.startswith("--")]
    elegidas = args if args else list(SECCIONES.keys())
    desconocidas = [a for a in elegidas if a not in SECCIONES]
    if desconocidas:
        print(f"Seccion(es) desconocida(s): {desconocidas}. Disponibles: {list(SECCIONES.keys())}")
        sys.exit(1)
    for nombre in elegidas:
        SECCIONES[nombre]()


if __name__ == "__main__":
    main()
