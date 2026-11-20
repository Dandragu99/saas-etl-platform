import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { miniMoney } from './mini.models';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-home',
  imports: [RouterLink],
  templateUrl: './mini-home.html',
  styleUrl: './mini-home.css',
})
export class MiniHome {
  protected readonly store = inject(MiniStoreService);
  protected readonly money = miniMoney;
}
