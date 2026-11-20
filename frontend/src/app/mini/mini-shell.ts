import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MiniStoreService } from './mini-store.service';

@Component({
  selector: 'app-mini-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './mini-shell.html',
  styleUrl: './mini-shell.css',
})
export class MiniShell implements OnInit {
  protected readonly store = inject(MiniStoreService);
  ngOnInit(): void {
    void this.store.load();
  }
}
