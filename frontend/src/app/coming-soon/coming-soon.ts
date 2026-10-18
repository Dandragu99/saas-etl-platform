import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

interface ComingSoonContent {
  readonly title: string;
  readonly description: string;
}

@Component({
  selector: 'app-coming-soon',
  imports: [RouterLink],
  templateUrl: './coming-soon.html',
  styleUrl: './coming-soon.css',
})
export class ComingSoon {
  private readonly route = inject(ActivatedRoute);

  protected readonly content = toSignal(
    this.route.data.pipe(
      map((data): ComingSoonContent => ({
        title: String(data['moduleTitle'] ?? 'Nuevo módulo'),
        description: String(data['moduleDescription'] ?? 'Esta funcionalidad está en preparación.'),
      })),
    ),
    {
      initialValue: {
        title: 'Nuevo módulo',
        description: 'Esta funcionalidad está en preparación.',
      },
    },
  );
}
