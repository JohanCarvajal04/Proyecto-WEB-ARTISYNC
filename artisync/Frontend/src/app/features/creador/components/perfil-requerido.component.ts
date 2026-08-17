import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Estado vacío que se muestra en cualquier vista del panel cuando el usuario
 * tiene rol CREADOR pero todavía no ha dado de alta su perfil: sin `idPerfil`
 * no existen servicios, reseñas, sorteos ni portafolio que consultar.
 */
@Component({
  selector: 'app-perfil-requerido',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="cr-card">
      <div class="cr-empty">
        <div class="cr-empty__icon">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </div>
        <div>
          <p class="cr-empty__title">Aún no tienes un perfil de creador</p>
          <p class="cr-empty__text">
            Tu perfil es la base del panel: sin él no se pueden publicar servicios,
            recibir reseñas ni abrir tu portafolio al público.
          </p>
        </div>
        <a routerLink="/creador/perfil" class="cr-btn cr-btn--primary">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          Crear mi perfil
        </a>
      </div>
    </div>
  `
})
export class PerfilRequeridoComponent {}
