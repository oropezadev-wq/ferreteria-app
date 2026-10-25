import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rol } from '../models/modelos';
import { AuthService } from './auth.service';

/** Sin sesión, manda al login; con un rol sin permiso, manda al panel. */
export const authGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.usuario()) {
    return router.createUrlTree(['/login']);
  }
  const roles = route.data['roles'] as Rol[] | undefined;
  if (roles && !auth.tieneRol(roles)) {
    return router.createUrlTree(['/panel']);
  }
  return true;
};
