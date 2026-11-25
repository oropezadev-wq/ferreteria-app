import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MetodoPago, Movimiento, Producto, ReporteVentas, Resumen, TurnoCaja, Venta } from '../../models/modelos';
import { ApiService, fechaIso, mensajeDeError } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { estiloCategoria, iconoProducto } from '../../services/categorias';
import { coberturaMinimo, nivelStock, NivelStock } from '../../services/niveles';

const DIA = 24 * 60 * 60 * 1000;
const NOMBRE_DIA = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
export const METODOS: { clave: MetodoPago; texto: string; icono: string }[] = [
  { clave: 'EFECTIVO', texto: 'Efectivo', icono: 'bi-cash' },
  { clave: 'TARJETA', texto: 'Tarjeta', icono: 'bi-credit-card' },
  { clave: 'YAPE', texto: 'Yape / Plin', icono: 'bi-phone' },
];

/**
 * Panel por rol.
 * - Vendedor: mostrador (vender ya, su caja, lo que cobró y sus últimas ventas).
 *   No consulta el resumen del inventario: no ve valor del inventario ni costos.
 * - Administrador: qué reponer, ventas de la semana, rotación, cobrado del día y kardex.
 */
@Component({
  selector: 'app-panel',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './panel.html',
})
export class Panel implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly estilo = estiloCategoria;
  protected readonly icono = iconoProducto;
  protected readonly metodos = METODOS;
  protected texto = '';

  protected readonly error = signal('');
  protected readonly cargando = signal(true);
  protected readonly alertas = signal<Producto[]>([]);
  protected readonly ventasHoy = signal<Venta[]>([]);
  // Vendedor
  protected readonly caja = signal<TurnoCaja | null>(null);
  protected readonly productos = signal<Producto[]>([]);
  // Administrador
  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly semana = signal<ReporteVentas | null>(null);
  protected readonly totalSemanaAnterior = signal<number | null>(null);
  protected readonly movimientos = signal<Movimiento[]>([]);

  /** Ventas registradas (no anuladas) de hoy; para el vendedor, solo las suyas. */
  protected readonly misVentas = computed(() => {
    const nombre = this.auth.usuario()?.nombre;
    return this.ventasHoy().filter(
      (v) => v.estado === 'REGISTRADA' && (this.auth.esAdministrador() || v.vendedor === nombre),
    );
  });
  protected readonly cobrado = computed(() => this.misVentas().reduce((s, v) => s + v.total, 0));
  protected readonly porMetodo = computed(() =>
    METODOS.map((m) => {
      const total = this.misVentas().filter((v) => v.metodoPago === m.clave).reduce((s, v) => s + v.total, 0);
      return { ...m, total, parte: this.cobrado() ? (total / this.cobrado()) * 100 : 0 };
    }),
  );

  /** Reposición agrupada por nivel real con la API actual: proporción stock / mínimo. */
  protected readonly reponer = computed(() => {
    const grupos: { nivel: NivelStock; titulo: string; nota: string; productos: Producto[] }[] = [
      { nivel: 'critico', titulo: 'Urgente', nota: 'sin stock o con la mitad del mínimo o menos', productos: [] },
      { nivel: 'bajo', titulo: 'Bajo el mínimo', nota: 'todavía hay, pero no alcanza el mínimo', productos: [] },
    ];
    for (const p of [...this.alertas()].sort((a, b) => a.stock / (a.stockMinimo || 1) - b.stock / (b.stockMinimo || 1))) {
      grupos[nivelStock(p) === 'critico' ? 0 : 1].productos.push(p);
    }
    return grupos.filter((g) => g.productos.length > 0);
  });
  protected readonly nivel = nivelStock;
  protected readonly cobertura = coberturaMinimo;

  /** Siete días con su total, incluidos los días sin ventas. */
  protected readonly dias = computed(() => {
    const porFecha = new Map((this.semana()?.ventasPorDia ?? []).map((d) => [d.fecha, d.total]));
    const lista = Array.from({ length: 7 }, (_, i) => {
      const fecha = new Date(Date.now() - (6 - i) * DIA);
      const iso = fechaIso(fecha);
      return { iso, nombre: NOMBRE_DIA[fecha.getDay()], dia: fecha.getDate(), total: porFecha.get(iso) ?? 0, hoy: i === 6 };
    });
    const max = Math.max(1, ...lista.map((d) => d.total));
    return lista.map((d) => ({ ...d, alto: (d.total / max) * 100 }));
  });
  protected readonly mejorDia = computed(() => {
    const d = [...this.dias()].sort((a, b) => b.total - a.total)[0];
    return d && d.total > 0 ? d : null;
  });
  protected readonly variacion = computed(() => {
    const antes = this.totalSemanaAnterior();
    const ahora = this.semana()?.total ?? 0;
    return antes ? Math.round(((ahora - antes) / antes) * 100) : null;
  });
  protected readonly kardexHoy = computed(() => {
    const hoy = fechaIso();
    return this.movimientos().filter((m) => m.fecha.startsWith(hoy)).slice(0, 8);
  });

  ngOnInit(): void {
    const alFallar = (e: HttpErrorResponse) => this.error.set(mensajeDeError(e));
    const hoy = fechaIso();
    this.api.alertasStock().subscribe({ next: (a) => this.alertas.set(a), error: alFallar });
    this.api.ventas(hoy, hoy).subscribe({
      next: (v) => {
        this.ventasHoy.set(v);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        alFallar(e);
      },
    });
    if (this.auth.esAdministrador()) {
      this.api.resumen().subscribe({ next: (r) => this.resumen.set(r), error: alFallar });
      this.api.reporteVentas(fechaIso(new Date(Date.now() - 6 * DIA)), hoy).subscribe({
        next: (r) => this.semana.set(r),
        error: alFallar,
      });
      this.api
        .reporteVentas(fechaIso(new Date(Date.now() - 13 * DIA)), fechaIso(new Date(Date.now() - 7 * DIA)))
        .subscribe({ next: (r) => this.totalSemanaAnterior.set(r.total), error: alFallar });
      this.api.movimientos().subscribe({ next: (m) => this.movimientos.set(m), error: alFallar });
    } else {
      this.api.cajaActual().subscribe({ next: (t) => this.caja.set(t), error: alFallar });
      this.api.buscarProductos('', true).subscribe({
        next: (p) =>
          this.productos.set(
            [...p].sort((a, b) => a.categoriaNombre.localeCompare(b.categoriaNombre, 'es') || a.descripcion.localeCompare(b.descripcion, 'es')),
          ),
        error: alFallar,
      });
    }
  }

  /** Lleva a Ventas con el texto o código: si hay un solo resultado, se agrega al carrito. */
  vender(texto: string): void {
    this.router.navigate(['/ventas'], { queryParams: texto.trim() ? { buscar: texto.trim() } : {} });
  }

  nombreMetodo(m: MetodoPago): string {
    return METODOS.find((x) => x.clave === m)?.texto ?? m;
  }

  iconoMetodo(m: MetodoPago): string {
    return METODOS.find((x) => x.clave === m)?.icono ?? 'bi-cash';
  }

  resumenVenta(v: Venta): string {
    return v.detalles.map((d) => `${d.cantidad} × ${d.descripcion}`).join(', ');
  }
}
