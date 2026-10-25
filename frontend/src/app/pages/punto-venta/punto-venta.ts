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

@Component({
  selector: 'app-punto-venta',
  imports: [CurrencyPipe, FormsModule],
  templateUrl: './punto-venta.html',
})
export class PuntoVenta {
  private readonly api = inject(ApiService);

  protected texto = '';
  protected metodoPago = 'EFECTIVO';
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

  cobrar(): void {
    this.mensaje.set(
      'Prototipo: la venta se calculó correctamente. El registro de la venta y la nota de venta ' +
        'se implementan en la siguiente iteración (CU08).',
    );
  }
}
