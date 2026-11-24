import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Categoria, ProductoCatalogo } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

/**
 * Página pública para los clientes de la ferretería: consultan productos, precios y
 * disponibilidad sin iniciar sesión. Solo lectura: la compra se hace en el mostrador.
 */
@Component({
  selector: 'app-catalogo',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './catalogo.html',
})
export class Catalogo implements OnInit {
  private readonly api = inject(ApiService);

  protected texto = '';
  protected readonly categoriaId = signal<number | undefined>(undefined);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<ProductoCatalogo[]>([]);
  protected readonly error = signal('');
  protected readonly cargando = signal(true);

  ngOnInit(): void {
    this.api.categoriasCatalogo().subscribe({
      next: (c) => this.categorias.set(c),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
    this.buscar();
  }

  elegirCategoria(id?: number): void {
    this.categoriaId.set(id);
    this.buscar();
  }

  buscar(): void {
    this.cargando.set(true);
    this.api.catalogo(this.texto.trim(), this.categoriaId()).subscribe({
      next: (p) => {
        this.error.set('');
        this.productos.set(p);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
