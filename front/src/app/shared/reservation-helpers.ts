import type { ReservationStatus } from '../core/api/api.models';

export function reservationStatusLabel(status: ReservationStatus): string {
  switch (status) {
    case 'EN_ATTENTE':
      return 'En attente';
    case 'ACCEPTEE':
      return 'Acceptée';
    case 'REFUSEE':
      return 'Refusée';
    default:
      return status;
  }
}

export function reservationStatusClass(status: ReservationStatus): string {
  switch (status) {
    case 'EN_ATTENTE':
      return 'badge-warn';
    case 'ACCEPTEE':
      return 'badge-ok';
    case 'REFUSEE':
      return 'badge-bad';
    default:
      return '';
  }
}
