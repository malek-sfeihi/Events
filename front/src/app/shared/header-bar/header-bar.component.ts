import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-header-bar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="hb">
      <a routerLink="/" class="hb-brand">align.</a>
      @if (subtitle()) {
        <span class="hb-sub">{{ subtitle() }}</span>
      }
      <div class="hb-actions">
        @if (photoUrl(); as photo) {
          <img class="hb-avatar" [src]="photo" alt="Profil" width="34" height="34" (error)="onPhotoError()" />
        } @else {
          <span class="hb-avatar hb-avatar--fallback">{{ userInitial() }}</span>
        }
        <button type="button" class="hb-link" (click)="logout()">Déconnexion</button>
      </div>
    </header>
  `,
  styles: `
    .hb {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 1.5rem;
      border-bottom: 1px solid var(--line);
      background: color-mix(in srgb, var(--surface) 78%, transparent);
      backdrop-filter: blur(16px) saturate(1.2);
      box-shadow: 0 12px 40px rgba(20, 18, 16, 0.05);
    }
    .hb-brand {
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: -0.03em;
      color: var(--ink);
      text-decoration: none;
    }
    .hb-sub {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.18em;
      color: var(--muted);
    }
    .hb-actions {
      margin-left: auto;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .hb-avatar {
      width: 34px;
      height: 34px;
      border-radius: 999px;
      object-fit: cover;
      border: 1px solid var(--line);
    }
    .hb-avatar--fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--muted);
      background: color-mix(in srgb, var(--line) 70%, transparent);
    }
    .hb-link {
      background: none;
      border: none;
      font: inherit;
      color: var(--accent);
      cursor: pointer;
      padding: 0.35rem 0;
    }
    .hb-link:hover {
      text-decoration: underline;
    }
  `,
})
export class HeaderBarComponent {
  readonly subtitle = input<string>();
  private readonly auth = inject(AuthService);
  private readonly photoFailed = signal(false);

  photoUrl(): string | null {
    if (this.photoFailed()) {
      return null;
    }
    const path = this.auth.sessionReadonly()?.photoUrl;
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return path.startsWith('/') ? `${environment.apiBaseUrl}${path}` : `${environment.apiBaseUrl}/${path}`;
  }

  userInitial(): string {
    const email = this.auth.sessionReadonly()?.email ?? '';
    const first = email.trim().charAt(0).toUpperCase();
    return first || 'U';
  }

  onPhotoError(): void {
    this.photoFailed.set(true);
  }

  logout(): void {
    this.auth.logout();
  }
}
