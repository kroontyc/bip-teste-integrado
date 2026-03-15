import { Routes } from '@angular/router';
import { BeneficioListComponent } from './features/beneficios/beneficio-list/beneficio-list.component';
import { TransferFormComponent } from './features/beneficios/transfer-form/transfer-form.component';

export const routes: Routes = [
  { path: '', component: BeneficioListComponent },
  { path: 'transfer', component: TransferFormComponent },
  { path: '**', redirectTo: '' }
];
