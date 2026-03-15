import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BeneficioService } from '../../../core/services/beneficio.service';
import { Beneficio } from '../../../core/models/beneficio.model';

@Component({
  selector: 'app-beneficio-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
    MatButtonModule, MatDialogModule, MatSnackBarModule
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Editar' : 'Novo' }} Beneficio</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex; flex-direction:column; gap:12px; padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Nome *</mat-label>
          <input matInput formControlName="nome" maxlength="100">
          <mat-error *ngIf="form.get('nome')?.hasError('required')">Nome e obrigatorio</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Descricao</mat-label>
          <textarea matInput formControlName="descricao" rows="3" maxlength="255"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Valor *</mat-label>
          <input matInput type="number" formControlName="valor" min="0" step="0.01">
          <mat-error *ngIf="form.get('valor')?.hasError('required')">Valor e obrigatorio</mat-error>
          <mat-error *ngIf="form.get('valor')?.hasError('min')">Valor nao pode ser negativo</mat-error>
        </mat-form-field>
        <mat-checkbox formControlName="ativo">Ativo</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="form.invalid || saving">
        {{ saving ? 'Salvando...' : 'Salvar' }}
      </button>
    </mat-dialog-actions>
  `
})
export class BeneficioFormComponent implements OnInit {

  form!: FormGroup;
  saving = false;

  get isEdit(): boolean { return !!this.data; }

  constructor(
    private fb: FormBuilder,
    private service: BeneficioService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<BeneficioFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Beneficio | null
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nome:      [this.data?.nome ?? '', [Validators.required, Validators.maxLength(100)]],
      descricao: [this.data?.descricao ?? '', Validators.maxLength(255)],
      valor:     [this.data?.valor ?? null, [Validators.required, Validators.min(0)]],
      ativo:     [this.data?.ativo ?? true]
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const request$ = this.isEdit
      ? this.service.update(this.data!.id, this.form.value)
      : this.service.create(this.form.value);

    request$.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Atualizado!' : 'Criado!', 'Fechar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.saving = false;
        this.snackBar.open(err?.error?.message ?? 'Erro ao salvar', 'Fechar', { duration: 5000 });
      }
    });
  }
}
