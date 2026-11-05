import { DatePipe, formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { Movimiento, Producto, TipoDocumento, TipoMovimiento } from '../../models/modelos';
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
  protected filtroDocumento = '';

  protected readonly nombreDocumento: Record<TipoDocumento, string> = {
    GUIA_REMISION: 'Guía de remisión',
    FACTURA: 'Factura',
    BOLETA: 'Boleta',
  };
  protected readonly hoy = formatDate(new Date(), 'yyyy-MM-dd', 'en-US');

  protected readonly form = inject(FormBuilder).group({
    productoId: [null as number | null, Validators.required],
    tipo: ['ENTRADA' as TipoMovimiento, Validators.required],
    cantidad: [null as number | null, [Validators.required, Validators.min(0)]],
    motivo: ['', [Validators.required, Validators.maxLength(150)]],
    tipoDocumento: [null as TipoDocumento | null],
    numeroDocumento: [''],
    fechaDocumento: [''],
    proveedor: [''],
  });

  private readonly tipoElegido = signal<TipoMovimiento>('ENTRADA');
  protected readonly esEntrada = computed(() => this.tipoElegido() === 'ENTRADA');
  protected readonly etiquetaCantidad = computed(() =>
    this.tipoElegido() === 'AJUSTE' ? 'Stock contado' : 'Cantidad',
  );
  protected readonly stockBajo = computed(() => this.productos().filter((p) => p.stockBajo));

  ngOnInit(): void {
    this.form.controls.tipo.valueChanges.subscribe((t) => {
      this.tipoElegido.set(t ?? 'ENTRADA');
      this.validarDocumento(t === 'ENTRADA');
    });
    this.validarDocumento(true);
    this.cargarProductos();
    this.cargarKardex();
  }

  registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const entrada = v.tipo === 'ENTRADA';
    const datos = {
      productoId: v.productoId as number,
      tipo: v.tipo as TipoMovimiento,
      cantidad: v.cantidad as number,
      motivo: v.motivo ?? '',
      tipoDocumento: entrada ? v.tipoDocumento : null,
      numeroDocumento: entrada ? (v.numeroDocumento ?? '').trim().toUpperCase() : null,
      fechaDocumento: entrada ? v.fechaDocumento : null,
      proveedor: entrada ? v.proveedor : null,
    };
    this.api.registrarMovimiento(datos).subscribe({
      next: (m) => {
        this.error.set('');
        this.exito.set(`${m.productoDescripcion}: stock ${m.stockAnterior} → ${m.stockResultante}.`);
        // Una guía suele traer varios productos: el documento se mantiene para el siguiente.
        this.form.reset({
          tipo: datos.tipo,
          productoId: datos.productoId,
          tipoDocumento: datos.tipoDocumento,
          numeroDocumento: datos.numeroDocumento,
          fechaDocumento: datos.fechaDocumento,
          proveedor: datos.proveedor,
        });
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
    this.api.movimientos(this.filtroProducto ?? undefined, this.filtroDocumento.trim()).subscribe({
      next: (lista) => this.movimientos.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[campo];
    return control.touched && control.invalid;
  }

  /** El documento del proveedor solo se pide en las entradas. */
  private validarDocumento(obligatorio: boolean): void {
    const c = this.form.controls;
    const reglas = obligatorio
      ? {
          tipoDocumento: [Validators.required],
          numeroDocumento: [Validators.required, Validators.pattern(/^[A-Za-z0-9]{1,4}-\d{1,8}$/)],
          fechaDocumento: [Validators.required],
          proveedor: [Validators.required, Validators.maxLength(120)],
        }
      : { tipoDocumento: [], numeroDocumento: [], fechaDocumento: [], proveedor: [] };
    for (const [campo, validadores] of Object.entries(reglas)) {
      const control = c[campo as keyof typeof reglas];
      control.setValidators(validadores);
      control.updateValueAndValidity({ emitEvent: false });
    }
  }

  private cargarProductos(): void {
    this.api.buscarProductos('', true).subscribe({
      next: (lista) => this.productos.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
