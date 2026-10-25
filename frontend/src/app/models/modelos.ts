// Tipos que devuelve y recibe la API (mismos campos que los DTO del backend).

export interface Categoria {
  id: number;
  nombre: string;
}

export interface Producto {
  id: number;
  codigo: string;
  descripcion: string;
  categoriaId: number;
  categoriaNombre: string;
  precioCompra: number;
  precioVenta: number;
  stock: number;
  stockMinimo: number;
  stockBajo: boolean;
  activo: boolean;
}

export interface ProductoRequest {
  codigo: string;
  descripcion: string;
  categoriaId: number;
  precioCompra: number;
  precioVenta: number;
  stockMinimo: number;
}

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE';

export interface Movimiento {
  id: number;
  productoId: number;
  productoCodigo: string;
  productoDescripcion: string;
  tipo: TipoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockResultante: number;
  motivo: string;
  fecha: string;
}

export interface MovimientoRequest {
  productoId: number;
  tipo: TipoMovimiento;
  cantidad: number;
  motivo: string;
}

export interface Resumen {
  productosActivos: number;
  productosStockBajo: number;
  valorInventario: number;
  movimientosHoy: number;
}

export type Rol = 'ADMINISTRADOR' | 'VENDEDOR';

export interface Usuario {
  nombre: string;
  usuario: string;
  rol: Rol;
}
