import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortafolioService } from '../../services/portafolio.service';
import { PeticionActualizarPortafolio, Portafolio, OpcionesPersonalizacion } from '../../models/portafolio.model';

@Component({
  selector: 'app-portafolio-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './portafolio-edit.component.html',
  styleUrls: ['./portafolio-edit.component.css']
})
export class PortafolioEditComponent implements OnInit {
  portafolio: Portafolio | null = null;
  opcionesPersonalizacion: OpcionesPersonalizacion = {
    primary: '#0d6efd',
    secondary: '#6c757d',
    bg: '#ffffff',
    text: '#212529',
    surface: '#f8f9fa'
  };
  isLoading = true;
  successMessage = '';
  errorMessage = '';

  constructor(private portafolioService: PortafolioService) {}

  ngOnInit(): void {
    // Supongamos que obtenemos el id del perfil del usuario actual (ej. 1)
    const idPerfil = 1; 
    this.portafolioService.obtenerPorPerfil(idPerfil).subscribe({
      next: (data) => {
        this.portafolio = data;
        if (data.opcionesPersonalizacion) {
          this.opcionesPersonalizacion = { ...data.opcionesPersonalizacion };
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'No se pudo cargar el portafolio.';
        this.isLoading = false;
      }
    });
  }

  saveConfig(): void {
    if (!this.portafolio) return;
    
    this.successMessage = '';
    this.errorMessage = '';
    this.isLoading = true;

    const req: PeticionActualizarPortafolio = {
      opcionesPersonalizacion: this.opcionesPersonalizacion
    };

    this.portafolioService.actualizar(this.portafolio.idPortafolio, req).subscribe({
      next: (updated) => {
        this.portafolio = updated;
        this.successMessage = 'Personalización guardada exitosamente.';
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error al guardar la personalización.';
        this.isLoading = false;
      }
    });
  }
}
