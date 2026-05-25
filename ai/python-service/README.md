# Service FastAPI — moteur de recommandation

## Prérequis

- Python 3.8+  
- `pip install -r requirements.txt`

## Lancer le serveur

Depuis ce dossier :

```bash
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Vérification : [http://127.0.0.1:8000/health](http://127.0.0.1:8000/health) doit répondre `status: up`.

## Contrat utilisé par Spring Boot

- **POST** `/api/v1/recommendations/score`  
- Corps JSON : événement (`eventType`, `participantCount`, `budget`) + liste `providers` (identiques aux champs agrégés côté Java).  
- Réponse : tableau d’objets `providerUserId`, `businessName`, `compatibilityScore`, `acceptanceProbability`, `explanation`.

La valeur par défaut dans `application.yml` du backend pointe vers `http://127.0.0.1:8000`.

## Sans Python (secours)

Dans `application.yml` du backend : `recommendation.python.enabled: false` — le scoring est alors calculé en Java (`LocalRecommendationScorer`), utile si le service Python n’est pas démarré.
