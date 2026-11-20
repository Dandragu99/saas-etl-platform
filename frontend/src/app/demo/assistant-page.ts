import { Component, signal } from '@angular/core';

const ANSWERS: Readonly<Record<string, string>> = {
  '¿Qué facturas tengo pendientes?':
    'Tienes 3 facturas sin cobrar por un total de 6.875,22 €. La factura de Restaurante La Brasa está vencida y sería la primera que revisaría.',
  '¿Cuánto he facturado este mes?':
    'Este mes has emitido 8.420 € en facturas, un 12 % más que el mes anterior. De ese importe, 5.907,22 € siguen pendientes de cobro.',
  '¿Qué obras tengo en curso?':
    'Hay 2 obras en curso: Instalación vivienda para Reformas Martínez (68 %) e Iluminación clínica para Clínica Dental Nova (35 %).',
  'Ayúdame a preparar un presupuesto.':
    'Claro. Te recomiendo confirmar cliente, alcance, materiales, horas de mano de obra e IVA. Puedo llevarte al formulario de presupuestos para completarlo.',
};
interface Message {
  from: 'assistant' | 'user';
  text: string;
}
@Component({
  selector: 'app-assistant-page',
  templateUrl: './assistant-page.html',
  styleUrl: './assistant-page.css',
})
export class AssistantPage {
  protected readonly suggestions = Object.keys(ANSWERS);
  protected readonly messages = signal<readonly Message[]>([
    {
      from: 'assistant',
      text: 'Buenos días, David. Soy tu asistente de demostración. Puedo resumir cobros, obras y actividad usando los datos ficticios de esta demo.',
    },
  ]);
  protected ask(question: string): void {
    this.messages.update((items) => [
      ...items,
      { from: 'user', text: question },
      {
        from: 'assistant',
        text:
          ANSWERS[question] ??
          'Esta consulta estará disponible cuando conectemos el asistente con los datos reales del negocio.',
      },
    ]);
  }
  protected submit(input: HTMLInputElement): void {
    const value = input.value.trim();
    if (!value) return;
    this.ask(value);
    input.value = '';
  }
}
