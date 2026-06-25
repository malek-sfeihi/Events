import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import type { EventDto, ProviderCatalogItem } from '../../core/api/api.models';
import { EVENT_TYPES } from '../../core/constants/event-types';
import { readApiError } from '../../core/api/error.util';
import { CatalogService } from '../../core/api/catalog.service';
import { EventService } from '../../core/api/event.service';
import { ReservationService } from '../../core/api/reservation.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-organizer-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organizer-catalog.component.html',
})
export class OrganizerCatalogComponent implements OnInit {
  private readonly catalogApi = inject(CatalogService);
  private readonly eventsApi = inject(EventService);
  private readonly reservationsApi = inject(ReservationService);

  readonly EVENT_TYPES = EVENT_TYPES;

  private readonly coverUrls = [
    '/images/landing/cover-1.jpg',
    '/images/landing/cover-2.jpg',
    '/images/landing/cover-3.jpg',
    '/images/landing/Patapain traiteur.jpg',
    '/images/landing/cover-5.jpg',
    '/images/landing/cover-6.jpg',
  ];

  readonly loading = signal(false);
  readonly booking = signal(false);
  readonly error = signal<string | null>(null);
  readonly providers = signal<ProviderCatalogItem[]>([]);
  readonly events = signal<EventDto[]>([]);

  filterType = '';
  bookTarget = signal<ProviderCatalogItem | null>(null);
  selectedEventId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadCatalog();
    this.eventsApi.listMine().subscribe({
      next: (list) => this.events.set(list),
      error: () => this.events.set([]),
    });
  }

  loadCatalog(): void {
    this.loading.set(true);
    this.error.set(null);
    this.catalogApi.listProviders(this.filterType || undefined).subscribe({
      next: (list) => {
        this.providers.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  applyFilter(): void {
    this.loadCatalog();
  }

  openBook(p: ProviderCatalogItem): void {
    this.bookTarget.set(p);
    const evs = this.events();
    this.selectedEventId.set(evs.length === 1 ? evs[0].id : null);
    this.error.set(null);
  }

  closeBook(): void {
    this.bookTarget.set(null);
    this.selectedEventId.set(null);
  }

  coverUrl(p: ProviderCatalogItem): string {
    if (p.logoUrl) {
      return p.logoUrl.startsWith('http') ? p.logoUrl : `${environment.apiBaseUrl}${p.logoUrl}`;
    }
    const i = Math.abs(p.providerUserId) % this.coverUrls.length;
    return this.coverUrls[i];
  }

  confirmBook(): void {
    const p = this.bookTarget();
    const eventId = this.selectedEventId();
    if (!p || eventId === null) {
      this.error.set('Choisissez un événement.');
      return;
    }
    this.booking.set(true);
    this.error.set(null);
    this.reservationsApi
      .create({ eventId, providerUserId: p.providerUserId })
      .subscribe({
        next: () => {
          this.booking.set(false);
          this.closeBook();
        },
        error: (err) => {
          this.error.set(readApiError(err));
          this.booking.set(false);
        },
      });
  }
}
