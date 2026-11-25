import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Categoria, ProductoCatalogo } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';
import { estiloCategoria, iconoProducto } from '../../services/categorias';

type Orden = 'nombre' | 'precio-asc' | 'precio-desc';

/**
 * Página pública para los clientes de la ferretería: consultan productos, precios y
 * disponibilidad sin iniciar sesión. Solo lectura: la compra se hace en el mostrador.
 *
 * La búsqueda por texto va a la API; el filtro por categoría y el orden se aplican en
 * pantalla sobre ese resultado, así cada categoría muestra cuántos productos tiene.
 */
@Component({
  selector: 'app-catalogo',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './catalogo.html',
})
export class Catalogo implements OnInit {
  private readonly api = inject(ApiService);

  protected texto = '';
  protected readonly buscado = signal('');
  protected readonly categoriaId = signal<number | undefined>(undefined);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly encontrados = signal<ProductoCatalogo[]>([]);
  protected readonly orden = signal<Orden>('nombre');
  protected readonly error = signal('');
  protected readonly cargando = signal(true);
  protected readonly estilo = estiloCategoria;
  protected readonly icono = iconoProducto;
  protected readonly esqueletos = Array.from({ length: 8 }, (_, i) => i);

  private readonly categoriaElegida = computed(
    () => this.categorias().find((c) => c.id === this.categoriaId())?.nombre,
  );

  /** Cantidad de productos del resultado actual por nombre de categoría. */
  protected readonly conteo = computed(() => {
    const mapa = new Map<string, number>();
    for (const p of this.encontrados()) {
      mapa.set(p.categoria, (mapa.get(p.categoria) ?? 0) + 1);
    }
    return mapa;
  });

  protected readonly productos = computed(() => {
    const categoria = this.categoriaElegida();
    const lista = categoria ? this.encontrados().filter((p) => p.categoria === categoria) : [...this.encontrados()];
    const orden = this.orden();
    return lista.sort((a, b) => {
      // Lo agotado va al final para que no tape lo que sí se puede comprar.
      if (a.disponible !== b.disponible) {
        return a.disponible ? -1 : 1;
      }
      if (orden === 'precio-asc') return a.precioVenta - b.precioVenta;
      if (orden === 'precio-desc') return b.precioVenta - a.precioVenta;
      return a.descripcion.localeCompare(b.descripcion, 'es');
    });
  });

  protected readonly titulo = computed(() => this.categoriaElegida() ?? 'Todos los productos');

  ngOnInit(): void {
    this.api.categoriasCatalogo().subscribe({
      next: (c) => this.categorias.set(c),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
    this.buscar();
  }

  elegirCategoria(id?: number): void {
    this.categoriaId.set(id);
  }

  cambiarOrden(valor: string): void {
    this.orden.set(valor as Orden);
  }

  limpiarBusqueda(): void {
    this.texto = '';
    this.buscar();
  }

  buscar(): void {
    this.cargando.set(true);
    const texto = this.texto.trim();
    this.api.catalogo(texto).subscribe({
      next: (p) => {
        this.error.set('');
        this.buscado.set(texto);
        this.encontrados.set(p);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
