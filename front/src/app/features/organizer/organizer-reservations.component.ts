import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

import type { EventDto, ReservationDto } from '../../core/api/api.models';
import { readApiError } from '../../core/api/error.util';
import { EventService } from '../../core/api/event.service';
import { ReservationService } from '../../core/api/reservation.service';
import { reservationStatusClass, reservationStatusLabel } from '../../shared/reservation-helpers';

@Component({
  selector: 'app-organizer-reservations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './organizer-reservations.component.html',
})
export class OrganizerReservationsComponent implements OnInit {
  private readonly reservationsApi = inject(ReservationService);
  private readonly eventsApi = inject(EventService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<ReservationDto[]>([]);
  readonly eventMap = signal(new Map<number, EventDto>());

  readonly statusLabel = reservationStatusLabel;
  readonly statusClass = reservationStatusClass;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.eventsApi.listMine().subscribe({
      next: (events) => {
        const m = new Map<number, EventDto>();
        for (const e of events) {
          m.set(e.id, e);
        }
        this.eventMap.set(m);
        this.reservationsApi.listSent().subscribe({
          next: (list) => {
            this.rows.set(list);
            this.loading.set(false);
          },
          error: (err) => {
            this.error.set(readApiError(err));
            this.loading.set(false);
          },
        });
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  eventLabel(eventId: number): string {
    const e = this.eventMap().get(eventId);
    return e ? `${e.eventType} (${e.eventDate})` : `#${eventId}`;
  }
}
