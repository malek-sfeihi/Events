export const EVENT_TYPES = [
  'Mariage',
  'Séminaire',
  'Anniversaire',
  'Concert',
  'Conférence',
  'Gala',
  "Soirée d'entreprise",
  'Baptême',
  'Autre',
] as const;

export type EventType = (typeof EVENT_TYPES)[number];