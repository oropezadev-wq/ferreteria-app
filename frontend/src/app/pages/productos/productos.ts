import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { Categoria, Producto } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';
import { estiloCategoria } from '../../services/categorias';
import { coberturaMinimo, nivelStock } from '../../services/niveles';

@Component({
  selector: 'app-productos',
  imports: [CurrencyPipe, DecimalPipe, FormsModule, ReactiveFormsModule],
  templateUrl: './productos.html',
})
export class Productos implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly productos = signal<Producto[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly error = signal('');
  protected readonly exito = signal('');
  protected readonly formularioVisible = signal(false);
  protected readonly editandoId = signal<number | null>(null);
  protected texto = '';
  protected readonly estilo = estiloCategoria;
  /** Filtros que se aplican en pantalla sobre la lista ya cargada. */
  protected readonly filtroCategoria = signal<number | null>(null);
  protected readonly soloStockBajo = signal(false);
  protected readonly visibles = computed(() =>
    this.productos().filter(
      (p) =>
        (this.filtroCategoria() === null || p.categoriaId === this.filtroCategoria()) &&
        (!this.soloStockBajo() || (p.stockBajo && p.activo)),
    ),
  );
  protected readonly cantidadStockBajo = computed(() => this.productos().filter((p) => p.stockBajo && p.activo).length);
  /** Cuántos productos hay en cada categoría (para los botones de pasillo). */
  protected readonly porCategoria = computed(() => {
    const mapa = new Map<number, number>();
    for (const p of this.productos()) {
      mapa.set(p.categoriaId, (mapa.get(p.categoriaId) ?? 0) + 1);
    }
    return mapa;
  });
  /** Densidad de la tabla: compacta muestra más filas en pantallas grandes. */
  protected readonly densa = signal(false);
  protected readonly nivel = nivelStock;
  protected readonly cobertura = coberturaMinimo;

  /** Margen sobre el precio de venta (lo que queda de cada sol vendido). */
  margen(p: Producto): number | null {
    return p.precioVenta > 0 ? ((p.precioVenta - p.precioCompra) / p.precioVenta) * 100 : null;
  }
  protected nuevaCategoria = '';

  protected readonly form = inject(FormBuilder).group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    descripcion: ['', [Validators.required, Validators.maxLength(120)]],
    categoriaId: [null as number | null, Validators.required],
    precioCompra: [null as number | null, [Validators.required, Validators.min(0)]],
    precioVenta: [null as number | null, [Validators.required, Validators.min(0.01)]],
    stockMinimo: [0 as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.buscar();
    this.cargarCategorias();
  }

  buscar(): void {
    this.api.buscarProductos(this.texto).subscribe({
      next: (lista) => this.productos.set(lista),
      error: (e) => this.mostrarError(e),
    });
  }

  nuevo(): void {
    this.editandoId.set(null);
    this.form.reset({ stockMinimo: 0 });
    this.formularioVisible.set(true);
  }

  editar(p: Producto): void {
    this.editandoId.set(p.id);
    this.form.setValue({
      codigo: p.codigo,
      descripcion: p.descripcion,
      categoriaId: p.categoriaId,
      precioCompra: p.precioCompra,
      precioVenta: p.precioVenta,
      stockMinimo: p.stockMinimo,
    });
    this.formularioVisible.set(true);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const datos = this.form.getRawValue() as {
      codigo: string; descripcion: string; categoriaId: number;
      precioCompra: number; precioVenta: number; stockMinimo: number;
    };
    const id = this.editandoId();
    const peticion = id ? this.api.actualizarProducto(id, datos) : this.api.crearProducto(datos);
    peticion.subscribe({
      next: (p) => {
        this.exito.set(`Producto ${p.codigo} guardado.`);
        this.error.set('');
        this.formularioVisible.set(false);
        this.buscar();
      },
      error: (e) => this.mostrarError(e),
    });
  }

  cambiarEstado(p: Producto): void {
    this.api.cambiarEstado(p.id, !p.activo).subscribe({
      next: () => this.buscar(),
      error: (e) => this.mostrarError(e),
    });
  }

  agregarCategoria(): void {
    const nombre = this.nuevaCategoria.trim();
    if (!nombre) {
      return;
    }
    this.api.crearCategoria(nombre).subscribe({
      next: (c) => {
        this.nuevaCategoria = '';
        this.cargarCategorias();
        this.form.controls.categoriaId.setValue(c.id);
      },
      error: (e) => this.mostrarError(e),
    });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[campo];
    return control.touched && control.invalid;
  }

  private cargarCategorias(): void {
    this.api.categorias().subscribe({
      next: (lista) => this.categorias.set(lista),
      error: (e) => this.mostrarError(e),
    });
  }

  private mostrarError(e: HttpErrorResponse): void {
    this.exito.set('');
    this.error.set(mensajeDeError(e));
  }
}
