import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Cliente } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

interface FormCliente {
  numeroDocumento: string;
  nombre: string;
  telefono: string;
}

const VACIO: FormCliente = { numeroDocumento: '', nombre: '', telefono: '' };

/** RF de clientes: registrar, buscar y editar compradores con DNI o RUC. */
@Component({
  selector: 'app-clientes',
  imports: [FormsModule],
  templateUrl: './clientes.html',
})
export class Clientes implements OnInit {
  private readonly api = inject(ApiService);

  protected filtro = '';
  protected form: FormCliente = { ...VACIO };
  protected readonly editando = signal<Cliente | null>(null);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly error = signal('');
  protected readonly mensaje = signal('');

  ngOnInit(): void {
    this.buscar();
  }

  buscar(): void {
    this.api.clientes(this.filtro.trim()).subscribe({
      next: (lista) => this.clientes.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  editar(cliente: Cliente): void {
    this.editando.set(cliente);
    this.form = { numeroDocumento: cliente.numeroDocumento, nombre: cliente.nombre, telefono: cliente.telefono ?? '' };
    this.error.set('');
    this.mensaje.set('');
  }

  cancelar(): void {
    this.editando.set(null);
    this.form = { ...VACIO };
  }

  guardar(): void {
    const datos = {
      numeroDocumento: this.form.numeroDocumento.trim(),
      nombre: this.form.nombre.trim(),
      telefono: this.form.telefono.trim() || null,
    };
    if (!/^(\d{8}|\d{11})$/.test(datos.numeroDocumento) || !datos.nombre) {
      this.error.set('Ingrese un DNI (8 dígitos) o RUC (11 dígitos) y el nombre.');
      return;
    }
    const actual = this.editando();
    const peticion = actual ? this.api.actualizarCliente(actual.id, datos) : this.api.crearCliente(datos);
    peticion.subscribe({
      next: (c) => {
        this.error.set('');
        this.mensaje.set(actual ? `Se actualizó a ${c.nombre}.` : `Se registró a ${c.nombre}.`);
        this.cancelar();
        this.buscar();
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
