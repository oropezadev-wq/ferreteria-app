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

  /** Venta que se está por anular (abre el diálogo) y el motivo que se escribe en él. */
  protected readonly porAnular = signal<Venta | null>(null);
  protected motivo = '';
  protected readonly anulando = signal(false);

  /** Periodos rápidos: llenan las fechas y consultan. */
  periodo(dias: number | 'mes'): void {
    const hoy = new Date();
    const desde = dias === 'mes' ? new Date(hoy.getFullYear(), hoy.getMonth(), 1) : new Date(Date.now() - dias * 24 * 60 * 60 * 1000);
    this.desde = fechaIso(desde);
    this.hasta = fechaIso(hoy);
    this.consultar();
  }

  anular(venta: Venta): void {
    this.motivo = '';
    this.porAnular.set(venta);
    // El diálogo aparece después de este ciclo; ahí se pone el cursor en el motivo.
    setTimeout(() => document.getElementById('motivo')?.focus());
  }

  cancelarAnulacion(): void {
    this.porAnular.set(null);
  }

  /** La anulación devuelve el stock y deja de sumar en el reporte y en la caja. */
  confirmarAnulacion(): void {
    const venta = this.porAnular();
    const motivo = this.motivo.trim();
    if (!venta || !motivo) {
      return;
    }
    this.anulando.set(true);
    this.api.anularVenta(venta.id, motivo).subscribe({
      next: (v) => {
        this.anulando.set(false);
        this.porAnular.set(null);
        this.mensaje.set(`Se anuló ${v.comprobante} y sus productos volvieron al stock.`);
        this.consultar();
      },
      error: (e) => {
        this.anulando.set(false);
        this.porAnular.set(null);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
