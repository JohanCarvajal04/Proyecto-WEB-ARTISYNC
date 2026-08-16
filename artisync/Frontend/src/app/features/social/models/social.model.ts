// ─── Modelos del Módulo Social (M7) ─────────────────────────────
// Los DTO de reseña, sorteo, participante y ganador ya estaban modelados para
// el panel del creador; el backend devuelve las mismas clases en los endpoints
// que consume el cliente, así que se reutilizan.
export type {
  RespuestaResena,
  RespuestaSorteo,
  RespuestaParticipante,
  RespuestaGanador
} from '../../creador/models/creador.model';

/** POST /api/v1/pedidos/{idPedido}/resena */
export interface PeticionCrearResena {
  calificacionEstrellas: number;
  textoResena: string | null;
}

/** Estados que publica el backend en `estadoSorteo`. */
export const SORTEO_ACTIVO = 'Activo';
export const SORTEO_FINALIZADO = 'Finalizado';
