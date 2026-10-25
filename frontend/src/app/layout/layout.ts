import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Rol } from '../models/modelos';
import { AuthService } from '../services/auth.service';

interface OpcionMenu {
  ruta: string;
  texto: string;
  icono: string;
  roles: Rol[];
}

const MENU: OpcionMenu[] = [
  { ruta: '/panel', texto: 'Panel principal', icono: 'bi-speedometer2', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/punto-venta', texto: 'Punto de venta', icono: 'bi-cart3', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/clientes', texto: 'Clientes', icono: 'bi-people', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/caja', texto: 'Caja', icono: 'bi-cash-coin', roles: ['ADMINISTRADOR', 'VENDEDOR'] },
  { ruta: '/productos', texto: 'Productos', icono: 'bi-box-seam', roles: ['ADMINISTRADOR'] },
  { ruta: '/inventario', texto: 'Inventario y kardex', icono: 'bi-clipboard-data', roles: ['ADMINISTRADOR'] },
  { ruta: '/reportes', texto: 'Reportes', icono: 'bi-bar-chart', roles: ['ADMINISTRADOR'] },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'bi-person-gear', roles: ['ADMINISTRADOR'] },
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

  salir(): void {
    this.auth.salir();
    this.router.navigate(['/login']);
  }
}
