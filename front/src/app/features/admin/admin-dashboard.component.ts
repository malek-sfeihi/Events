import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import type { AdminPendingProviderDto, AdminStatsDto, AdminUserSummaryDto } from '../../core/api/api.models';
import { readApiError } from '../../core/api/error.util';
import { AdminService } from '../../core/api/admin.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly stats = signal<AdminStatsDto | null>(null);
  readonly pending = signal<AdminPendingProviderDto[]>([]);
  readonly users = signal<AdminUserSummaryDto[]>([]);
  readonly approvingId = signal<number | null>(null);
  readonly togglingUserId = signal<number | null>(null);
  readonly deletingUserId = signal<number | null>(null);
  readonly enableSaving = signal(false);
  readonly enableError = signal<string | null>(null);
  readonly enableSuccess = signal<string | null>(null);

  readonly enableForm = this.fb.nonNullable.group({
    userId: ['', [Validators.required, Validators.pattern(/^[1-9]\d*$/)]],
    enabled: [true],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      stats: this.admin.stats(),
      pending: this.admin.pendingProviders(),
      users: this.admin.listUsers(),
    }).subscribe({
      next: ({ stats, pending, users }) => {
        this.stats.set(stats);
        this.pending.set(pending);
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.loading.set(false);
      },
    });
  }

  isSelf(userId: number): boolean {
    return this.auth.currentUserId() === userId;
  }

  canDelete(u: AdminUserSummaryDto): boolean {
    return !this.isSelf(u.id) && (u.role === 'ORGANISATEUR' || u.role === 'PRESTATAIRE');
  }

  roleLabel(role: string): string {
    switch (role) {
      case 'ORGANISATEUR':
        return 'Organisateur';
      case 'PRESTATAIRE':
        return 'Prestataire';
      case 'ADMIN':
        return 'Admin';
      default:
        return role;
    }
  }

  approve(providerUserId: number): void {
    this.approvingId.set(providerUserId);
    this.admin.approveProvider(providerUserId).subscribe({
      next: () => {
        this.approvingId.set(null);
        this.reload();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.approvingId.set(null);
      },
    });
  }

  toggleAccount(u: AdminUserSummaryDto, enabled: boolean): void {
    if (this.isSelf(u.id)) {
      return;
    }
    if (!enabled && !confirm(`Désactiver le compte ${u.email} ?`)) {
      return;
    }
    this.togglingUserId.set(u.id);
    this.admin.setUserEnabled(u.id, enabled).subscribe({
      next: () => {
        this.togglingUserId.set(null);
        this.reload();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.togglingUserId.set(null);
      },
    });
  }

  deleteAccount(u: AdminUserSummaryDto): void {
    if (!this.canDelete(u)) {
      return;
    }
    const role = this.roleLabel(u.role).toLowerCase();
    if (
      !confirm(
        `Supprimer définitivement le compte ${u.email} (${role}) ?\n\nÉvénements, réservations et profil prestataire associés seront également supprimés. Cette action est irréversible.`,
      )
    ) {
      return;
    }
    this.deletingUserId.set(u.id);
    this.admin.deleteUser(u.id).subscribe({
      next: () => {
        this.deletingUserId.set(null);
        this.reload();
      },
      error: (err) => {
        this.error.set(readApiError(err));
        this.deletingUserId.set(null);
      },
    });
  }

  submitEnable(): void {
    if (this.enableForm.invalid) {
      this.enableForm.markAllAsTouched();
      return;
    }
    const uid = Number(this.enableForm.controls.userId.value);
    this.enableSaving.set(true);
    this.enableError.set(null);
    this.enableSuccess.set(null);
    this.admin.setUserEnabled(uid, this.enableForm.controls.enabled.value).subscribe({
      next: () => {
        this.enableSaving.set(false);
        this.enableSuccess.set('Statut du compte mis à jour.');
        this.reload();
      },
      error: (err) => {
        this.enableError.set(readApiError(err));
        this.enableSaving.set(false);
      },
    });
  }
}
