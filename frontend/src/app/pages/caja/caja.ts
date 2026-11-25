import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MetodoPago, TurnoCaja, Venta } from '../../models/modelos';
import { ApiService, fechaIso, mensajeDeError } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

/** CU11: apertura y cierre del turno de caja del vendedor con los montos reales de sus ventas. */
@Component({
  selector: 'app-caja',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './caja.html',
})
export class Caja implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  /** Ventas de hoy (para listar las del turno abierto o las del último cierre). */
  private readonly ventasHoy = signal<Venta[]>([]);
  protected readonly ventasTurno = computed(() => {
    const turno = this.turno() ?? this.cerrado();
    const nombre = this.auth.usuario()?.nombre;
    if (!turno) {
      return [];
    }
    return this.ventasHoy().filter(
      (v) => v.vendedor === nombre && v.fecha >= turno.fechaApertura && (!turno.fechaCierre || v.fecha <= turno.fechaCierre),
    );
  });
  protected readonly metodo: Record<MetodoPago, string> = { EFECTIVO: 'Efectivo', TARJETA: 'Tarjeta', YAPE: 'Yape / Plin' };

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
    this.cargarVentas();
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

  private cargarVentas(): void {
    const hoy = fechaIso();
    this.api.ventas(hoy, hoy).subscribe({
      next: (v) => this.ventasHoy.set(v),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
