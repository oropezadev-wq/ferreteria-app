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

export type TipoMovimiento = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'DEVOLUCION';

export type TipoDocumento = 'GUIA_REMISION' | 'FACTURA' | 'BOLETA';

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
  tipoDocumento: TipoDocumento | null;
  numeroDocumento: string | null;
  fechaDocumento: string | null;
  proveedor: string | null;
}

export interface MovimientoRequest {
  productoId: number;
  tipo: TipoMovimiento;
  cantidad: number;
  motivo: string;
  tipoDocumento: TipoDocumento | null;
  numeroDocumento: string | null;
  fechaDocumento: string | null;
  proveedor: string | null;
}

export interface Resumen {
  productosActivos: number;
  productosStockBajo: number;
  valorInventario: number;
  movimientosHoy: number;
  ventasHoy: number;
}

export type Rol = 'ADMINISTRADOR' | 'VENDEDOR';

export interface Usuario {
  nombre: string;
  usuario: string;
  rol: Rol;
}

export type TipoComprobante = 'BOLETA' | 'FACTURA';

export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'YAPE';

export interface VentaRequest {
  tipoComprobante: TipoComprobante;
  metodoPago: MetodoPago;
  clienteDocumento: string | null;
  clienteNombre: string | null;
  lineas: { productoId: number; cantidad: number }[];
}

export interface DetalleVenta {
  codigo: string;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Venta {
  id: number;
  comprobante: string;
  tipoComprobante: TipoComprobante;
  fecha: string;
  clienteDocumento: string | null;
  clienteNombre: string | null;
  vendedor: string;
  metodoPago: MetodoPago;
  gravada: number;
  igv: number;
  total: number;
  estado: 'REGISTRADA' | 'ANULADA';
  estadoSunat: string;
  motivoAnulacion: string | null;
  detalles: DetalleVenta[];
}

export interface Cliente {
  id: number;
  tipoDocumento: 'DNI' | 'RUC';
  numeroDocumento: string;
  nombre: string;
  telefono: string | null;
}

export interface ClienteRequest {
  numeroDocumento: string;
  nombre: string;
  telefono: string | null;
}

export interface UsuarioFila {
  id: number;
  usuario: string;
  nombre: string;
  rol: Rol;
  activo: boolean;
}

export interface UsuarioRequest {
  usuario: string;
  nombre: string;
  clave: string;
  rol: Rol;
}

export interface TurnoCaja {
  id: number;
  abierto: boolean;
  montoApertura: number;
  ventasEfectivo: number;
  ventasTarjeta: number;
  ventasYape: number;
  montoEsperado: number;
  montoContado: number | null;
  diferencia: number | null;
  observacion: string | null;
  fechaApertura: string;
  fechaCierre: string | null;
}

export interface Rotacion {
  producto: string;
  unidades: number;
}

export interface ReporteVentas {
  desde: string;
  hasta: string;
  cantidadVentas: number;
  cantidadAnuladas: number;
  total: number;
  costo: number;
  utilidad: number;
  ventasPorDia: { fecha: string; total: number }[];
  mayorRotacion: Rotacion[];
  menorRotacion: Rotacion[];
}

export interface ProductoCatalogo {
  codigo: string;
  descripcion: string;
  categoria: string;
  precioVenta: number;
  disponible: boolean;
}
