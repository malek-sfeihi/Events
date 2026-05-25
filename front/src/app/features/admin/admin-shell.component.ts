import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { HeaderBarComponent } from '../../shared/header-bar/header-bar.component';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, HeaderBarComponent],
  templateUrl: './admin-shell.component.html',
})
export class AdminShellComponent {}
