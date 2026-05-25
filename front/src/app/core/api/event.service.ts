import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { EventDto, UpsertEventPayload } from './api.models';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/events`;

  listMine(): Observable<EventDto[]> {
    return this.http.get<EventDto[]>(this.base);
  }

  getById(id: number): Observable<EventDto> {
    return this.http.get<EventDto>(`${this.base}/${id}`);
  }

  create(payload: UpsertEventPayload): Observable<EventDto> {
    return this.http.post<EventDto>(this.base, payload);
  }

  update(id: number, payload: UpsertEventPayload): Observable<EventDto> {
    return this.http.put<EventDto>(`${this.base}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  uploadPhoto(eventId: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<EventDto>(`${this.base}/${eventId}/photo`, formData)
      .pipe(map((event) => event.photoUrl ?? ''));
  }
}
