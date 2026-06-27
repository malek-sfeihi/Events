export interface EventDto {
  id: number;
  eventType: string;
  eventDate: string;
  participantCount: number;
  budget: number;
  preferences: string | null;
  photoUrl: string | null;
}

export interface UpsertEventPayload {
  eventType: string;
  eventDate: string;
  participantCount: number;
  budget: number;
  preferences?: string | null;
}

export type ReservationStatus = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface ReservationDto {
  id: number;
  eventId: number;
  organizerUserId: number;
  providerUserId: number;
  status: ReservationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReservationPayload {
  eventId: number;
  providerUserId: number;
}

export interface RecommendationScoreDto {
  providerUserId: number;
  businessName: string;
  compatibilityScore: number;
  acceptanceProbability: number;
  explanation: string;
}

export interface ProviderCatalogItem {
  providerUserId: number;
  businessName: string;
  minCapacity: number;
  maxCapacity: number;
  acceptedEventTypes: string[];
  minimumPrice: number;
  availabilityNotes: string | null;
  logoUrl: string | null;
}

export interface ProviderProfileDto {
  id: number;
  businessName: string;
  minCapacity: number;
  maxCapacity: number;
  acceptedEventTypes: string[];
  minimumPrice: number;
  availabilityNotes: string | null;
  approved: boolean;
  logoUrl: string | null;
}

export interface UpsertProviderProfilePayload {
  businessName: string;
  minCapacity: number;
  maxCapacity: number;
  acceptedEventTypes: string[];
  minimumPrice: number;
  availabilityNotes?: string | null;
}

export type UserRole = 'ORGANISATEUR' | 'PRESTATAIRE' | 'ADMIN';

export interface AdminUserSummaryDto {
  id: number;
  email: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
}

export interface AdminStatsDto {
  organizerCount: number;
  prestataireCount: number;
  adminCount: number;
  totalUsers: number;
  eventCount: number;
  reservationCount: number;
  reservationPendingCount: number;
  reservationAcceptedCount: number;
  reservationRejectedCount: number;
  pendingProviderProfilesCount: number;
}

export interface AdminPendingProviderDto {
  providerUserId: number;
  email: string;
  fullName: string;
  profile: ProviderProfileDto;
}

export interface ChatHistoryItem {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequestPayload {
  message: string;
  history: ChatHistoryItem[];
}

export interface ChatResponseDto {
  reply: string;
}
