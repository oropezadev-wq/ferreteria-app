import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { TurnoCaja } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

/** CU11: apertura y cierre del turno de caja del vendedor con los montos reales de sus ventas. */
@Component({
  selector: 'app-caja',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './caja.html',
})
export class Caja implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly turno = signal<TurnoCaja | null>(null);
  protected readonly cerrado = signal<TurnoCaja | null>(null);
  protected readonly contado = signal<number | null>(null);
  protected readonly error = signal('');
  protected montoApertura = 200;
  protected observacion = '';

  /** Vista previa de la diferencia; el cálculo que vale es el de la API al confirmar. */
  protected readonly diferencia = computed(() => {
    const turno = this.turno();
    const contado = this.contado();
    return turno && contado !== null ? contado - turno.montoEsperado : null;
  });

  ngOnInit(): void {
    this.api.cajaActual().subscribe({
      next: (t) => this.turno.set(t),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  abrir(): void {
    this.api.abrirCaja(this.montoApertura).subscribe({
      next: (t) => {
        this.error.set('');
        this.cerrado.set(null);
        this.turno.set(t);
        this.contado.set(null);
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  cerrar(): void {
    const contado = this.contado();
    if (contado === null) {
      return;
    }
    this.api.cerrarCaja(contado, this.observacion).subscribe({
      next: (t) => {
        this.error.set('');
        this.turno.set(null);
        this.cerrado.set(t);
        this.observacion = '';
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
