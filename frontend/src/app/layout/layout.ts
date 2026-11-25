import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Rol } from '../models/modelos';
import { AuthService } from '../services/auth.service';

interface OpcionMenu {
  ruta: string;
  texto: string;
  icono: string;
  roles: Rol[];
  grupo: 'Mostrador' | 'Administración';
}

const MENU: OpcionMenu[] = [
  { ruta: '/panel', texto: 'Panel principal', icono: 'bi-speedometer2', roles: ['ADMINISTRADOR', 'VENDEDOR'], grupo: 'Mostrador' },
  { ruta: '/ventas', texto: 'Ventas', icono: 'bi-cart3', roles: ['ADMINISTRADOR', 'VENDEDOR'], grupo: 'Mostrador' },
  { ruta: '/clientes', texto: 'Clientes', icono: 'bi-people', roles: ['ADMINISTRADOR', 'VENDEDOR'], grupo: 'Mostrador' },
  { ruta: '/caja', texto: 'Caja', icono: 'bi-cash-coin', roles: ['ADMINISTRADOR', 'VENDEDOR'], grupo: 'Mostrador' },
  { ruta: '/productos', texto: 'Productos', icono: 'bi-box-seam', roles: ['ADMINISTRADOR'], grupo: 'Administración' },
  { ruta: '/inventario', texto: 'Inventario y kardex', icono: 'bi-clipboard-data', roles: ['ADMINISTRADOR'], grupo: 'Administración' },
  { ruta: '/reportes', texto: 'Reportes', icono: 'bi-bar-chart', roles: ['ADMINISTRADOR'], grupo: 'Administración' },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'bi-person-gear', roles: ['ADMINISTRADOR'], grupo: 'Administración' },
];

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly menuAbierto = signal(false);
  protected readonly opciones = computed(() => MENU.filter((o) => this.auth.tieneRol(o.roles)));
  /** El menú se agrupa en lo que se usa en el mostrador y lo que administra el negocio. */
  protected readonly grupos = computed(() =>
    (['Mostrador', 'Administración'] as const)
      .map((nombre) => ({ nombre, opciones: this.opciones().filter((o) => o.grupo === nombre) }))
      .filter((g) => g.opciones.length > 0),
  );
  protected readonly iniciales = computed(() =>
    (this.auth.usuario()?.nombre ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join(''),
  );
  protected readonly hoy = ((texto: string) => texto.charAt(0).toUpperCase() + texto.slice(1))(
    new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date()),
  );

  salir(): void {
    this.auth.salir();
    this.router.navigate(['/login']);
  }
}
