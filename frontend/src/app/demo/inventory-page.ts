import { Component } from '@angular/core';
const MATERIALS = [
  { name: 'Cable H07Z1-K 2,5 mm²', sku: 'CAB-25-AZ', stock: 84, min: 30, unit: 'm' },
  { name: 'Magnetotérmico 16A', sku: 'MAG-C16', stock: 6, min: 8, unit: 'ud.' },
  { name: 'Mecanismo Schuko blanco', sku: 'MEC-SCH-W', stock: 24, min: 10, unit: 'ud.' },
  { name: 'Tubo corrugado 20 mm', sku: 'TUB-20', stock: 12, min: 25, unit: 'm' },
  { name: 'Downlight LED 18W', sku: 'LED-DL18', stock: 15, min: 6, unit: 'ud.' },
];
@Component({
  selector: 'app-inventory-page',
  templateUrl: './inventory-page.html',
  styleUrl: './inventory-page.css',
})
export class InventoryPage {
  protected readonly materials = MATERIALS;
  protected low(stock: number, min: number): boolean {
    return stock < min;
  }
  protected stockLabel(stock: number, min: number): string {
    return this.low(stock, min) ? 'Stock bajo' : 'Disponible';
  }
}
