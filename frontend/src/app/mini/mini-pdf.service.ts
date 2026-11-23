import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Directory, Filesystem } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
import { MiniQuote, MiniSettings } from './mini.models';

@Injectable({ providedIn: 'root' })
export class MiniPdfService {
  async download(quote: MiniQuote, settings: MiniSettings): Promise<void> {
    if (Capacitor.isNativePlatform()) {
      const uri = await this.writeNativePdf(quote, settings, Directory.Documents);
      await Share.share({
        title: quote.number,
        text: 'Presupuesto de demostración de Voltia Mini',
        url: uri,
        dialogTitle: 'Abrir o guardar PDF',
      });
      return;
    }

    const url = URL.createObjectURL(this.create(quote, settings));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${quote.number}.pdf`;
    anchor.hidden = true;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 30_000);
  }

  async share(quote: MiniQuote, settings: MiniSettings): Promise<void> {
    if (Capacitor.isNativePlatform()) {
      const uri = await this.writeNativePdf(quote, settings, Directory.Cache);
      await Share.share({
        title: quote.number,
        text: 'Presupuesto de demostración de Voltia Mini',
        url: uri,
        dialogTitle: 'Compartir presupuesto',
      });
      return;
    }

    const file = new File([this.create(quote, settings)], `${quote.number}.pdf`, {
      type: 'application/pdf',
    });
    if (navigator.canShare?.({ files: [file] })) {
      try {
        await navigator.share({ title: quote.number, files: [file] });
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return;
        throw error;
      }
    } else {
      await this.download(quote, settings);
    }
  }

  create(quote: MiniQuote, settings: MiniSettings): Blob {
    const lines = [
      settings.tradeName.toUpperCase(),
      `NIF: ${settings.taxId}`,
      settings.address,
      `${settings.phone}  ${settings.email}`,
      '',
      `PRESUPUESTO ${quote.number}`,
      `Fecha: ${quote.date}`,
      `Cliente: ${quote.clientName}`,
      '',
      'CONCEPTOS',
      ...quote.lines.map(
        (line) => `${line.description}  ${line.quantity} x ${line.unitPrice.toFixed(2)} EUR`,
      ),
      '',
      `Subtotal: ${quote.subtotal.toFixed(2)} EUR`,
      `IVA (${quote.taxRate}%): ${quote.tax.toFixed(2)} EUR`,
      `TOTAL: ${quote.total.toFixed(2)} EUR`,
      '',
      'DOCUMENTO DE DEMOSTRACION - NO VALIDO PARA EMISION FISCAL',
    ];
    const stream = lines
      .map(
        (line, index) =>
          `BT /F1 ${index === 0 ? 18 : 10} Tf 50 ${790 - index * 22} Td (${this.escape(line)}) Tj ET`,
      )
      .join('\n');
    const objects = [
      '<< /Type /Catalog /Pages 2 0 R >>',
      '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
      '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
      `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`,
      '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    ];
    let body = '%PDF-1.4\n';
    const offsets: number[] = [0];
    objects.forEach((object, index) => {
      offsets.push(body.length);
      body += `${index + 1} 0 obj\n${object}\nendobj\n`;
    });
    const xref = body.length;
    body += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
    body += offsets
      .slice(1)
      .map((offset) => `${offset.toString().padStart(10, '0')} 00000 n \n`)
      .join('');
    body += `trailer << /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
    return new Blob([body], { type: 'application/pdf' });
  }

  private escape(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^\x20-\x7E]/g, '')
      .replace(/[()\\]/g, '\\$&');
  }

  private async writeNativePdf(
    quote: MiniQuote,
    settings: MiniSettings,
    directory: Directory,
  ): Promise<string> {
    const data = await this.toBase64(this.create(quote, settings));
    const result = await Filesystem.writeFile({
      path: `${quote.number}.pdf`,
      data,
      directory,
      recursive: true,
    });
    return result.uri;
  }

  private async toBase64(blob: Blob): Promise<string> {
    const bytes = new Uint8Array(await blob.arrayBuffer());
    const chunks: string[] = [];
    const chunkSize = 8192;
    for (let index = 0; index < bytes.length; index += chunkSize) {
      chunks.push(String.fromCharCode(...bytes.subarray(index, index + chunkSize)));
    }
    return btoa(chunks.join(''));
  }
}
