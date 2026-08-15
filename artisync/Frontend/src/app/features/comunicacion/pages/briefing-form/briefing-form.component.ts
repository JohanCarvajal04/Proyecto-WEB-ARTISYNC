import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BriefingService } from '../../services/briefing.service';
import { BriefingEnviado, PeticionResponderBriefing, RespuestaBriefing } from '../../models/comunicacion.model';

@Component({
  selector: 'app-briefing-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './briefing-form.html',
  styleUrls: []
})
export class BriefingFormComponent implements OnInit {
  public idPedido: number = 0;
  public briefing: BriefingEnviado | null = null;
  public respuestas: { [key: number]: string } = {};
  public cargando: boolean = true;
  public guardando: boolean = false;
  public errorMensaje: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private briefingService: BriefingService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.idPedido = Number(params.get('idPedido'));
      if (this.idPedido) {
        this.cargarBriefing();
      }
    });
  }

  cargarBriefing(): void {
    this.cargando = true;
    this.briefingService.obtenerBriefing(this.idPedido).subscribe({
      next: (res) => {
        this.briefing = res;
        this.cargando = false;
        
        // Inicializar respuestas vacías
        if (this.briefing?.plantilla?.preguntas) {
          this.briefing.plantilla.preguntas.forEach(p => {
            this.respuestas[p.idPregunta] = '';
          });
        }
      },
      error: (err) => {
        this.errorMensaje = 'No se pudo cargar el formulario de briefing.';
        this.cargando = false;
      }
    });
  }

  enviarBriefing(): void {
    if (!this.briefing || this.briefing.completado) return;
    
    // Validar que todas las preguntas estén respondidas
    const faltan = this.briefing.plantilla.preguntas.some(p => !this.respuestas[p.idPregunta] || !this.respuestas[p.idPregunta].trim());
    
    if (faltan) {
      this.errorMensaje = 'Por favor, responde todas las preguntas del formulario.';
      return;
    }

    this.guardando = true;
    this.errorMensaje = '';

    const payload: PeticionResponderBriefing = {
      respuestas: Object.keys(this.respuestas).map(idPregunta => ({
        idPregunta: Number(idPregunta),
        textoRespuesta: this.respuestas[Number(idPregunta)]
      }))
    };

    this.briefingService.responderBriefing(this.idPedido, payload).subscribe({
      next: () => {
        this.guardando = false;
        if (this.briefing) {
          this.briefing.completado = true;
        }
        // Navegar de regreso al detalle del pedido
        this.router.navigate(['/pedidos', this.idPedido]);
      },
      error: (err) => {
        this.errorMensaje = 'Ocurrió un error al enviar el briefing. Inténtalo de nuevo.';
        this.guardando = false;
      }
    });
  }
}
