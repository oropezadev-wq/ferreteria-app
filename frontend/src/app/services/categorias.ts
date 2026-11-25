/**
 * Ícono y color de pasillo de cada categoría. La API no guarda imágenes de productos:
 * cada tarjeta se ilustra con el ícono del rubro. Las categorías nuevas que cree el
 * administrador reciben un tono fijo según su nombre y el ícono genérico de caja.
 */
export interface EstiloCategoria {
  icono: string;
  /** Clase de pasillo (define --cat con el color de la categoría). */
  tono: string;
}

const REGLAS: { clave: RegExp; icono: string; tono: string }[] = [
  { clave: /gasfit|plomer|tuber/, icono: 'bi-droplet-half', tono: 'pasillo-agua' },
  { clave: /electr/, icono: 'bi-lightning-charge', tono: 'pasillo-ocre' },
  { clave: /manual|herramient/, icono: 'bi-hammer', tono: 'pasillo-ciruela' },
  { clave: /pintur/, icono: 'bi-paint-bucket', tono: 'pasillo-musgo' },
  { clave: /tornill|fijaci|clavo|perno/, icono: 'bi-nut', tono: 'pasillo-pizarra' },
];

const TONOS_LIBRES = ['pasillo-pizarra', 'pasillo-musgo', 'pasillo-ciruela', 'pasillo-agua', 'pasillo-ocre'];

function normalizar(texto: string): string {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

export function estiloCategoria(nombre: string | null | undefined): EstiloCategoria {
  const texto = normalizar(nombre ?? '');
  const regla = REGLAS.find((r) => r.clave.test(texto));
  if (regla) {
    return { icono: regla.icono, tono: regla.tono };
  }
  let suma = 0;
  for (const letra of texto) {
    suma += letra.charCodeAt(0);
  }
  return { icono: 'bi-box-seam', tono: TONOS_LIBRES[suma % TONOS_LIBRES.length] };
}

/** Ícono por tipo de producto según su descripción; si no se reconoce, el de su categoría. */
const PRODUCTOS: { clave: RegExp; icono: string }[] = [
  { clave: /destornill/, icono: 'bi-screwdriver' },
  { clave: /alicate|llave/, icono: 'bi-wrench-adjustable' },
  { clave: /martill/, icono: 'bi-hammer' },
  { clave: /taladr|amolad|sierra/, icono: 'bi-gear-wide-connected' },
  { clave: /cinta|tefl/, icono: 'bi-record-circle' },
  { clave: /tubo|codo|llave de paso/, icono: 'bi-droplet-half' },
  { clave: /thinner|solvente/, icono: 'bi-droplet' },
  { clave: /pintur|esmalte|latex/, icono: 'bi-paint-bucket' },
  { clave: /brocha|rodillo/, icono: 'bi-brush' },
  { clave: /tornill|tarugo|perno|clavo/, icono: 'bi-nut' },
];

export function iconoProducto(descripcion: string, categoria: string): string {
  const texto = normalizar(descripcion);
  return PRODUCTOS.find((r) => r.clave.test(texto))?.icono ?? estiloCategoria(categoria).icono;
}
