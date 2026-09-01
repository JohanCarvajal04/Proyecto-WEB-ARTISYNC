# Histórico del SRS

Archivo de versiones anteriores de la Especificación de Requisitos de Software, exigido por el apartado A.3.1 de la guía de la Entrega Final ("la versión anterior queda archivada en `docs/requisitos/historico/`").

La versión vigente es siempre [`../SRS.md`](../SRS.md), y su PDF firmado, `../SRS-v1.0.0.pdf`.

## Política de versionado

Cada etiqueta de entrega deja aquí una fotografía del SRS tal como estaba en ese punto del historial. Los archivos `.md` de esta carpeta **no se editan**: se extraen del historial de Git, de modo que reproducen exactamente el estado etiquetado y no una reconstrucción posterior.

Para regenerarlos o añadir una versión nueva:

```bash
git show v0.9.0-rc:docs/requisitos/SRS.md > docs/requisitos/historico/SRS-v0.9.0-rc.md
```

## Contenido

| Archivo                 | Origen                                     | Nota                                                                                             |
| ----------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| `SRS-v0.7.1.md`         | `git show v0.7.1:docs/requisitos/SRS.md`   | Cierre de observaciones de las Entregas 1A y 1B.                                                  |
| `SRS-v0.9.0-rc.md`      | `git show v0.9.0-rc:docs/requisitos/SRS.md` | Tercera Entrega. **Idéntico byte a byte a `SRS-v0.7.1.md`**: el SRS no cambió entre ambas etiquetas. Se conservan los dos porque cada uno documenta el estado de su entrega. |
| `SRS-2026-07-30.pdf`    | Archivo suelto que estaba en `docs/requisitos/` | PDF generado el 30-07-2026, sin versión en el nombre y sin pipeline que lo produjera. Se archiva con su fecha real en lugar de atribuirle una etiqueta que no puede confirmarse: es posterior a `v0.9.0-rc` (24-07) y anterior a `v1.0.0`. |

La etiqueta `v0.7.0` no aparece aquí porque en ese punto del historial `docs/requisitos/SRS.md` todavía no existía: el corpus de requisitos vivía en `Entrega 1A.docx` (migrado a LaTeX el 01-09-2026, ver [`entrega-1a.tex`](entrega-1a.tex) / [`entrega-1a.pdf`](entrega-1a.pdf) en este mismo directorio). La formalización del SRS conforme a ISO/IEC/IEEE 29148:2018 se hizo en `v0.9.0-rc`, según consta en [`../CHANGELOG-REQ.md`](../CHANGELOG-REQ.md).
