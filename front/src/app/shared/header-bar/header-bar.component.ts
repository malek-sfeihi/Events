import { Component, inject, input, signal } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-header-bar',
  standalone: true,
  imports: [],
  template: `
    <header class="hb">
      <div class="hb-left">
        @if (subtitle()) {
          <span class="hb-role-icon" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </span>
          <span class="hb-sub">{{ subtitle() }}</span>
        }
      </div>
      <div class="hb-actions">
        @if (photoUrl(); as photo) {
          <img class="hb-avatar" [src]="photo" alt="Profil" width="34" height="34" (error)="onPhotoError()" />
        } @else {
          <span class="hb-avatar hb-avatar--fallback">{{ userInitial() }}</span>
        }
        <span class="hb-email">{{ userEmail() }}</span>
        <button type="button" class="hb-logout" (click)="logout()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          Déconnexion
        </button>
      </div>
    </header>
  `,
  styles: `
    .hb {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.85rem 1.5rem;
      border-bottom: 1px solid var(--line);
      background: var(--surface);
      box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    }
    .hb-left {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .hb-role-icon {
      color: var(--accent);
      display: flex;
      align-items: center;
    }
    .hb-sub {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.14em;
      color: var(--muted);
      font-weight: 600;
    }
    .hb-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      margin-left: auto;
    }
    .hb-email {
      font-size: 0.82rem;
      color: var(--muted);
      display: none;
    }
    @media (min-width: 640px) {
      .hb-email { display: block; }
    }
    .hb-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      object-fit: cover;
      border: 2px solid var(--accent-soft);
      flex-shrink: 0;
    }
    .hb-avatar--fallback {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.78rem;
      font-weight: 700;
      color: var(--accent);
      background: var(--accent-soft);
      flex-shrink: 0;
    }
    .hb-logout {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      background: none;
      border: 1px solid var(--line);
      border-radius: 8px;
      font: inherit;
      font-size: 0.8rem;
      color: var(--muted);
      cursor: pointer;
      padding: 0.35rem 0.75rem;
      transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;
    }
    .hb-logout:hover {
      border-color: var(--danger);
      color: var(--danger);
      background: var(--danger-soft);
    }
  `,
})
export class HeaderBarComponent {
  readonly subtitle = input<string>();
  private readonly auth = inject(AuthService);
  private readonly photoFailed = signal(false);

  photoUrl(): string | null {
    if (this.photoFailed()) return null;
    const path = this.auth.sessionReadonly()?.photoUrl;
    if (!path) return null;
    if (path.startsWith('http')) return path;
    return path.startsWith('/') ? `${environment.apiBaseUrl}${path}` : `${environment.apiBaseUrl}/${path}`;
  }

  userInitial(): string {
    const email = this.auth.sessionReadonly()?.email ?? '';
    return email.trim().charAt(0).toUpperCase() || 'U';
  }

  userEmail(): string {
    return this.auth.sessionReadonly()?.email ?? '';
  }

  onPhotoError(): void {
    this.photoFailed.set(true);
  }

  logout(): void {
    this.auth.logout();
  }
}