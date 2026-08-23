// sockjs-client (usado por ChatService para el chat del pedido) da por hecho
// el `global` de Node al cargarse; el navegador no lo tiene y esto rompía en
// blanco cualquier ruta que arrastrara ese chunk (p. ej. pedido-detalle, que
// embebe el chat). Debe fijarse antes de cualquier otro import.
(window as unknown as { global: unknown }).global = window;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
