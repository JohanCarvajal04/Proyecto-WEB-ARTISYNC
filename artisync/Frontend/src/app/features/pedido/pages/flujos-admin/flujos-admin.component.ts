import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { FlujoTrabajoService } from '../../services/flujo-trabajo.service';
import {
  RespuestaFlujoTrabajo,
  RespuestaEtapaConfig,
  PeticionCrearFlujoTrabajo,
  PeticionEtapaConfig
} from '../../models/pedido.model';

@Component({
  selector: 'app-flujos-admin',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './flujos-admin.component.html'
})
export class FlujosAdminComponent implements OnInit {
  flujos: RespuestaFlujoTrabajo[] = [];
  loading = true;
  error = '';

  // Alta de flujo: aquí las etapas sí viajan en el payload, porque
  // `crearFlujoTrabajo` las persiste.
  showForm = false;
  form: PeticionCrearFlujoTrabajo = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
  saving = false;
  nuevaEtapa: PeticionEtapaConfig = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false };

  /**
   * Edición de un flujo existente.
   *
   * No reutiliza el formulario de alta a propósito: `PUT /flujos/{id}` solo
   * guarda nombre y descripción e ignora `peticion.etapas`, así que editar la
   * lista en el formulario descartaba los cambios sin avisar. Las etapas se
   * gestionan una a una contra `/etapas`, que es lo que define la guía M4 §4.1.
   */
  gestionando: RespuestaFlujoTrabajo | null = null;
  datosFlujo = { nombreFlujo: '', descripcionFlujo: '' };
  guardandoDatos = false;
  etapaNueva: PeticionEtapaConfig = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false };
  etapaEnCurso: number | null = null;

  constructor(private flujoService: FlujoTrabajoService) {}

  ngOnInit(): void {
    this.cargarFlujos();
  }

  cargarFlujos(): void {
    this.loading = true;
    this.flujoService.listarFlujos().subscribe({
      next: (data) => { this.flujos = data; this.loading = false; },
      error: (err) => { this.error = err.error?.message || 'Error al cargar flujos'; this.loading = false; }
    });
  }

  // ── Alta de flujo ─────────────────────────────────────────────────────────

  openCreate(): void {
    this.form = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
    this.nuevaEtapa = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false };
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
  }

  addEtapa(): void {
    if (!this.nuevaEtapa.nombreEtapa.trim()) return;
    this.form.etapas.push({ ...this.nuevaEtapa, nombreEtapa: this.nuevaEtapa.nombreEtapa.trim() });
    this.nuevaEtapa = { nombreEtapa: '', numeroOrden: this.form.etapas.length + 1, esEtapaFinal: false };
  }

  removeEtapa(index: number): void {
    this.form.etapas.splice(index, 1);
    this.form.etapas.forEach((e, i) => e.numeroOrden = i + 1);
  }

  onSubmit(): void {
    if (!this.form.nombreFlujo || this.form.etapas.length === 0) {
      this.error = 'Nombre y al menos una etapa son requeridos';
      return;
    }

    this.saving = true;
    this.error = '';

    this.flujoService.crearFlujo(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.cargarFlujos();
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al guardar flujo';
        this.saving = false;
      }
    });
  }

  // ── Gestión de un flujo existente ─────────────────────────────────────────

  openEdit(flujo: RespuestaFlujoTrabajo): void {
    this.gestionando = flujo;
    this.datosFlujo = {
      nombreFlujo: flujo.nombreFlujo,
      descripcionFlujo: flujo.descripcionFlujo
    };
    this.etapaNueva = { nombreEtapa: '', numeroOrden: flujo.etapas.length + 1, esEtapaFinal: false };
    this.error = '';
  }

  cerrarGestion(): void {
    this.gestionando = null;
  }

  get etapasOrdenadas(): RespuestaEtapaConfig[] {
    return [...(this.gestionando?.etapas ?? [])].sort((a, b) => a.numeroOrden - b.numeroOrden);
  }

  guardarDatosFlujo(): void {
    const flujo = this.gestionando;
    if (!flujo || !this.datosFlujo.nombreFlujo.trim()) return;

    this.guardandoDatos = true;
    this.error = '';

    // Se mandan las etapas actuales por compatibilidad del DTO; el backend las
    // ignora en esta operación.
    this.flujoService.actualizarFlujo(flujo.idFlujo, {
      nombreFlujo: this.datosFlujo.nombreFlujo.trim(),
      descripcionFlujo: this.datosFlujo.descripcionFlujo,
      etapas: []
    }).subscribe({
      next: (actualizado) => {
        this.guardandoDatos = false;
        this.refrescar(actualizado);
      },
      error: (err) => {
        this.error = err.error?.message || 'No se pudieron guardar los datos del flujo';
        this.guardandoDatos = false;
      }
    });
  }

  agregarEtapaAlFlujo(): void {
    const flujo = this.gestionando;
    const nombre = this.etapaNueva.nombreEtapa.trim();
    if (!flujo || !nombre) return;

    this.etapaEnCurso = -1;
    this.error = '';

    this.flujoService.agregarEtapa(flujo.idFlujo, {
      nombreEtapa: nombre,
      numeroOrden: this.etapasOrdenadas.length + 1,
      esEtapaFinal: this.etapaNueva.esEtapaFinal
    }).subscribe({
      next: (actualizado) => {
        this.etapaEnCurso = null;
        this.etapaNueva = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false };
        this.refrescar(actualizado);
      },
      error: (err) => {
        this.etapaEnCurso = null;
        this.error = err.error?.message || 'No se pudo añadir la etapa';
      }
    });
  }

  alternarEtapaFinal(etapa: RespuestaEtapaConfig): void {
    const flujo = this.gestionando;
    if (!flujo || this.etapaEnCurso !== null) return;

    this.etapaEnCurso = etapa.idFlujoEtapa;
    this.flujoService.actualizarEtapa(flujo.idFlujo, etapa.idFlujoEtapa, {
      nombreEtapa: etapa.nombreEtapa,
      numeroOrden: etapa.numeroOrden,
      esEtapaFinal: !etapa.esEtapaFinal
    }).subscribe({
      next: (actualizado) => {
        this.etapaEnCurso = null;
        this.refrescar(actualizado);
      },
      error: (err) => {
        this.etapaEnCurso = null;
        this.error = err.error?.message || 'No se pudo actualizar la etapa';
      }
    });
  }

  /** Reordena intercambiando `numeroOrden` con la etapa vecina. */
  moverEtapa(indice: number, direccion: -1 | 1): void {
    const flujo = this.gestionando;
    const etapas = this.etapasOrdenadas;
    const destino = indice + direccion;
    if (!flujo || destino < 0 || destino >= etapas.length || this.etapaEnCurso !== null) return;

    const actual = etapas[indice];
    const vecina = etapas[destino];

    this.etapaEnCurso = actual.idFlujoEtapa;
    forkJoin([
      this.flujoService.actualizarEtapa(flujo.idFlujo, actual.idFlujoEtapa, {
        nombreEtapa: actual.nombreEtapa,
        numeroOrden: vecina.numeroOrden,
        esEtapaFinal: actual.esEtapaFinal
      }),
      this.flujoService.actualizarEtapa(flujo.idFlujo, vecina.idFlujoEtapa, {
        nombreEtapa: vecina.nombreEtapa,
        numeroOrden: actual.numeroOrden,
        esEtapaFinal: vecina.esEtapaFinal
      })
    ]).subscribe({
      next: ([, ultimo]) => {
        this.etapaEnCurso = null;
        this.refrescar(ultimo);
      },
      error: (err) => {
        this.etapaEnCurso = null;
        this.error = err.error?.message || 'No se pudo reordenar la etapa';
        // El primer PUT pudo haber pasado: se recarga para no dejar la vista mintiendo.
        this.cargarFlujos();
      }
    });
  }

  eliminarEtapaDelFlujo(etapa: RespuestaEtapaConfig): void {
    const flujo = this.gestionando;
    if (!flujo) return;
    if (!confirm(`¿Eliminar la etapa «${etapa.nombreEtapa}» de este flujo?`)) return;

    this.etapaEnCurso = etapa.idFlujoEtapa;
    this.flujoService.eliminarEtapa(flujo.idFlujo, etapa.idFlujoEtapa).subscribe({
      next: () => {
        this.etapaEnCurso = null;
        // El DELETE no devuelve el flujo, así que se relee.
        this.flujoService.obtenerFlujo(flujo.idFlujo).subscribe({
          next: (actualizado) => this.refrescar(actualizado),
          error: () => this.cargarFlujos()
        });
      },
      error: (err) => {
        this.etapaEnCurso = null;
        this.error = err.error?.message || 'No se pudo eliminar la etapa';
      }
    });
  }

  /** Sustituye el flujo en la lista y en el panel con la versión del servidor. */
  private refrescar(actualizado: RespuestaFlujoTrabajo): void {
    this.flujos = this.flujos.map(f => f.idFlujo === actualizado.idFlujo ? actualizado : f);
    this.gestionando = actualizado;
  }
}
