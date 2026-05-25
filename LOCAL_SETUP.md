# Démarrage en local (toute la plateforme)

Ordre recommandé : base de données → Python (IA) → backend Java → frontend Angular.

## 1. Prérequis

- **JDK 17**, **Maven**
- **Node.js** (LTS) et **npm**
- **PostgreSQL** en service local
- **Python 3.8+** avec `pip`

## 2. PostgreSQL

1. Créer une base (ex. `eventdb`) et un utilisateur avec droits sur cette base.
2. Copier les paramètres dans  
   `back/event-backend/src/main/resources/application-dev.yml`  
   (`spring.datasource.url`, `username`, `password`).

## 3. Service IA (FastAPI)

```powershell
cd "ai\python-service"
pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Vérifier : navigateur ou curl → `http://127.0.0.1:8000/health` → `"status": "up"`.

Le backend Spring lit par défaut `http://127.0.0.1:8000` dans `application.yml` (`recommendation.python.*`).

**Sans Python** (pour tester vite le reste) : dans `application.yml`, passer  
`recommendation.python.enabled: false`  
(le scoring est alors calculé en Java, même logique).

## 4. Backend Spring Boot

```powershell
cd "back\event-backend"
mvn spring-boot:run
```

API : `http://localhost:8080` — santé : `GET http://localhost:8080/api/health`

Profil actif par défaut : `dev` (voir `application.yml`).

## 5. Frontend Angular

```powershell
cd "front"
npm install
npx ng serve
```

Application : `http://localhost:4200`

## 6. Comptes de test

Utiliser les comptes documentés dans le projet / Postman (`back/event-backend/postman/`) pour les rôles **ORGANISATEUR**, **PRESTATAIRE**, **ADMIN**.

Parcours démo : connexion organisateur → créer un événement → menu **Recommandations** → catalogue → envoyer une demande ; prestataire → traiter la demande ; admin → valider un profil si besoin.

## 7. Dépannage rapide

| Problème | Piste |
|----------|--------|
| Erreur 503 sur les recommandations | FastAPI non démarré ou mauvais port ; ou désactiver Python (`enabled: false`). |
| Erreur DB | URL / mot de passe PostgreSQL dans `application-dev.yml`. |
| CORS | Déjà géré côté Spring ; front sur 4200, API sur 8080. |
