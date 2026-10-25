import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly error = signal('');
  protected readonly form = inject(FormBuilder).nonNullable.group({
    usuario: ['', Validators.required],
    clave: ['', Validators.required],
  });

  ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { usuario, clave } = this.form.getRawValue();
    if (this.auth.ingresar(usuario.trim(), clave)) {
      this.router.navigate(['/panel']);
    } else {
      this.error.set('Usuario o contraseña incorrectos.');
    }
  }
}
