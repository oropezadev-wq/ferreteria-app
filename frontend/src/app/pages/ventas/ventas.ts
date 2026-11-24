import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MetodoPago, Producto, TipoComprobante, Venta } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

interface LineaVenta {
  producto: Producto;
  cantidad: number;
}

const TASA_IGV = 0.18;
// SUNAT exige identificar al cliente en boletas que superan este monto.
const TOPE_BOLETA_SIN_DNI = 700;

@Component({
  selector: 'app-ventas',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './ventas.html',
})
export class Ventas {
  private readonly api = inject(ApiService);

  protected texto = '';
  protected metodoPago: MetodoPago = 'EFECTIVO';
  protected tipoComprobante: TipoComprobante = 'BOLETA';
  protected documento = '';
  protected razonSocial = '';
  protected readonly resultados = signal<Producto[]>([]);
  protected readonly lineas = signal<LineaVenta[]>([]);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');
  protected readonly enviando = signal(false);
  protected readonly ultimaVenta = signal<Venta | null>(null);

  // Los precios ya incluyen IGV: se separa la base imponible del impuesto.
  protected readonly total = computed(() =>
    this.lineas().reduce((suma, l) => suma + l.producto.precioVenta * l.cantidad, 0),
  );
  protected readonly gravada = computed(() => this.total() / (1 + TASA_IGV));
  protected readonly igv = computed(() => this.total() - this.gravada());

  /** RF3: búsqueda real contra la API (el lector de código de barras escribe y envía Enter). */
  buscar(): void {
    this.mensaje.set('');
    this.api.buscarProductos(this.texto, true).subscribe({
      next: (lista) => {
        this.error.set('');
        this.resultados.set(lista);
        if (lista.length === 1) {
          this.agregar(lista[0]);
        }
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  agregar(producto: Producto): void {
    const actual = this.lineas().find((l) => l.producto.id === producto.id);
    const cantidad = (actual?.cantidad ?? 0) + 1;
    if (cantidad > producto.stock) {
      this.error.set(`Solo hay ${producto.stock} unidades de ${producto.descripcion}.`);
      return;
    }
    this.error.set('');
    if (actual) {
      this.lineas.update((ls) => ls.map((l) => (l === actual ? { ...l, cantidad } : l)));
    } else {
      this.lineas.update((ls) => [...ls, { producto, cantidad: 1 }]);
    }
    this.texto = '';
    this.resultados.set([]);
  }

  cambiarCantidad(linea: LineaVenta, cantidad: number): void {
    const valida = Math.max(1, Math.min(cantidad || 1, linea.producto.stock));
    this.lineas.update((ls) => ls.map((l) => (l === linea ? { ...l, cantidad: valida } : l)));
  }

  quitar(linea: LineaVenta): void {
    this.lineas.update((ls) => ls.filter((l) => l !== linea));
  }

  /**
   * Reglas del comprobante: factura con RUC y razón social; boleta con DNI si supera el tope.
   * La API vuelve a validarlas; aquí se revisan antes para avisar sin esperar al servidor.
   */
  private validarComprobante(): string {
    const doc = this.documento.trim();
    if (this.tipoComprobante === 'FACTURA') {
      if (!/^(10|20)\d{9}$/.test(doc)) {
        return 'La factura necesita un RUC válido de 11 dígitos (empieza con 10 o 20).';
      }
      if (!this.razonSocial.trim()) {
        return 'La factura necesita la razón social del cliente.';
      }
      return '';
    }
    if (doc && !/^\d{8}$/.test(doc)) {
      return 'El DNI debe tener 8 dígitos.';
    }
    if (!doc && this.total() > TOPE_BOLETA_SIN_DNI) {
      return `Para una boleta de más de S/ ${TOPE_BOLETA_SIN_DNI} se debe ingresar el DNI del cliente.`;
    }
    return '';
  }

  cobrar(): void {
    const problema = this.validarComprobante();
    if (problema) {
      this.mensaje.set('');
      this.error.set(problema);
      return;
    }
    this.error.set('');
    this.enviando.set(true);
    const documento = this.documento.trim();
    this.api
      .registrarVenta({
        tipoComprobante: this.tipoComprobante,
        metodoPago: this.metodoPago,
        clienteDocumento: documento || null,
        clienteNombre: this.razonSocial.trim() || null,
        lineas: this.lineas().map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
      })
      .subscribe({
        next: (venta) => {
          this.enviando.set(false);
          this.ultimaVenta.set(venta);
          this.mensaje.set(`Venta registrada: ${venta.comprobante} por S/ ${venta.total.toFixed(2)}.`);
          this.lineas.set([]);
          this.documento = '';
          this.razonSocial = '';
        },
        error: (e) => {
          this.enviando.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }

  nuevaVenta(): void {
    this.ultimaVenta.set(null);
    this.mensaje.set('');
  }

  imprimir(): void {
    window.print();
  }
}
