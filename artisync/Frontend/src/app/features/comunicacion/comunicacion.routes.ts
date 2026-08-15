import { Routes } from '@angular/router';
import { BriefingFormComponent } from './pages/briefing-form/briefing-form.component';
import { SalaChatComponent } from './pages/sala-chat/sala-chat.component';

export const COMUNICACION_ROUTES: Routes = [
  {
    path: 'briefing/:idPedido',
    component: BriefingFormComponent,
    title: 'Completar Briefing'
  },
  {
    path: 'chat/:idPedido',
    component: SalaChatComponent,
    title: 'Sala de Chat'
  }
];
