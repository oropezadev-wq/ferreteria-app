import { Producto } from '../models/modelos';

/**
 * Nivel de stock con lo que la API entrega hoy (stock y mínimo):
 * - critico: sin stock o con la mitad del mínimo o menos.
 * - bajo: por debajo del mínimo (o en el mínimo, si la API lo marca como bajo).
 * - ok: sobre el mínimo.
 * La urgencia por días de cobertura necesita las ventas por producto (backend).
 */
export type NivelStock = 'critico' | 'bajo' | 'ok';

export function nivelStock(p: Pick<Producto, 'stock' | 'stockMinimo' | 'stockBajo'>): NivelStock {
  if (p.stock <= 0 || (p.stockMinimo > 0 && p.stock <= p.stockMinimo / 2)) {
    return 'critico';
  }
  return p.stockBajo ? 'bajo' : 'ok';
}

/** Porcentaje del mínimo cubierto (para la barra de nivel), tope 100. */
export function coberturaMinimo(p: Pick<Producto, 'stock' | 'stockMinimo'>): number {
  return p.stockMinimo > 0 ? Math.min(100, (p.stock / p.stockMinimo) * 100) : 100;
}
