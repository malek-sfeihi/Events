import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authUrlPrefix = `${environment.apiBaseUrl}/api/auth/`;
  if (req.url.startsWith(authUrlPrefix)) {
    return next(req);
  }
  const auth = inject(AuthService);
  const t = auth.token();
  if (!t) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${t}` } }));
};
