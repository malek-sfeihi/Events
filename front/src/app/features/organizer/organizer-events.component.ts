import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { of, switchMap } from 'rxjs';

import type { EventDto } from '../../core/api/api.models';
import { EVENT_TYPES } from '../../core/constants/event-types';
import { readApiError } from '../../core/api/error.util';
import { EventService } from '../../core/api/event.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-organizer-events',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './organizer-events.component.html',
})
export class OrganizerEventsComponent implements OnInit {
  private readonly eventsApi = inject(EventService);
  private readonly fb = inject(FormBuilder);

  readonly EVENT_TYPES = EVENT_TYPES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<EventDto[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly photoError = signal<string | null>(null);
  private eventPhotoFile: File | null = null;

  readonly today = new Date().toISOString().slice(0, 10);

  readonly form = this.fb.nonNullable.group({
    eventType: ['', Validators.required],
    eventDate: ['', Validators.required],
    participantCount: [10, [Validators.required, Validators.min(1)]],
    budget: [1000, [Validators.required, Validators.min(0.01)]],
    preferences: [''],
  });

  onEventPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.photoError.set(null);
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.photoError.set('La photo ne doit pas dépasser 5 Mo.');
        input.value = '';
        return;
      }
    }
    this.eventPhotoFile = file;
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  photoSrc(path: string | null): string | null {
    if (!path) {
      return null;
    }
    return path.startsWith('http') ? path : `${environment.apiBaseUrl}${path}`;
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.eventsApi.listMine().subscribe({
      next: (list) => {
        this.events.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  startEdit(e: EventDto): void {
    this.editingId.set(e.id);
    this.form.patchValue({
      eventType: e.eventType,
      eventDate: e.eventDate.slice(0, 10),
      participantCount: e.participantCount,
      budget: e.budget,
      preferences: e.preferences ?? '',
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form.reset({
      eventType: '',
      eventDate: '',
      participantCount: 10,
      budget: 1000,
      preferences: '',
    });
    this.eventPhotoFile = null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const payload = {
      eventType: v.eventType.trim(),
      eventDate: v.eventDate,
      participantCount: v.participantCount,
      budget: v.budget,
      preferences: v.preferences?.trim() ? v.preferences.trim() : null,
    };
    const id = this.editingId();
    this.saving.set(true);
    this.error.set(null);
    const req$ = id !== null ? this.eventsApi.update(id, payload) : this.eventsApi.create(payload);
    req$
      .pipe(
        switchMap((saved) =>
          this.eventPhotoFile ? this.eventsApi.uploadPhoto(saved.id, this.eventPhotoFile) : of(saved.photoUrl ?? ''),
        ),
      )
      .subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.reload();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.saving.set(false);
      },
    });
  }

  delete(e: EventDto): void {
    if (!confirm(`Supprimer l’événement « ${e.eventType} » ?`)) {
      return;
    }
    this.error.set(null);
    this.eventsApi.delete(e.id).subscribe({
      next: () => {
        if (this.editingId() === e.id) {
          this.cancelEdit();
        }
        this.reload();
      },
      error: (err) => this.error.set(readApiError(err)),
    });
  }
}
