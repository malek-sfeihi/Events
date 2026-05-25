import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, Role } from './auth.types';

const STORAGE_KEY = 'event_align_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<AuthResponse | null>(this.loadSession());

  readonly sessionReadonly = this.session.asReadonly();
  readonly isLoggedIn = computed(() => !!this.session()?.token);
  readonly currentRole = computed(() => this.session()?.role ?? null);
  readonly currentUserId = computed(() => this.session()?.userId ?? null);

  private loadSession(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
      return null;
    }
  }

  private persist(response: AuthResponse): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    this.session.set(response);
  }

  token(): string | null {
    return this.session()?.token ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/api/auth/login`, { email, password })
      .pipe(tap((r) => this.persist(r)));
  }

  register(payload: {
    fullName: string;
    email: string;
    password: string;
    role: Role;
  }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/api/auth/register`, payload)
      .pipe(tap((r) => this.persist(r)));
  }

  uploadMyPhoto(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<{ photoUrl: string }>(`${environment.apiBaseUrl}/api/users/me/photo`, formData)
      .pipe(
        tap((r) => {
          const current = this.session();
          if (current) {
            this.persist({ ...current, photoUrl: r.photoUrl });
          }
        }),
        map((r) => r.photoUrl),
      );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    void this.router.navigate(['/']);
  }

  navigateAfterLogin(): void {
    const role = this.session()?.role;
    switch (role) {
      case 'ORGANISATEUR':
        void this.router.navigate(['/organizer', 'events']);
        break;
      case 'PRESTATAIRE':
        void this.router.navigate(['/provider', 'profile']);
        break;
      case 'ADMIN':
        void this.router.navigate(['/admin']);
        break;
      default:
        void this.router.navigate(['/']);
    }
  }
}
