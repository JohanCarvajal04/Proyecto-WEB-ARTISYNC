export interface OpcionesPersonalizacion {
  primary: string;
  secondary: string;
  bg: string;
  text: string;
  surface: string;
}

export interface Portafolio {
  idPortafolio: number;
  idPerfil: number;
  fechaCreacion: string;
  totalVisitasAcumuladas: number;
  esPublico: boolean;
  opcionesPersonalizacion: OpcionesPersonalizacion;
}

export interface PeticionActualizarPortafolio {
  esPublico?: boolean;
  opcionesPersonalizacion?: OpcionesPersonalizacion;
}

export interface PeticionCrearPortafolio {
  idPerfil: number;
  esPublico?: boolean;
  opcionesPersonalizacion?: OpcionesPersonalizacion;
}
