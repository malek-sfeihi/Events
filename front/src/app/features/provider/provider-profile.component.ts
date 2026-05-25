import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { readApiError } from '../../core/api/error.util';
import { ProviderProfileService } from '../../core/api/provider-profile.service';

function splitTypes(text: string): string[] {
  return text
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

@Component({
  selector: 'app-provider-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './provider-profile.component.html',
})
export class ProviderProfileComponent implements OnInit {
  private readonly api = inject(ProviderProfileService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly hasExistingProfile = signal(false);
  readonly approved = signal(false);

  readonly form = this.fb.nonNullable.group({
    businessName: ['', [Validators.required, Validators.minLength(2)]],
    minCapacity: [10, [Validators.required, Validators.min(1)]],
    maxCapacity: [200, [Validators.required, Validators.min(1)]],
    acceptedEventTypesText: ['Mariage, Séminaire'],
    minimumPrice: [500, [Validators.required, Validators.min(0.01)]],
    availabilityNotes: [''],
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getMine().subscribe({
      next: (p) => {
        this.hasExistingProfile.set(true);
        this.approved.set(p.approved);
        this.form.patchValue({
          businessName: p.businessName,
          minCapacity: p.minCapacity,
          maxCapacity: p.maxCapacity,
          acceptedEventTypesText: p.acceptedEventTypes.join(', '),
          minimumPrice: p.minimumPrice,
          availabilityNotes: p.availabilityNotes ?? '',
        });
        this.loading.set(false);
      },
      error: (err: { status?: number }) => {
        if (err.status === 404) {
          this.hasExistingProfile.set(false);
          this.approved.set(false);
        } else {
          this.error.set(readApiError(err));
        }
        this.loading.set(false);
      },
    });
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
    const types = splitTypes(v.acceptedEventTypesText);
    if (types.length === 0) {
      this.error.set('Indiquez au moins un type d’événement (séparés par des virgules).');
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
        acceptedEventTypes: types,
        minimumPrice: v.minimumPrice,
        availabilityNotes: v.availabilityNotes?.trim() ? v.availabilityNotes.trim() : null,
      })
      .subscribe({
        next: (p) => {
          this.hasExistingProfile.set(true);
          this.approved.set(p.approved);
          this.success.set('Profil enregistré.');
          this.saving.set(false);
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
        this.form.reset({
          businessName: '',
          minCapacity: 10,
          maxCapacity: 200,
          acceptedEventTypesText: '',
          minimumPrice: 500,
          availabilityNotes: '',
        });
        this.success.set('Profil supprimé.');
      },
      error: (err) => this.error.set(readApiError(err)),
    });
  }
}
