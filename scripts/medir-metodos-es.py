#!/usr/bin/env python3
"""Mide el porcentaje de metodos con token en espanol sobre el backend
(src/main/java), para el criterio E1 de la rubrica del examen final
(docs/observaciones/REVISION-EXTERNA-EXAMEN-FINAL-20260911.md, punto 2):
"nombres del codigo en ingles". La cifra de tipos ya se mide de forma
directa (grep sobre nombres de clase/interfaz); esta cifra cubre el otro
factor del criterio, que la rubrica evalua tomando el peor de los dos.

Metodologia: se detectan declaraciones de metodo (public/private/protected)
en cada archivo .java de src/main/java, se separa el identificador en
palabras por camelCase, y se marca el metodo como "en espanol" si alguna
palabra coincide con una lista de raices frecuentes en este codebase
(verbos como crear/listar/obtener, sustantivos de dominio como pedido/
contrato/plantilla, etc.). Es una heuristica de grep, no un parser de
Java: puede haber falsos positivos/negativos en identificadores ambiguos,
pero es estable y reproducible para comparar antes/despues de una pasada
de renombrado.

Uso:
    python scripts/medir-metodos-es.py                       # todo el backend
    python scripts/medir-metodos-es.py src/main/java/.../pedido  # un paquete

Exit code: 0 siempre que el script corra sin errores de E/S (mide
cobertura, no es un gate de CI).
"""
import re
import sys
import collections
import pathlib

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

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
token sesion fecha hora estado tipo categoria etiqueta direccion telefono
contrato plantilla firma entrega entregable retiro garantia comision perfil
portafolio item certificado verificacion identidad documento etapa flujo
ticket propuesta terminos seguimiento historial sorteo ganador participante
""".split())

METHOD_DECL = re.compile(
    r'(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?'
    r'(?:<[^>]+>\s+)?[\w\<\>\[\]\., ?]+?\s+(\w+)\s*\([^;{]*\)\s*(?:throws[^{]+)?\{'
)


def split_camel(name):
    parts = re.findall(r'[A-Z]?[a-z0-9]+|[A-Z]+(?=[A-Z]|$)', name)
    return [p.lower() for p in parts]


def main():
    root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/java")

    total = 0
    spanish = 0
    by_pkg = collections.Counter()
    by_pkg_total = collections.Counter()

    for f in root.rglob("*.java"):
        parts = f.parts
        try:
            idx = parts.index("artisync")
            layer = parts[idx + 1] if len(parts) > idx + 1 else "root"
            domain = parts[idx + 2] if len(parts) > idx + 2 and not parts[idx + 2].endswith(".java") else "_top"
            pkg_bucket = f"{layer}/{domain}"
        except ValueError:
            pkg_bucket = "other"

        text = f.read_text(encoding="utf-8", errors="ignore")
        for m in METHOD_DECL.finditer(text):
            name = m.group(1)
            if name in ("if", "for", "while", "switch", "catch", "synchronized"):
                continue
            total += 1
            by_pkg_total[pkg_bucket] += 1
            words = split_camel(name)
            if any(w in SPANISH_TOKENS for w in words):
                spanish += 1
                by_pkg[pkg_bucket] += 1

    if total == 0:
        print("No se detectaron metodos en la ruta indicada.")
        return

    print(f"Total metodos detectados: {total}")
    print(f"Con token en espanol: {spanish} ({100 * spanish / total:.1f}%)")
    print()
    print("Por paquete (espanol/total):")
    for pkg, tot in by_pkg_total.most_common():
        print(f"  {pkg:20s} {by_pkg[pkg]:4d} / {tot:4d}  ({100 * by_pkg[pkg] / tot:.1f}%)")


if __name__ == "__main__":
    main()
