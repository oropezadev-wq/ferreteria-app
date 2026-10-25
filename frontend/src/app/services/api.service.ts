import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Categoria,
  Movimiento,
  MovimientoRequest,
  Producto,
  ProductoRequest,
  Resumen,
} from '../models/modelos';

const API = 'http://localhost:8081/api';

/** Único punto de la vista que habla con la capa de negocio (API REST). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  resumen(): Observable<Resumen> {
    return this.http.get<Resumen>(`${API}/panel/resumen`);
  }

  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${API}/categorias`);
  }

  crearCategoria(nombre: string): Observable<Categoria> {
    return this.http.post<Categoria>(`${API}/categorias`, { nombre });
  }

  buscarProductos(texto = '', soloActivos = false): Observable<Producto[]> {
    const params = new HttpParams().set('buscar', texto).set('soloActivos', soloActivos);
    return this.http.get<Producto[]>(`${API}/productos`, { params });
  }

  alertasStock(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${API}/productos/alertas`);
  }

  crearProducto(producto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(`${API}/productos`, producto);
  }

  actualizarProducto(id: number, producto: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${API}/productos/${id}`, producto);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Producto> {
    const accion = activo ? 'activar' : 'desactivar';
    return this.http.patch<Producto>(`${API}/productos/${id}/${accion}`, null);
  }

  movimientos(productoId?: number): Observable<Movimiento[]> {
    let params = new HttpParams();
    if (productoId) {
      params = params.set('productoId', productoId);
    }
    return this.http.get<Movimiento[]>(`${API}/movimientos`, { params });
  }

  registrarMovimiento(movimiento: MovimientoRequest): Observable<Movimiento> {
    return this.http.post<Movimiento>(`${API}/movimientos`, movimiento);
  }
}

/** Convierte la respuesta de error de la API en un texto para mostrar al usuario. */
export function mensajeDeError(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que la API esté encendida.';
  }
  const cuerpo = error.error;
  if (cuerpo?.detalles?.length) {
    return `${cuerpo.mensaje}: ${cuerpo.detalles.join(', ')}`;
  }
  return cuerpo?.mensaje ?? 'Ocurrió un error inesperado.';
}
