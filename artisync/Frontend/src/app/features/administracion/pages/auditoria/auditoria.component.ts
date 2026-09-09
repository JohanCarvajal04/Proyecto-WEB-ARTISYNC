import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../../services/auditoria.service';
import {
  EventoAuditoria, EventoAuditoriaResumen, FiltroAuditoria,
  MODULOS_AUDITORIA, RESULTADOS_AUDITORIA
} from '../../models/auditoria.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { BotonExportarComponent } from '../../../../shared/components/boton-exportar/boton-exportar.component';
import { FormatoReporte, OpcionesExportacion } from '../../../../shared/models/formato-reporte.model';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';
import { rangoFechasInvertido } from '../../../../shared/utils/rango-fechas';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [FormsModule, BotonExportarComponent],
  templateUrl: './auditoria.component.html'
})
export class AuditoriaComponent implements OnInit {

  private auditoriaService = inject(AuditoriaService);
  private toastService = inject(ToastService);

  readonly modulos = MODULOS_AUDITORIA;
  readonly resultados = RESULTADOS_AUDITORIA;

  readonly pagina = signal<Pagina<EventoAuditoriaResumen>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  readonly acciones = signal<string[]>([]);

  readonly filtro = signal<FiltroAuditoria>({});

  readonly detalle = signal<EventoAuditoria | null>(null);
  readonly cargandoDetalle = signal<boolean>(false);

  readonly exportando = signal<boolean>(false);

  ngOnInit(): void {
    this.auditoriaService.listarAcciones().subscribe({
      next: (acciones) => this.acciones.set(acciones),
      error: () => this.acciones.set([])
    });
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.auditoriaService.listar(this.filtro(), page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar la bitácora de auditoría');
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  actualizarFiltro<K extends keyof FiltroAuditoria>(campo: K, valor: FiltroAuditoria[K]): void {
    this.filtro.update(f => ({ ...f, [campo]: valor }));
  }

  aplicarFiltros(): void {
    if (rangoFechasInvertido(this.filtro())) {
      this.toastService.error('La fecha "Desde" no puede ser posterior a "Hasta".');
      return;
    }
    this.cargar(0);
  }

  limpiarFiltros(): void {
    this.filtro.set({});
    this.cargar(0);
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  verDetalle(evento: EventoAuditoriaResumen): void {
    this.cargandoDetalle.set(true);
    this.auditoriaService.obtener(evento.idEventoAuditoria).subscribe({
      next: (completo) => {
        this.detalle.set(completo);
        this.cargandoDetalle.set(false);
      },
      error: () => {
        this.toastService.error('No se pudo cargar el detalle del evento');
        this.cargandoDetalle.set(false);
      }
    });
  }

  cerrarDetalle(): void {
    this.detalle.set(null);
  }

  exportar(opcion: FormatoReporte | OpcionesExportacion): void {
    if (rangoFechasInvertido(this.filtro())) {
      this.toastService.error('La fecha "Desde" no puede ser posterior a "Hasta".');
      return;
    }

    const formato: FormatoReporte = typeof opcion === 'string' ? opcion : opcion.formato;
    const page: number | undefined = typeof opcion === 'object' ? opcion.page : undefined;
    const size: number | undefined = typeof opcion === 'object' ? opcion.size : undefined;
    const todasLasPartes: boolean = typeof opcion === 'object' ? !!opcion.todasLasPartes : false;

    if (todasLasPartes) {
      this.descargarTodasLasPartes(formato, size);
      return;
    }

    this.exportando.set(true);
    this.auditoriaService.exportar(this.filtro(), formato, page, size).subscribe({
      next: (respuesta) => {
        this.exportando.set(false);
        const sufijo = page !== undefined ? `_parte_${page + 1}` : '';
        descargarRespuesta(respuesta, `auditoria${sufijo}_${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
      },
      error: async (err) => {
        this.exportando.set(false);
        const mensaje = await mensajeErrorBlob(err, 'No se pudo exportar la bitácora');
        this.toastService.error(mensaje);
      }
    });
  }

  private descargarTodasLasPartes(formato: FormatoReporte, tamanoLote?: number): void {
    const total = this.pagina().totalElementos;
    const tope = tamanoLote ?? 5000;
    const totalPartes = Math.max(1, Math.ceil(total / tope));
    let parteActual = 0;

    this.exportando.set(true);
    this.toastService.info(`Iniciando descarga de ${totalPartes} partes (${formato})...`);

    const descargarSiguiente = () => {
      if (parteActual >= totalPartes) {
        this.exportando.set(false);
        this.toastService.success(`Descarga completada: ${totalPartes} partes descargadas con éxito.`);
        return;
      }

      const p = parteActual;
      this.auditoriaService.exportar(this.filtro(), formato, p, tope).subscribe({
        next: (respuesta) => {
          descargarRespuesta(respuesta, `auditoria_parte_${p + 1}_${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
          parteActual++;
          setTimeout(descargarSiguiente, 600);
        },
        error: async (err) => {
          this.exportando.set(false);
          const mensaje = await mensajeErrorBlob(err, `Error al descargar parte ${p + 1}`);
          this.toastService.error(mensaje);
        }
      });
    };

    descargarSiguiente();
  }

  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  json(valor: unknown): string {
    return JSON.stringify(valor, null, 2);
  }

  claseBadge(resultado: string): string {
    switch (resultado) {
      case 'EXITO': return 'bg-emerald-50 text-emerald-700';
      case 'FALLIDO': return 'bg-rose-50 text-rose-700';
      case 'DENEGADO': return 'bg-amber-50 text-amber-700';
      default: return 'bg-slate-50 text-slate-600';
    }
  }

  claseModulo(modulo: string): string {
    switch (modulo) {
      case 'SEGURIDAD':    return 'bg-purple-100 text-purple-700 ring-1 ring-purple-200';
      case 'SISTEMA':      return 'bg-slate-100 text-slate-600 ring-1 ring-slate-200';
      case 'PORTAFOLIO':   return 'bg-blue-100 text-blue-700 ring-1 ring-blue-200';
      case 'CATALOGO':     return 'bg-indigo-100 text-indigo-700 ring-1 ring-indigo-200';
      case 'PEDIDOS':      return 'bg-orange-100 text-orange-700 ring-1 ring-orange-200';
      case 'FINANZAS':     return 'bg-emerald-100 text-emerald-700 ring-1 ring-emerald-200';
      case 'COMUNICACION': return 'bg-sky-100 text-sky-700 ring-1 ring-sky-200';
      case 'SOCIAL':       return 'bg-pink-100 text-pink-700 ring-1 ring-pink-200';
      default:             return 'bg-slate-100 text-slate-600 ring-1 ring-slate-200';
    }
  }
}
