import { Component, inject, signal, OnInit } from '@angular/core';
import { ModeracionService } from '../../services/moderacion.service';
import { Portafolio } from '../../models/moderacion.model';

@Component({
  selector: 'app-mod-portafolios',
  standalone: true,
  templateUrl: './mod-portafolios.component.html'
})
export class ModPortafoliosComponent implements OnInit {
  private modService = inject(ModeracionService);

  readonly portafolios = signal<Portafolio[]>([]);
  readonly isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.loadPortafolios();
  }

  loadPortafolios(): void {
    this.isLoading.set(true);
    this.modService.listarPortafolios().subscribe({
      next: (data) => {
        this.portafolios.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }
}
