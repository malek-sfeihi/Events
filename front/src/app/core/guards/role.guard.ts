import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import type { Role } from '../auth/auth.types';
import { AuthService } from '../auth/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowed = route.data['roles'] as Role[] | undefined;
  if (!allowed?.length) {
    return true;
  }
  const r = auth.currentRole();
  if (r && allowed.includes(r)) {
    return true;
  }
  void router.navigate(['/']);
  return false;
};
