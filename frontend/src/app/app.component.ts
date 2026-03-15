import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>account_balance_wallet</mat-icon>
      <span style="margin-left:8px">Benefícios</span>
      <span style="flex:1"></span>
      <a mat-button routerLink="/" routerLinkActive="active-link" [routerLinkActiveOptions]="{exact:true}">
        <mat-icon>list</mat-icon> Lista
      </a>
      <a mat-button routerLink="/transfer" routerLinkActive="active-link">
        <mat-icon>swap_horiz</mat-icon> Transferir
      </a>
    </mat-toolbar>
    <div class="page-container">
      <router-outlet />
    </div>
  `,
  styles: [`.active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }`]
})
export class AppComponent {}
