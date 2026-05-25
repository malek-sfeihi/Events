import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import type { EventDto, RecommendationScoreDto } from '../../core/api/api.models';
import { readApiError } from '../../core/api/error.util';
import { EventService } from '../../core/api/event.service';
import { RecommendationService } from '../../core/api/recommendation.service';

@Component({
  selector: 'app-organizer-recommendations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organizer-recommendations.component.html',
})
export class OrganizerRecommendationsComponent implements OnInit {
  private readonly eventsApi = inject(EventService);
  private readonly recommendationApi = inject(RecommendationService);

  readonly loadingEvents = signal(false);
  readonly loadingScores = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<EventDto[]>([]);
  readonly scores = signal<RecommendationScoreDto[]>([]);

  selectedEventId: number | null = null;

  ngOnInit(): void {
    this.loadingEvents.set(true);
    this.eventsApi.listMine().subscribe({
      next: (list) => {
        this.events.set(list);
        this.loadingEvents.set(false);
        if (list.length === 1) {
          this.selectedEventId = list[0].id;
          this.loadScores(list[0].id);
        }
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loadingEvents.set(false);
      },
    });
  }

  onEventChange(): void {
    const id = this.selectedEventId;
    if (id === null) {
      this.scores.set([]);
      return;
    }
    this.loadScores(id);
  }

  private loadScores(eventId: number): void {
    this.loadingScores.set(true);
    this.error.set(null);
    this.recommendationApi.listForEvent(eventId).subscribe({
      next: (list) => {
        this.scores.set(list);
        this.loadingScores.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.scores.set([]);
        this.loadingScores.set(false);
      },
    });
  }
}
