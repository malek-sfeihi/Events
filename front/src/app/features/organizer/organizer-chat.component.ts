import { CommonModule } from '@angular/common';
import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import type { ChatHistoryItem, ChatResponseDto } from '../../core/api/api.models';
import { ChatService } from '../../core/api/chat.service';
import { readApiError } from '../../core/api/error.util';

@Component({
  selector: 'app-organizer-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './organizer-chat.component.html',
})
export class OrganizerChatComponent {
  private readonly chatApi = inject(ChatService);

  @ViewChild('messagesEnd') private messagesEnd?: ElementRef;

  readonly messages = signal<ChatHistoryItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  inputText = '';

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.loading()) return;

    const history = [...this.messages()];
    this.messages.update((m) => [...m, { role: 'user', content: text }]);
    this.inputText = '';
    this.loading.set(true);
    this.error.set(null);

    this.chatApi.send({ message: text, history }).subscribe({
      next: (res: ChatResponseDto) => {
        this.messages.update((m) => [...m, { role: 'assistant', content: res.reply }]);
        this.loading.set(false);
        this.scrollToBottom();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' });
    }, 50);
  }
}