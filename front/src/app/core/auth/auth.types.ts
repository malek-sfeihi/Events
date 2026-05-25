export type Role = 'ORGANISATEUR' | 'PRESTATAIRE' | 'ADMIN';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  role: Role;
  photoUrl: string | null;
}
