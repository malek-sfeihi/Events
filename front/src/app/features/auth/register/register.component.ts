import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import type { Role } from '../../../core/auth/auth.types';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly error = signal<string | null>(null);
  private photoFile: File | null = null;

  readonly roles: { value: Role; label: string }[] = [
    { value: 'ORGANISATEUR', label: 'Organisateur d\'événements' },
    { value: 'PRESTATAIRE',  label: 'Prestataire de services' },
  ];

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: ['ORGANISATEUR' as Role, Validators.required],
  });

  photoError = '';

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.photoError = '';
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.photoError = 'La photo ne doit pas dépasser 5 Mo.';
        input.value = '';
        return;
      }
      if (!file.type.startsWith('image/')) {
        this.photoError = 'Seules les images sont acceptées.';
        input.value = '';
        return;
      }
    }
    this.photoFile = file;
  }

  submit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.auth
      .register({
        fullName: v.fullName,
        email: v.email,
        password: v.password,
        role: v.role,
      })
      .pipe(switchMap(() => (this.photoFile ? this.auth.uploadMyPhoto(this.photoFile) : of(''))))
      .subscribe({
        next: () => {
          this.auth.navigateAfterLogin();
        },
        error: (err) => {
          const msg = err?.error?.error ?? 'Inscription impossible.';
          this.error.set(typeof msg === 'string' ? msg : 'Inscription impossible.');
        },
      });
  }
}
