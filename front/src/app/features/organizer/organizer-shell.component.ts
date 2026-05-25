import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { HeaderBarComponent } from '../../shared/header-bar/header-bar.component';

@Component({
  selector: 'app-organizer-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, HeaderBarComponent],
  templateUrl: './organizer-shell.component.html',
})
export class OrganizerShellComponent {}
