import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EtlExecutionHistory } from '../execution-history/etl-execution-history';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, EtlExecutionHistory],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {}
