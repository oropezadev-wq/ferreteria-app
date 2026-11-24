import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ReporteVentas, Venta } from '../../models/modelos';
import { ApiService, fechaIso, mensajeDeError } from '../../services/api.service';

/** RF7: ventas, utilidad y rotación del periodo, y lista de comprobantes con su anulación. */
@Component({
  selector: 'app-reportes',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './reportes.html',
})
export class Reportes implements OnInit {
  private readonly api = inject(ApiService);

  protected desde = fechaIso(new Date(Date.now() - 6 * 24 * 60 * 60 * 1000));
  protected hasta = fechaIso();
  protected readonly reporte = signal<ReporteVentas | null>(null);
  protected readonly ventas = signal<Venta[]>([]);
  protected readonly error = signal('');
  protected readonly mensaje = signal('');
  protected readonly maximo = computed(() =>
    Math.max(1, ...(this.reporte()?.ventasPorDia ?? []).map((d) => d.total)),
  );

  ngOnInit(): void {
    this.consultar();
  }

  consultar(): void {
    this.api.reporteVentas(this.desde, this.hasta).subscribe({
      next: (r) => {
        this.error.set('');
        this.reporte.set(r);
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
    this.api.ventas(this.desde, this.hasta).subscribe({
      next: (v) => this.ventas.set(v),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  /** La anulación devuelve el stock y deja de sumar en el reporte y en la caja. */
  anular(venta: Venta): void {
    const motivo = prompt(`Motivo de la anulación de ${venta.comprobante}:`);
    if (!motivo?.trim()) {
      return;
    }
    this.api.anularVenta(venta.id, motivo.trim()).subscribe({
      next: (v) => {
        this.mensaje.set(`Se anuló ${v.comprobante} y sus productos volvieron al stock.`);
        this.consultar();
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
