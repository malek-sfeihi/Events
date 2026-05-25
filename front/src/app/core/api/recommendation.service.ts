import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { RecommendationScoreDto } from './api.models';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/recommendations`;

  /** Classement et scores pour les prestataires éligibles par rapport à l'événement choisi. */
  listForEvent(eventId: number): Observable<RecommendationScoreDto[]> {
    return this.http.get<RecommendationScoreDto[]>(`${this.base}/events/${eventId}`);
  }
}
