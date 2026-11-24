import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Rol, UsuarioFila } from '../../models/modelos';
import { ApiService, mensajeDeError } from '../../services/api.service';

/** CU02: el administrador crea usuarios y los activa o desactiva. */
@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule],
  templateUrl: './usuarios.html',
})
export class Usuarios implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly usuarios = signal<UsuarioFila[]>([]);
  protected readonly error = signal('');
  protected readonly mensaje = signal('');
  protected readonly formAbierto = signal(false);

  // Mismas reglas que valida la API: así el usuario ve el error antes de enviar.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    usuario: ['', [Validators.required, Validators.pattern(/^[a-z0-9._]{3,30}$/)]],
    clave: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)]],
    rol: ['VENDEDOR' as Rol, Validators.required],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.usuarios().subscribe({
      next: (lista) => this.usuarios.set(lista),
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.api.crearUsuario(this.form.getRawValue()).subscribe({
      next: (u) => {
        this.error.set('');
        this.mensaje.set(`Se creó el usuario ${u.usuario}.`);
        this.form.reset();
        this.formAbierto.set(false);
        this.cargar();
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }

  alternar(fila: UsuarioFila): void {
    this.api.cambiarEstadoUsuario(fila.id, !fila.activo).subscribe({
      next: () => {
        this.error.set('');
        this.cargar();
      },
      error: (e) => this.error.set(mensajeDeError(e)),
    });
  }
}
