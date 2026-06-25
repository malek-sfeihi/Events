import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import type { EventDto, ProviderCatalogItem, ReservationDto } from '../../core/api/api.models';
import { readApiError } from '../../core/api/error.util';
import { CatalogService } from '../../core/api/catalog.service';
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
  private readonly catalogApi = inject(CatalogService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<ReservationDto[]>([]);
  readonly eventMap = signal(new Map<number, EventDto>());
  readonly providerMap = signal(new Map<number, string>());

  readonly statusLabel = reservationStatusLabel;
  readonly statusClass = reservationStatusClass;

  readonly pendingCount = computed(() => this.rows().filter(r => r.status === 'EN_ATTENTE').length);
  readonly acceptedCount = computed(() => this.rows().filter(r => r.status === 'ACCEPTEE').length);
  readonly rejectedCount = computed(() => this.rows().filter(r => r.status === 'REFUSEE').length);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      events: this.eventsApi.listMine(),
      reservations: this.reservationsApi.listSent(),
      providers: this.catalogApi.listProviders(),
    }).subscribe({
      next: ({ events, reservations, providers }) => {
        const em = new Map<number, EventDto>();
        for (const e of events) em.set(e.id, e);
        this.eventMap.set(em);

        const pm = new Map<number, string>();
        for (const p of providers) pm.set(p.providerUserId, p.businessName);
        this.providerMap.set(pm);

        this.rows.set(reservations);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  eventLabel(eventId: number): string {
    const e = this.eventMap().get(eventId);
    return e ? e.eventType : `#${eventId}`;
  }

  eventDate(eventId: number): string {
    const e = this.eventMap().get(eventId);
    return e ? e.eventDate : '';
  }

  providerName(providerUserId: number): string {
    return this.providerMap().get(providerUserId) ?? `Prestataire #${providerUserId}`;
  }
}
