import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { Movimiento, Producto, TipoMovimiento } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

@Component({
  selector: 'app-inventario',
  imports: [DatePipe, FormsModule, ReactiveFormsModule],
  templateUrl: './inventario.html',
})
export class Inventario implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly productos = signal<Producto[]>([]);
  protected readonly movimientos = signal<Movimiento[]>([]);
  protected readonly error = signal('');
  protected readonly exito = signal('');
  protected filtroProducto: number | null = null;

  protected readonly form = inject(FormBuilder).group({
    productoId: [null as number | null, Validators.required],
    tipo: ['ENTRADA' as TipoMovimiento, Validators.required],
    cantidad: [null as number | null, [Validators.required, Validators.min(0)]],
    motivo: ['', [Validators.required, Validators.maxLength(150)]],
  });

  private readonly tipoElegido = signal<TipoMovimiento>('ENTRADA');
  protected readonly etiquetaCantidad = computed(() =>
    this.tipoElegido() === 'AJUSTE' ? 'Stock contado' : 'Cantidad',
  );
  protected readonly stockBajo = computed(() => this.productos().filter((p) => p.stockBajo));

  ngOnInit(): void {
    this.form.controls.tipo.valueChanges.subscribe((t) => this.tipoElegido.set(t ?? 'ENTRADA'));
    this.cargarProductos();
    this.cargarKardex();
  }

  registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const datos = this.form.getRawValue() as {
      productoId: number; tipo: TipoMovimiento; cantidad: number; motivo: string;
    };
    this.api.registrarMovimiento(datos).subscribe({
      next: (m) => {
        this.error.set('');
        this.exito.set(`${m.productoDescripcion}: stock ${m.stockAnterior} → ${m.stockResultante}.`);
        this.form.reset({ tipo: datos.tipo, productoId: datos.productoId });
        this.cargarProductos();
        this.cargarKardex();
      },
      error: (e: HttpErrorResponse) => {
        this.exito.set('');
        this.error.set(mensajeDeError(e));
      },
    });
  }

  cargarKardex(): void {
    this.api.movimientos(this.filtroProducto ?? undefined).subscribe({
      next: (lista) => this.movimientos.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[campo];
    return control.touched && control.invalid;
  }

  private cargarProductos(): void {
    this.api.buscarProductos('', true).subscribe({
      next: (lista) => this.productos.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
