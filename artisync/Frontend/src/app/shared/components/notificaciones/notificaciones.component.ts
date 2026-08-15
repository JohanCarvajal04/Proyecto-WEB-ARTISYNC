import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../../../features/comunicacion/services/notificacion.service';
import { Notificacion } from '../../../features/comunicacion/models/comunicacion.model';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificaciones.html',
  styleUrls: []
})
export class NotificacionesComponent implements OnInit, OnDestroy {
  public notificaciones: Notificacion[] = [];
  public noLeidas: number = 0;
  public mostrarDropdown: boolean = false;
  private subs: Subscription = new Subscription();

  constructor(private notificacionService: NotificacionService) {}

  ngOnInit(): void {
    this.cargarNotificaciones();
    // Refrescar cada 30 segundos
    this.subs.add(
      interval(30000).subscribe(() => this.cargarNotificaciones())
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  cargarNotificaciones(): void {
    this.notificacionService.obtenerNotificaciones(0, 10).subscribe({
      next: (res) => {
        this.notificaciones = res.content || [];
        this.noLeidas = this.notificaciones.filter(n => !n.leida).length;
      },
      error: (err) => console.error('Error cargando notificaciones', err)
    });
  }

  toggleDropdown(): void {
    this.mostrarDropdown = !this.mostrarDropdown;
  }

  marcarLeida(n: Notificacion): void {
    if (n.leida) return;
    
    this.notificacionService.marcarComoLeida(n.idNotificacion).subscribe({
      next: () => {
        n.leida = true;
        this.noLeidas = Math.max(0, this.noLeidas - 1);
      }
    });
  }
}
