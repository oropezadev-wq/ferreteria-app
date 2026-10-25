import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Producto, Resumen } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-panel',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './panel.html',
})
export class Panel implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly alertas = signal<Producto[]>([]);
  protected readonly error = signal('');

  ngOnInit(): void {
    const alFallar = (e: HttpErrorResponse) => this.error.set(mensajeDeError(e));
    this.api.resumen().subscribe({ next: (r) => this.resumen.set(r), error: alFallar });
    this.api.alertasStock().subscribe({ next: (a) => this.alertas.set(a), error: alFallar });
  }
}
