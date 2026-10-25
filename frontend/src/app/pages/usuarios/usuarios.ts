import { Component, signal } from '@angular/core';

interface FilaUsuario {
  nombre: string;
  usuario: string;
  rol: 'Administrador' | 'Vendedor';
  activo: boolean;
}

/** Pantalla de prototipo de gestión de usuarios (CU02) con datos de ejemplo. */
@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.html',
})
export class Usuarios {
  protected readonly usuarios = signal<FilaUsuario[]>([
    { nombre: 'Administrador', usuario: 'admin', rol: 'Administrador', activo: true },
    { nombre: 'Vendedor de turno', usuario: 'vendedor', rol: 'Vendedor', activo: true },
    { nombre: 'Vendedor de fin de semana', usuario: 'vendedor2', rol: 'Vendedor', activo: false },
  ]);

  alternar(fila: FilaUsuario): void {
    this.usuarios.update((us) => us.map((u) => (u === fila ? { ...u, activo: !u.activo } : u)));
  }
}
