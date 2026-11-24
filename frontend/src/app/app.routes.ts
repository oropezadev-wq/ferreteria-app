import { Routes } from '@angular/router';

import { Layout } from './layout/layout';
import { Caja } from './pages/caja/caja';
import { Catalogo } from './pages/catalogo/catalogo';
import { Clientes } from './pages/clientes/clientes';
import { Inventario } from './pages/inventario/inventario';
import { Login } from './pages/login/login';
import { Panel } from './pages/panel/panel';
import { Productos } from './pages/productos/productos';
import { Ventas } from './pages/ventas/ventas';
import { Reportes } from './pages/reportes/reportes';
import { Usuarios } from './pages/usuarios/usuarios';
import { authGuard } from './services/auth.guard';

const ADMIN = { roles: ['ADMINISTRADOR'] };
const VENDEDOR = { roles: ['VENDEDOR', 'ADMINISTRADOR'] };

export const routes: Routes = [
  { path: 'login', component: Login },
  // Página pública para los clientes: no pide inicio de sesión.
  { path: 'catalogo', component: Catalogo },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: 'panel', component: Panel },
      { path: 'ventas', component: Ventas, data: VENDEDOR },
      { path: 'clientes', component: Clientes, data: VENDEDOR },
      { path: 'caja', component: Caja, data: VENDEDOR },
      { path: 'productos', component: Productos, data: ADMIN },
      { path: 'inventario', component: Inventario, data: ADMIN },
      { path: 'reportes', component: Reportes, data: ADMIN },
      { path: 'usuarios', component: Usuarios, data: ADMIN },
      { path: '', redirectTo: 'panel', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
