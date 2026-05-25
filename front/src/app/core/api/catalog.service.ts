import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { ProviderCatalogItem } from './api.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/catalog/providers`;

  listProviders(eventType?: string): Observable<ProviderCatalogItem[]> {
    let params = new HttpParams();
    if (eventType?.trim()) {
      params = params.set('eventType', eventType.trim());
    }
    return this.http.get<ProviderCatalogItem[]>(this.url, { params });
  }
}
