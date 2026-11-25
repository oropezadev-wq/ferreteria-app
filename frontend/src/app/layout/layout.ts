import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { Rol } from '../models/modelos';
import { AuthService } from '../services/auth.service';

interface OpcionMenu {
  ruta: string;
  texto: string;
  /** Texto corto para la navegación inferior del celular. */
  corto: string;
  icono: string;
  roles: Rol[];
}

const MENU: OpcionMenu[] = [
  { ruta: '/panel', texto: 'Panel principal', corto: 'Panel', icono: 'bi-speedometer2', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/ventas', texto: 'Ventas', corto: 'Ventas', icono: 'bi-upc-scan', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/caja', texto: 'Caja', corto: 'Caja', icono: 'bi-cash-coin', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/clientes', texto: 'Clientes', corto: 'Clientes', icono: 'bi-people', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/productos', texto: 'Productos', corto: 'Productos', icono: 'bi-box-seam', roles: ['ADMINISTRADOR'] },
  { ruta: '/inventario', texto: 'Inventario y kardex', corto: 'Kardex', icono: 'bi-clipboard-data', roles: ['ADMINISTRADOR'] },
  { ruta: '/reportes', texto: 'Reportes', corto: 'Reportes', icono: 'bi-bar-chart', roles: ['ADMINISTRADOR'] },
  { ruta: '/usuarios', texto: 'Usuarios', corto: 'Usuarios', icono: 'bi-person-gear', roles: ['ADMINISTRADOR'] },
];

/** En el celular caben 4 accesos abajo; el resto va en "Más". */
const ABAJO = 4;

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly masAbierto = signal(false);
  protected readonly opciones = computed(() => MENU.filter((o) => this.auth.tieneRol(o.roles)));
  protected readonly abajo = computed(() => {
    const todas = this.opciones();
    return todas.length <= ABAJO + 1 ? todas : todas.slice(0, ABAJO);
  });
  protected readonly enMas = computed(() => {
    const todas = this.opciones();
    return todas.length <= ABAJO + 1 ? [] : todas.slice(ABAJO);
  });
  protected readonly iniciales = computed(() =>
    (this.auth.usuario()?.nombre ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join(''),
  );

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.masAbierto.set(false));
  }

  /** F2 abre Ventas desde cualquier pantalla (en Ventas, además, empieza una venta nueva). */
  @HostListener('document:keydown.F2', ['$event'])
  irAVentas(evento: Event): void {
    evento.preventDefault();
    if (!this.router.url.startsWith('/ventas')) {
      this.router.navigate(['/ventas']);
    }
  }

  salir(): void {
    this.auth.salir();
    this.router.navigate(['/login']);
  }
}
