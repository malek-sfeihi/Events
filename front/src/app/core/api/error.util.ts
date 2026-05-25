/** Message lisible pour les erreurs HttpClient (réseau, 403, corps JSON Spring, etc.). */
export function readApiError(err: unknown, fallback = 'Une erreur est survenue.'): string {
  const o = err as {
    error?: { error?: string; message?: string } | string;
    message?: string;
    status?: number;
    url?: string;
  };

  if (o?.status === 0) {
    return 'Connexion au serveur impossible. Vérifiez que le backend tourne (ex. http://localhost:8080) et que l’URL dans environment correspond.';
  }
  if (o?.status === 403) {
    return 'Accès refusé (403). Déconnexion puis reconnexion avec un compte ayant le bon rôle.';
  }

  if (typeof o?.error === 'object' && o.error && typeof o.error.error === 'string') {
    return o.error.error;
  }
  if (typeof o?.error === 'object' && o.error && typeof o.error.message === 'string') {
    return o.error.message;
  }
  if (typeof o?.error === 'string') {
    return o.error;
  }
  return fallback;
}
