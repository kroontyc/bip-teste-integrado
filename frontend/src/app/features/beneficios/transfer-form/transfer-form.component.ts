import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { BeneficioService } from '../../../core/services/beneficio.service';
import { Beneficio } from '../../../core/models/beneficio.model';

@Component({
  selector: 'app-transfer-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatCardModule, MatSnackBarModule, MatIconModule
  ],
  template: `
    <mat-card style="max-width:500px; margin:0 auto">
      <mat-card-header>
        <mat-card-title>
          <mat-icon style="vertical-align:middle; margin-right:8px">swap_horiz</mat-icon>
          Transferência entre Benefícios
        </mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" style="display:flex; flex-direction:column; gap:16px; padding-top:16px">
          <mat-form-field appearance="outline">
            <mat-label>De (Origem) *</mat-label>
            <mat-select formControlName="fromId">
              <mat-option *ngFor="let b of beneficios" [value]="b.id">
                #{{ b.id }} {{ b.nome }} — R$ {{ b.valor | number:'1.2-2':'pt-BR' }}
              </mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Para (Destino) *</mat-label>
            <mat-select formControlName="toId">
              <mat-option *ngFor="let b of beneficios" [value]="b.id">
                #{{ b.id }} {{ b.nome }} — R$ {{ b.valor | number:'1.2-2':'pt-BR' }}
              </mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Valor *</mat-label>
            <input matInput type="number" formControlName="amount" min="0.01" step="0.01">
          </mat-form-field>
          <div *ngIf="errorMsg" style="color:red; font-size:14px">{{ errorMsg }}</div>
        </form>
      </mat-card-content>
      <mat-card-actions align="end">
        <button mat-raised-button color="primary" (click)="transfer()" [disabled]="form.invalid || transferring">
          <mat-icon>send</mat-icon> {{ transferring ? 'Transferindo...' : 'Transferir' }}
        </button>
      </mat-card-actions>
    </mat-card>
  `
})
export class TransferFormComponent implements OnInit {

  form!: FormGroup;
  beneficios: Beneficio[] = [];
  transferring = false;
  errorMsg = '';

  constructor(private fb: FormBuilder, private service: BeneficioService, private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fromId: [null, Validators.required],
      toId:   [null, Validators.required],
      amount: [null, [Validators.required, Validators.min(0.01)]]
    });
    this.service.getAll().subscribe({ next: data => this.beneficios = data });
  }

  transfer(): void {
    if (this.form.invalid) return;
    this.errorMsg = '';
    const { fromId, toId } = this.form.value;
    if (fromId === toId) { this.errorMsg = 'Origem e destino não podem ser iguais.'; return; }

    this.transferring = true;
    this.service.transfer(this.form.value).subscribe({
      next: () => {
        this.transferring = false;
        this.snackBar.open('Transferência realizada!', 'Fechar', { duration: 4000 });
        this.form.reset();
        this.service.getAll().subscribe(data => this.beneficios = data);
      },
      error: (err) => {
        this.transferring = false;
        this.errorMsg = err?.error?.message ?? 'Erro na transferência.';
      }
    });
  }
}
