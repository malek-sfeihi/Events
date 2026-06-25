import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { ProviderProfileDto, UpsertProviderProfilePayload } from './api.models';

@Injectable({ providedIn: 'root' })
export class ProviderProfileService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/providers/me`;

  getMine(): Observable<ProviderProfileDto> {
    return this.http.get<ProviderProfileDto>(this.base);
  }

  upsert(payload: UpsertProviderProfilePayload): Observable<ProviderProfileDto> {
    return this.http.put<ProviderProfileDto>(this.base, payload);
  }

  uploadLogo(file: File): Observable<ProviderProfileDto> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ProviderProfileDto>(`${this.base}/logo`, fd);
  }

  deleteMine(): Observable<void> {
    return this.http.delete<void>(this.base);
  }
}
