import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Producto } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

interface LineaVenta {
  producto: Producto;
  cantidad: number;
}

const TASA_IGV = 0.18;
// SUNAT exige identificar al cliente en boletas que superan este monto.
const TOPE_BOLETA_SIN_DNI = 700;

type TipoComprobante = 'BOLETA' | 'FACTURA';

@Component({
  selector: 'app-ventas',
  imports: [CurrencyPipe, FormsModule],
  templateUrl: './ventas.html',
})
export class Ventas {
  private readonly api = inject(ApiService);

  protected texto = '';
  protected metodoPago = 'EFECTIVO';
  protected tipoComprobante: TipoComprobante = 'BOLETA';
  protected documento = '';
  protected razonSocial = '';
  protected readonly resultados = signal<Producto[]>([]);
  protected readonly lineas = signal<LineaVenta[]>([]);
  protected readonly mensaje = signal('');
  protected readonly error = signal('');

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

  /** Reglas del comprobante: factura con RUC y razón social; boleta con DNI si supera el tope. */
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
    const nombre = this.tipoComprobante === 'FACTURA' ? 'la factura' : 'la boleta';
    this.mensaje.set(
      `Prototipo: la venta y los datos de ${nombre} son correctos. El registro de la venta y el envío ` +
        'del comprobante a SUNAT se implementan en la siguiente iteración (CU08).',
    );
  }
}
