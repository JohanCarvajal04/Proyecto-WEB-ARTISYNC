import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

import { ChatService } from './chat.service';
import { RespuestaMensajeChat } from '../models/comunicacion.model';

const mensajePedidoA: RespuestaMensajeChat = {
  idMensaje: 1,
  idSala: 100,
  idRemitente: 5,
  nombreRemitente: 'Cliente A',
  cuerpoMensaje: 'Hola, pedido A',
  fechaHoraEnvio: '2026-08-23T10:00:00',
  leido: true
};

describe('ChatService', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // AuthService (dependencia de ChatService) dispara un POST a
    // /api/v1/auth/refresh en un microtask durante su constructor (intento de
    // restaurar sesion al arrancar) -- ver auth.service.ts. Es una peticion de
    // fondo ajena a lo que estos tests verifican; se drena antes de exigir que
    // no queden solicitudes abiertas.
    httpMock.match('/api/v1/auth/refresh')
      .forEach(req => req.flush(null, { status: 401, statusText: 'Unauthorized' }));
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // NG-01 (auditoria Angular): ChatService es providedIn:'root' y su
  // BehaviorSubject sobrevivia entre pedidos distintos -- el siguiente
  // suscriptor recibia de inmediato la conversacion ajena que quedo cargada.
  it('NG-01: disconnect() vacia el historial de mensajes cargado', () => {
    // Carga el historial del pedido A (joinSala dispara el GET del historial;
    // el resto de la conexion WS no es necesario para esta aserción).
    service.joinSala(100, 1);
    httpMock.expectOne('/api/v1/pedidos/1/chat/mensajes?size=100')
      .flush({ content: [mensajePedidoA] });

    let ultimo: RespuestaMensajeChat[] = [];
    service.mensajes$.subscribe(m => (ultimo = m));
    expect(ultimo).toEqual([mensajePedidoA]);

    service.disconnect();

    expect(ultimo).toEqual([]);
  });

  it('NG-01: joinSala() vacia el historial anterior antes de pedir el nuevo', () => {
    service.joinSala(100, 1);
    httpMock.expectOne('/api/v1/pedidos/1/chat/mensajes?size=100')
      .flush({ content: [mensajePedidoA] });

    const emisiones: RespuestaMensajeChat[][] = [];
    service.mensajes$.subscribe(m => emisiones.push(m));
    expect(emisiones[0]).toEqual([mensajePedidoA]);

    // Entrar a la sala del pedido B SIN pasar por disconnect(): el caso del
    // componente que no llega a destruirse antes de cambiar de pedido.
    service.joinSala(200, 2);

    // Primera emision tras joinSala() debe ser el vaciado, no arrastrar A.
    expect(emisiones[1]).toEqual([]);

    httpMock.expectOne('/api/v1/pedidos/2/chat/mensajes?size=100')
      .flush({ content: [] });
  });

  it('NG-01: un fallo al cargar el historial no deja el anterior en pantalla', () => {
    service.joinSala(100, 1);
    httpMock.expectOne('/api/v1/pedidos/1/chat/mensajes?size=100')
      .flush({ content: [mensajePedidoA] });

    let ultimo: RespuestaMensajeChat[] = [];
    service.mensajes$.subscribe(m => (ultimo = m));
    expect(ultimo).toEqual([mensajePedidoA]);

    service.joinSala(200, 2);
    httpMock.expectOne('/api/v1/pedidos/2/chat/mensajes?size=100')
      .flush('error de red', { status: 500, statusText: 'Server Error' });

    expect(ultimo).toEqual([]);
  });
});
