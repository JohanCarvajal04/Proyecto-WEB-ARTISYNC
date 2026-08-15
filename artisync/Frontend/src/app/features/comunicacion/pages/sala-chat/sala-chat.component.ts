import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { Mensaje, SalaChat } from '../../models/comunicacion.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-sala-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sala-chat.html',
  styleUrls: []
})
export class SalaChatComponent implements OnInit, OnDestroy {
  @Input() idPedido!: number;
  
  public mensajes: Mensaje[] = [];
  public mensajeNuevo: string = '';
  public salaActiva: boolean = false;
  public errorMessage: string = '';
  public currentUserId: number = 0; // Se llenaría con el token del usuario actual en un caso real
  
  private subs: Subscription = new Subscription();

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    // Simulamos obtener el ID del usuario del local storage
    const tokenPayload = localStorage.getItem('tokenPayload');
    if (tokenPayload) {
      try {
        this.currentUserId = JSON.parse(tokenPayload).idUsuario;
      } catch(e) {}
    }

    // Verificar si la sala está activa
    this.subs.add(
      this.chatService.obtenerEstadoSala(this.idPedido).subscribe({
        next: (sala: SalaChat) => {
          this.salaActiva = sala.salaActiva;
          if (this.salaActiva) {
            this.chatService.joinSala(sala.idSala, this.idPedido);
          }
        },
        error: (err) => {
          console.error('Error al obtener estado de sala', err);
          this.errorMessage = 'No se pudo cargar la sala de chat.';
        }
      })
    );

    // Escuchar mensajes
    this.subs.add(
      this.chatService.mensajes$.subscribe(mensajes => {
        this.mensajes = mensajes;
        this.scrollToBottom();
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.chatService.disconnect();
  }

  enviarMensaje(): void {
    if (!this.mensajeNuevo.trim() || !this.salaActiva) return;
    
    const texto = this.mensajeNuevo.trim();
    this.mensajeNuevo = '';
    
    // Intenta usar STOMP
    try {
      this.chatService.enviarMensajeWs(this.idPedido, texto);
    } catch (e) {
      // Si falla, usar REST como fallback (REQ-F-14 permite fallback)
      this.chatService.enviarMensaje(this.idPedido, texto).subscribe({
        error: (err) => {
          this.errorMessage = err.error?.message || 'Error enviando mensaje. Revisa no incluir datos de contacto.';
          setTimeout(() => this.errorMessage = '', 5000);
        }
      });
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const container = document.getElementById('chat-container');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }
}
