import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { EVENT_TYPES } from '../../core/constants/event-types';
import { readApiError } from '../../core/api/error.util';
import { ProviderProfileService } from '../../core/api/provider-profile.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-provider-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './provider-profile.component.html',
})
export class ProviderProfileComponent implements OnInit {
  private readonly api = inject(ProviderProfileService);
  private readonly fb = inject(FormBuilder);

  readonly EVENT_TYPES = EVENT_TYPES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly hasExistingProfile = signal(false);
  readonly approved = signal(false);
  readonly selectedTypes = signal<Set<string>>(new Set());
  readonly currentLogoUrl = signal<string | null>(null);
  readonly logoPreview = signal<string | null>(null);
  readonly logoError = signal<string | null>(null);

  private logoFile: File | null = null;

  readonly form = this.fb.nonNullable.group({
    businessName: ['', [Validators.required, Validators.minLength(2)]],
    minCapacity: [10, [Validators.required, Validators.min(1)]],
    maxCapacity: [200, [Validators.required, Validators.min(1)]],
    minimumPrice: [500, [Validators.required, Validators.min(0.01)]],
    availabilityNotes: [''],
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getMine().subscribe({
      next: (p) => {
        this.hasExistingProfile.set(true);
        this.approved.set(p.approved);
        this.selectedTypes.set(new Set(p.acceptedEventTypes));
        this.currentLogoUrl.set(p.logoUrl ?? null);
        this.form.patchValue({
          businessName: p.businessName,
          minCapacity: p.minCapacity,
          maxCapacity: p.maxCapacity,
          minimumPrice: p.minimumPrice,
          availabilityNotes: p.availabilityNotes ?? '',
        });
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        if (err.status !== 404) {
          this.error.set(readApiError(err));
        }
        this.loading.set(false);
      },
    });
  }

  isTypeSelected(type: string): boolean {
    return this.selectedTypes().has(type);
  }

  toggleType(type: string): void {
    const s = new Set(this.selectedTypes());
    if (s.has(type)) {
      s.delete(type);
    } else {
      s.add(type);
    }
    this.selectedTypes.set(s);
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.logoError.set(null);
    if (!file) {
      this.logoFile = null;
      this.logoPreview.set(null);
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.logoError.set('Le logo ne doit pas dépasser 5 Mo.');
      input.value = '';
      return;
    }
    this.logoFile = file;
    const reader = new FileReader();
    reader.onload = (e) => this.logoPreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  logoSrc(url: string): string {
    return url.startsWith('http') ? url : `${environment.apiBaseUrl}${url}`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.minCapacity > v.maxCapacity) {
      this.error.set('La capacité minimale ne peut pas dépasser la maximale.');
      return;
    }
    if (this.selectedTypes().size === 0) {
      this.error.set('Sélectionnez au moins un type d\'événement.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api
      .upsert({
        businessName: v.businessName.trim(),
        minCapacity: v.minCapacity,
        maxCapacity: v.maxCapacity,
        acceptedEventTypes: [...this.selectedTypes()],
        minimumPrice: v.minimumPrice,
        availabilityNotes: v.availabilityNotes?.trim() ? v.availabilityNotes.trim() : null,
      })
      .subscribe({
        next: (p) => {
          this.hasExistingProfile.set(true);
          this.approved.set(p.approved);
          if (this.logoFile) {
            this.api.uploadLogo(this.logoFile).subscribe({
              next: (updated) => {
                this.currentLogoUrl.set(updated.logoUrl ?? null);
                this.logoFile = null;
                this.logoPreview.set(null);
                this.success.set('Profil et logo enregistrés.');
                this.saving.set(false);
              },
              error: (err) => {
                this.error.set('Profil enregistré, mais erreur logo : ' + readApiError(err));
                this.saving.set(false);
              },
            });
          } else {
            this.success.set('Profil enregistré.');
            this.saving.set(false);
          }
        },
        error: (err) => {
          this.error.set(readApiError(err));
          this.saving.set(false);
        },
      });
  }

  deleteProfile(): void {
    if (!confirm('Supprimer définitivement votre profil prestataire ?')) {
      return;
    }
    this.error.set(null);
    this.api.deleteMine().subscribe({
      next: () => {
        this.hasExistingProfile.set(false);
        this.approved.set(false);
        this.selectedTypes.set(new Set());
        this.currentLogoUrl.set(null);
        this.logoPreview.set(null);
        this.logoFile = null;
        this.form.reset({
          businessName: '',
          minCapacity: 10,
          maxCapacity: 200,
          minimumPrice: 500,
          availabilityNotes: '',
        });
        this.success.set('Profil supprimé.');
      },
      error: (err) => this.error.set(readApiError(err)),
    });
  }
}