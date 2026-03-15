import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BeneficioService } from '../../../core/services/beneficio.service';
import { Beneficio } from '../../../core/models/beneficio.model';
import { BeneficioFormComponent } from '../beneficio-form/beneficio-form.component';

@Component({
  selector: 'app-beneficio-list',
  standalone: true,
  imports: [
    CommonModule, CurrencyPipe,
    MatTableModule, MatButtonModule, MatIconModule,
    MatCardModule, MatChipsModule, MatDialogModule, MatSnackBarModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Benefícios</mat-card-title>
        <mat-card-subtitle>Gerenciamento completo de benefícios</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div style="margin-bottom:16px; text-align:right">
          <button mat-raised-button color="primary" (click)="openCreate()">
            <mat-icon>add</mat-icon> Novo Benefício
          </button>
        </div>

        <table mat-table [dataSource]="beneficios" style="width:100%">

          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>ID</th>
            <td mat-cell *matCellDef="let b">{{ b.id }}</td>
          </ng-container>

          <ng-container matColumnDef="nome">
            <th mat-header-cell *matHeaderCellDef>Nome</th>
            <td mat-cell *matCellDef="let b">{{ b.nome }}</td>
          </ng-container>

          <ng-container matColumnDef="descricao">
            <th mat-header-cell *matHeaderCellDef>Descrição</th>
            <td mat-cell *matCellDef="let b">{{ b.descricao || '—' }}</td>
          </ng-container>

          <ng-container matColumnDef="valor">
            <th mat-header-cell *matHeaderCellDef>Valor</th>
            <td mat-cell *matCellDef="let b">{{ b.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
          </ng-container>

          <ng-container matColumnDef="ativo">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let b">
              <mat-chip [color]="b.ativo ? 'primary' : 'warn'" highlighted>
                {{ b.ativo ? 'Ativo' : 'Inativo' }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="acoes">
            <th mat-header-cell *matHeaderCellDef>Ações</th>
            <td mat-cell *matCellDef="let b">
              <button mat-icon-button color="primary" (click)="openEdit(b)" title="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="delete(b)" title="Excluir">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="6" style="text-align:center; padding:16px">
              Nenhum benefício encontrado.
            </td>
          </tr>
        </table>
      </mat-card-content>
    </mat-card>
  `
})
export class BeneficioListComponent implements OnInit {

  beneficios: Beneficio[] = [];
  displayedColumns = ['id', 'nome', 'descricao', 'valor', 'ativo', 'acoes'];

  constructor(
    private service: BeneficioService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.service.getAll().subscribe({
      next: data => this.beneficios = data,
      error: () => this.snackBar.open('Erro ao carregar benefícios', 'Fechar', { duration: 3000 })
    });
  }

  openCreate(): void {
    const ref = this.dialog.open(BeneficioFormComponent, { width: '480px', data: null });
    ref.afterClosed().subscribe(saved => { if (saved) this.load(); });
  }

  openEdit(b: Beneficio): void {
    const ref = this.dialog.open(BeneficioFormComponent, { width: '480px', data: b });
    ref.afterClosed().subscribe(saved => { if (saved) this.load(); });
  }

  delete(b: Beneficio): void {
    if (!confirm(`Excluir "${b.nome}"?`)) return;
    this.service.delete(b.id).subscribe({
      next: () => { this.snackBar.open('Excluído', 'Fechar', { duration: 3000 }); this.load(); },
      error: () => this.snackBar.open('Erro ao excluir', 'Fechar', { duration: 3000 })
    });
  }
}
