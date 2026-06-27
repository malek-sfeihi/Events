import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import type { ChatRequestPayload, ChatResponseDto } from './api.models';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/chat`;

  send(payload: ChatRequestPayload): Observable<ChatResponseDto> {
    return this.http.post<ChatResponseDto>(this.base, payload);
  }
}