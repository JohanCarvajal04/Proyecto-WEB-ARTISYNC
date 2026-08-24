import { Component, OnInit } from '@angular/core';
import { PortafolioService } from '../../services/portafolio.service';
import { Portafolio, OpcionesPersonalizacion } from '../../models/portafolio.model';

@Component({
  selector: 'app-portafolio-view',
  standalone: true,
  imports: [],
  templateUrl: './portafolio-view.component.html',
  styleUrls: ['./portafolio-view.component.css']
})
export class PortafolioViewComponent implements OnInit {
  portafolio: Portafolio | null = null;
  isLoading = true;

  // Variables por defecto de Bootstrap (o theme) si no hay personalización
  defaultTheme = {
    primary: '#0d6efd',
    secondary: '#6c757d',
    bg: '#ffffff',
    text: '#212529',
    surface: '#f8f9fa'
  };

  constructor(private portafolioService: PortafolioService) {}

  ngOnInit(): void {
    // Supongamos que recibimos el idPerfil por ruta, ej. 1
    const idPerfil = 1; 
    this.portafolioService.obtenerPorPerfil(idPerfil).subscribe({
      next: (data) => {
        this.portafolio = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  get themeVariables() {
    const opts = this.portafolio?.opcionesPersonalizacion || this.defaultTheme;
    return {
      '--artisync-primary': opts.primary,
      '--artisync-secondary': opts.secondary,
      '--artisync-bg': opts.bg,
      '--artisync-text': opts.text,
      '--artisync-surface': opts.surface
    };
  }
}
