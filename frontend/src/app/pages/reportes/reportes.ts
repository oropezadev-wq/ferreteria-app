import { CurrencyPipe } from '@angular/common';
import { Component } from '@angular/core';

interface FilaRotacion {
  producto: string;
  unidades: number;
}

/** Pantalla de prototipo de reportes (RF7) con cifras de ejemplo. */
@Component({
  selector: 'app-reportes',
  imports: [CurrencyPipe],
  templateUrl: './reportes.html',
})
export class Reportes {
  protected readonly ventasSemana = [
    { dia: 'Lun', monto: 980 },
    { dia: 'Mar', monto: 1240 },
    { dia: 'Mié', monto: 860 },
    { dia: 'Jue', monto: 1410 },
    { dia: 'Vie', monto: 1720 },
    { dia: 'Sáb', monto: 2150 },
  ];
  protected readonly maximo = Math.max(...this.ventasSemana.map((v) => v.monto));

  protected readonly mayorRotacion: FilaRotacion[] = [
    { producto: 'Tornillo drywall 6 x 1" (ciento)', unidades: 86 },
    { producto: 'Cinta teflón 3/4"', unidades: 64 },
    { producto: 'Tubo PVC 1/2" x 5 m', unidades: 41 },
  ];
  protected readonly menorRotacion: FilaRotacion[] = [
    { producto: 'Amoladora angular 4 1/2"', unidades: 1 },
    { producto: 'Taladro percutor 650 W', unidades: 2 },
    { producto: 'Alicate universal 8"', unidades: 3 },
  ];
  protected readonly ventasMes = 28450;
  protected readonly costoMes = 19320;
}
