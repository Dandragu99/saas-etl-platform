import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DEMO_WORKS, money } from '../demo/demo-data';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  protected readonly works = DEMO_WORKS.slice(0, 3);
  protected readonly money = money;
}
