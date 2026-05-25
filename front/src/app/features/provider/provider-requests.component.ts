import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Observable } from 'rxjs';

import type { ReservationDto } from '../../core/api/api.models';
import { readApiError } from '../../core/api/error.util';
import { ReservationService } from '../../core/api/reservation.service';
import { reservationStatusClass, reservationStatusLabel } from '../../shared/reservation-helpers';

@Component({
  selector: 'app-provider-requests',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './provider-requests.component.html',
})
export class ProviderRequestsComponent implements OnInit {
  private readonly reservationsApi = inject(ReservationService);

  readonly loading = signal(false);
  readonly actingId = signal<number | null>(null);
  readonly error = signal<string | null>(null);
  readonly rows = signal<ReservationDto[]>([]);

  readonly statusLabel = reservationStatusLabel;
  readonly statusClass = reservationStatusClass;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.reservationsApi.listReceived().subscribe({
      next: (list) => {
        this.rows.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  accept(id: number): void {
    this.runPatch(id, this.reservationsApi.accept(id));
  }

  reject(id: number): void {
    if (!confirm('Refuser cette demande ?')) {
      return;
    }
    this.runPatch(id, this.reservationsApi.reject(id));
  }

  private runPatch(id: number, req$: Observable<ReservationDto>): void {
    this.actingId.set(id);
    this.error.set(null);
    req$.subscribe({
      next: () => {
        this.actingId.set(null);
        this.reload();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.actingId.set(null);
      },
    });
  }
}
