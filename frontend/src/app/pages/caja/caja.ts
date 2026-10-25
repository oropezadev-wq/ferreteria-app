import { CurrencyPipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** Pantalla de prototipo del cierre de turno (CU11) con montos de ejemplo. */
@Component({
  selector: 'app-caja',
  imports: [CurrencyPipe, FormsModule],
  templateUrl: './caja.html',
})
export class Caja {
  protected readonly abierta = signal(true);
  protected readonly apertura = 200;
  protected readonly ventasEfectivo = 845.5;
  protected readonly ventasTarjeta = 312;
  protected readonly ventasYape = 127;
  protected readonly contado = signal<number | null>(null);
  protected montoApertura = 200;

  protected readonly esperado = computed(() => this.apertura + this.ventasEfectivo);
  protected readonly diferencia = computed(() =>
    this.contado() === null ? null : (this.contado() as number) - this.esperado(),
  );
}
