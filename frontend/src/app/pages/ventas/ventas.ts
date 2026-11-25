import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, ElementRef, HostListener, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { MetodoPago, Producto, TipoComprobante, Venta } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';
import { estiloCategoria, iconoProducto } from '../../services/categorias';
import { nivelStock } from '../../services/niveles';

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
export class Ventas implements OnInit {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly buscador = viewChild<ElementRef<HTMLInputElement>>('buscador');
  protected readonly estilo = estiloCategoria;
  protected readonly icono = iconoProducto;
  protected readonly nivel = nivelStock;

  /** Productos activos agrupados por pasillo (categoría) para vender sin escribir. */
  protected readonly catalogo = signal<Producto[]>([]);
  protected readonly pasilloAbierto = signal<string | null>(null);
  protected readonly pasillos = computed(() => {
    const mapa = new Map<string, Producto[]>();
    for (const p of this.catalogo()) {
      mapa.set(p.categoriaNombre, [...(mapa.get(p.categoriaNombre) ?? []), p]);
    }
    return [...mapa.entries()]
      .sort((a, b) => a[0].localeCompare(b[0], 'es'))
      .map(([nombre, productos]) => ({ nombre, productos }));
  });

  /** Monto que entrega el cliente (solo pantalla; no se envía a la API). */
  protected readonly recibido = signal<number | null>(null);
  protected readonly vuelto = computed(() => {
    const r = this.recibido();
    return r !== null && r >= this.total() ? r - this.total() : null;
  });
  protected readonly falta = computed(() => {
    const r = this.recibido();
    return r !== null && r < this.total() ? this.total() - r : null;
  });
  /** Montos rápidos: exacto y billetes que alcanzan para el total. */
  protected readonly montosRapidos = computed(() => {
    const t = this.total();
    return [{ texto: 'Exacto', monto: t }, ...[20, 50, 100, 200].filter((b) => b > t).slice(0, 3).map((b) => ({ texto: `S/ ${b}`, monto: b }))];
  });
  protected readonly vueltoUltimo = signal<{ recibido: number; vuelto: number } | null>(null);

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
  /** Texto buscado que no trajo resultados (para avisarlo bajo el buscador). */
  protected readonly sinResultados = signal('');
  protected readonly unidades = computed(() => this.lineas().reduce((suma, l) => suma + l.cantidad, 0));

  // Los precios ya incluyen IGV: se separa la base imponible del impuesto.
  protected readonly total = computed(() =>
    this.lineas().reduce((suma, l) => suma + l.producto.precioVenta * l.cantidad, 0),
  );
  protected readonly gravada = computed(() => this.total() / (1 + TASA_IGV));
  protected readonly igv = computed(() => this.total() - this.gravada());

  ngOnInit(): void {
    this.recargarCatalogo();
    // Desde el panel se llega con ?buscar=CÓDIGO: un solo resultado entra directo al carrito.
    const buscar = this.ruta.snapshot.queryParamMap.get('buscar');
    if (buscar) {
      this.texto = buscar;
      this.buscar();
    }
  }

  /** Recarga los pasillos (también después de cobrar, para mostrar el stock al día). */
  private recargarCatalogo(): void {
    this.api.buscarProductos('', true).subscribe({
      next: (lista) => {
        this.catalogo.set(lista);
        if (!this.pasilloAbierto() && this.pasillos().length) {
          this.pasilloAbierto.set(this.pasillos()[0].nombre);
        }
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  alternarPasillo(nombre: string): void {
    this.pasilloAbierto.set(this.pasilloAbierto() === nombre ? null : nombre);
  }

  cantidadEnCarrito(p: Producto): number {
    return this.lineas().find((l) => l.producto.id === p.id)?.cantidad ?? 0;
  }

  /** F2: venta nueva y cursor en el buscador. F9: cobrar. */
  @HostListener('document:keydown', ['$event'])
  atajos(evento: KeyboardEvent): void {
    if (evento.key === 'F2') {
      evento.preventDefault();
      this.nuevaVenta();
      this.buscador()?.nativeElement.focus();
    } else if (evento.key === 'F9') {
      evento.preventDefault();
      if (this.lineas().length > 0 && !this.enviando()) {
        this.cobrar();
      }
    }
  }

  /** RF3: búsqueda real contra la API (el lector de código de barras escribe y envía Enter). */
  buscar(): void {
    this.mensaje.set('');
    this.api.buscarProductos(this.texto, true).subscribe({
      next: (lista) => {
        this.error.set('');
        this.sinResultados.set(lista.length === 0 ? this.texto.trim() : '');
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
          const recibido = this.recibido();
          this.vueltoUltimo.set(this.metodoPago === 'EFECTIVO' && recibido !== null && recibido >= venta.total ? { recibido, vuelto: recibido - venta.total } : null);
          this.recibido.set(null);
          this.ultimaVenta.set(venta);
          this.recargarCatalogo();
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
    this.vueltoUltimo.set(null);
    this.mensaje.set('');
  }

  imprimir(): void {
    window.print();
  }
}
