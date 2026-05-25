import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'organizer',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ORGANISATEUR'] },
    loadComponent: () =>
      import('./features/organizer/organizer-shell.component').then((m) => m.OrganizerShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'events' },
      {
        path: 'events',
        loadComponent: () =>
          import('./features/organizer/organizer-events.component').then((m) => m.OrganizerEventsComponent),
      },
      {
        path: 'recommendations',
        loadComponent: () =>
          import('./features/organizer/organizer-recommendations.component').then(
            (m) => m.OrganizerRecommendationsComponent,
          ),
      },
      {
        path: 'catalog',
        loadComponent: () =>
          import('./features/organizer/organizer-catalog.component').then((m) => m.OrganizerCatalogComponent),
      },
      {
        path: 'reservations',
        loadComponent: () =>
          import('./features/organizer/organizer-reservations.component').then(
            (m) => m.OrganizerReservationsComponent,
          ),
      },
    ],
  },
  {
    path: 'provider',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PRESTATAIRE'] },
    loadComponent: () =>
      import('./features/provider/provider-shell.component').then((m) => m.ProviderShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/provider/provider-profile.component').then((m) => m.ProviderProfileComponent),
      },
      {
        path: 'requests',
        loadComponent: () =>
          import('./features/provider/provider-requests.component').then((m) => m.ProviderRequestsComponent),
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/admin/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
      },
    ],
  },
  { path: '**', redirectTo: '', pathMatch: 'full' },
];
