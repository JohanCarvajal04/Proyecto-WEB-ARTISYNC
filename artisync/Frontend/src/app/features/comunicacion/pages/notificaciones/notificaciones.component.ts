import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { NotificacionService } from '../../services/notificacion.service';
import { RespuestaNotificacion } from '../../models/comunicacion.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [],
  templateUrl: './notificaciones.component.html'
})
export class NotificacionesComponent implements OnInit {

  private notificacionService = inject(NotificacionService);
  private toast = inject(ToastService);

  readonly pagina = signal<Pagina<RespuestaNotificacion>>(paginaVacia());
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');
  readonly marcandoTodas = signal<boolean>(false);

  readonly noLeidas = this.notificacionService.noLeidas;

  readonly hayNoLeidas = computed(() => this.pagina().contenido.some(n => !n.estaLeida));

  ngOnInit(): void {
    this.cargar(0);
    this.notificacionService.contarNoLeidas().subscribe({ next: () => {}, error: () => {} });
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.error.set('');

    this.notificacionService.listar(page).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudieron cargar las notificaciones');
        this.isLoading.set(false);
      }
    });
  }

  marcarLeida(notificacion: RespuestaNotificacion): void {
    if (notificacion.estaLeida) return;

    this.notificacionService.marcarComoLeida(notificacion.idNotificacion).subscribe({
      next: () => {
        this.pagina.update(p => ({
          ...p,
          contenido: p.contenido.map(n =>
            n.idNotificacion === notificacion.idNotificacion ? { ...n, estaLeida: true } : n)
        }));
      },
      error: () => this.toast.error('No se pudo marcar la notificación')
    });
  }

  marcarTodas(): void {
    this.marcandoTodas.set(true);
    this.notificacionService.marcarTodasLeidas().subscribe({
      next: () => {
        this.pagina.update(p => ({
          ...p,
          contenido: p.contenido.map(n => ({ ...n, estaLeida: true }))
        }));
        this.marcandoTodas.set(false);
        this.toast.success('Todas las notificaciones marcadas como leídas');
      },
      error: () => {
        this.marcandoTodas.set(false);
        this.toast.error('No se pudieron marcar las notificaciones');
      }
    });
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  /** Los tipos de evento vienen en SCREAMING_SNAKE desde el backend. */
  etiquetaEvento(tipoEvento: string): string {
    if (!tipoEvento) return 'Notificación';
    return tipoEvento.replace(/_/g, ' ').toLowerCase()
      .replace(/^\w/, c => c.toUpperCase());
  }

  colorEvento(tipoEvento: string): string {
    const tipo = (tipoEvento || '').toUpperCase();
    if (tipo.includes('PAGO')) return 'bg-emerald-50 text-emerald-700';
    if (tipo.includes('RECHAZ') || tipo.includes('CANCEL')) return 'bg-rose-50 text-rose-700';
    if (tipo.includes('CONTRATO') || tipo.includes('FIRMA')) return 'bg-violet-50 text-violet-700';
    if (tipo.includes('MENSAJE') || tipo.includes('CHAT')) return 'bg-sky-50 text-sky-700';
    return 'bg-slate-100 text-slate-600';
  }

  formatFecha(fecha: string): string {
    if (!fecha) return '—';
    const d = new Date(fecha);
    const ahora = new Date();
    const minutos = Math.floor((ahora.getTime() - d.getTime()) / 60000);

    if (minutos < 1) return 'Hace un momento';
    if (minutos < 60) return `Hace ${minutos} min`;
    if (minutos < 1440) return `Hace ${Math.floor(minutos / 60)} h`;
    return d.toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
