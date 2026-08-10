import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FlujoTrabajoService } from '../../services/flujo-trabajo.service';
import { RespuestaFlujoTrabajo, PeticionCrearFlujoTrabajo, PeticionEtapaConfig } from '../../models/pedido.model';

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

  // Form state
  showForm = false;
  editingFlujo: RespuestaFlujoTrabajo | null = null;
  form: PeticionCrearFlujoTrabajo = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
  saving = false;

  // Add etapa
  nuevaEtapa: PeticionEtapaConfig = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false };

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

  openCreate(): void {
    this.editingFlujo = null;
    this.form = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
    this.showForm = true;
  }

  openEdit(flujo: RespuestaFlujoTrabajo): void {
    this.editingFlujo = flujo;
    this.form = {
      nombreFlujo: flujo.nombreFlujo,
      descripcionFlujo: flujo.descripcionFlujo,
      etapas: flujo.etapas.map(e => ({
        nombreEtapa: e.nombreEtapa,
        numeroOrden: e.numeroOrden,
        esEtapaFinal: e.esEtapaFinal
      }))
    };
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingFlujo = null;
  }

  addEtapa(): void {
    if (!this.nuevaEtapa.nombreEtapa) return;
    this.form.etapas.push({ ...this.nuevaEtapa });
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

    const obs$ = this.editingFlujo
      ? this.flujoService.actualizarFlujo(this.editingFlujo.idFlujo, this.form)
      : this.flujoService.crearFlujo(this.form);

    obs$.subscribe({
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

  eliminarEtapaDelFlujo(flujoId: number, etapaId: number): void {
    if (!confirm('¿Eliminar esta etapa?')) return;
    this.flujoService.eliminarEtapa(flujoId, etapaId).subscribe({
      next: () => this.cargarFlujos()
    });
  }
}
