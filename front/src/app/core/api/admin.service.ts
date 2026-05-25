import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type {
  AdminPendingProviderDto,
  AdminStatsDto,
  AdminUserSummaryDto,
  ProviderProfileDto,
} from './api.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/admin`;

  stats(): Observable<AdminStatsDto> {
    return this.http.get<AdminStatsDto>(`${this.base}/stats`);
  }

  pendingProviders(): Observable<AdminPendingProviderDto[]> {
    return this.http.get<AdminPendingProviderDto[]>(`${this.base}/providers/pending`);
  }

  listUsers(): Observable<AdminUserSummaryDto[]> {
    return this.http.get<AdminUserSummaryDto[]>(`${this.base}/users`);
  }

  approveProvider(providerUserId: number): Observable<ProviderProfileDto> {
    return this.http.patch<ProviderProfileDto>(
      `${this.base}/providers/${providerUserId}/approve`,
      {},
    );
  }

  setUserEnabled(userId: number, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.base}/users/${userId}/enabled`, { enabled });
  }

  /** Suppression définitive (organisateur ou prestataire uniquement). */
  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/users/${userId}`);
  }
}
