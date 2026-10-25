import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Cliente {
  documento: string;
  nombre: string;
  telefono: string;
}

/** Pantalla de prototipo: los datos viven solo en la vista. */
@Component({
  selector: 'app-clientes',
  imports: [FormsModule],
  templateUrl: './clientes.html',
})
export class Clientes {
  protected filtro = signal('');
  protected nuevo: Cliente = { documento: '', nombre: '', telefono: '' };
  protected readonly error = signal('');

  protected readonly clientes = signal<Cliente[]>([
    { documento: '45781236', nombre: 'Juan Pérez Quispe', telefono: '987 654 321' },
    { documento: '20512345678', nombre: 'Constructora Los Andes S.A.C.', telefono: '01 456 7890' },
    { documento: '71234567', nombre: 'María Torres Vega', telefono: '956 123 789' },
  ]);

  protected readonly filtrados = computed(() => {
    const f = this.filtro().toLowerCase();
    return this.clientes().filter((c) => c.documento.includes(f) || c.nombre.toLowerCase().includes(f));
  });

  agregar(): void {
    const doc = this.nuevo.documento.trim();
    if (!/^(\d{8}|\d{11})$/.test(doc) || !this.nuevo.nombre.trim()) {
      this.error.set('Ingrese un DNI (8 dígitos) o RUC (11 dígitos) y el nombre.');
      return;
    }
    this.error.set('');
    this.clientes.update((cs) => [{ ...this.nuevo, documento: doc }, ...cs]);
    this.nuevo = { documento: '', nombre: '', telefono: '' };
  }
}
