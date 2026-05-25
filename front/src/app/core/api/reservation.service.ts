import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { CreateReservationPayload, ReservationDto } from './api.models';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/reservations`;

  create(payload: CreateReservationPayload): Observable<ReservationDto> {
    return this.http.post<ReservationDto>(this.base, payload);
  }

  listSent(): Observable<ReservationDto[]> {
    return this.http.get<ReservationDto[]>(`${this.base}/sent`);
  }

  listReceived(): Observable<ReservationDto[]> {
    return this.http.get<ReservationDto[]>(`${this.base}/received`);
  }

  accept(id: number): Observable<ReservationDto> {
    return this.http.patch<ReservationDto>(`${this.base}/${id}/accept`, {});
  }

  reject(id: number): Observable<ReservationDto> {
    return this.http.patch<ReservationDto>(`${this.base}/${id}/reject`, {});
  }
}
