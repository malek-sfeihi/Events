# Intelligence artificielle / aide à la décision

## Architecture retenue

| Couche | Rôle |
|--------|------|
| **Spring Boot** | Sécurité JWT, accès base PostgreSQL, agrégation des données (événement + profils prestataires + compteurs d’historique des réservations). Expose au frontend `GET /api/recommendations/events/{eventId}`. |
| **FastAPI (Python)** | Moteur de scoring et textes d’explicabilité. Endpoint interne : `POST /api/v1/recommendations/score`. |

Le backend Java **ne recalcule pas** le score lorsque `recommendation.python.enabled=true` : il envoie un JSON au service Python et renvoie la réponse à Angular.

## Emplacement du code

| Composant | Chemin |
|-----------|--------|
| Service FastAPI | `ai/python-service/` (`app/main.py`, `app/scoring.py`) |
| Client HTTP depuis Java | `back/event-backend/.../recommendation/python/PythonRecommendationClient.java` |
| Orchestration + agrégation métier | `.../recommendation/RecommendationServiceImpl.java` |
| Secours sans Python (tests / hors ligne) | `.../recommendation/local/LocalRecommendationScorer.java` — même logique que Python si `recommendation.python.enabled=false` |

Configuration : `back/event-backend/src/main/resources/application.yml` (`recommendation.python.*`).

## Évolution ML (pas à pas)

| Étape | Statut | Contenu |
|-------|--------|---------|
| 1 | OK | Problème : prédire acceptation à partir de l'historique |
| 2 | OK | `python-service/scripts/export_training_data.py` → `data/reservations_training.csv` |
| 3 | À venir | Feature engineering / entraînement |
| 4–6 | À venir | Modèle, intégration `scoring.py`, évaluation |

Voir `python-service/data/README.md` pour lancer l'export.

La logique dans `scoring.py` sera complétée par un modèle **scikit-learn** en conservant le contrat `ScoreRequest` / liste de `ScoreItem`. Spring et Angular restent inchangés.
