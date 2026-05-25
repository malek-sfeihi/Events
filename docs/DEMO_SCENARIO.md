# Scénario de démonstration (soutenance)

À faire **la veille** ou **1 h avant** : PostgreSQL + backend Spring + frontend Angular ; pour les **recommandations IA**, lancer aussi FastAPI (voir `LOCAL_SETUP.md`).

---

## Comptes « tout prêt » (profil `dev`)

Si la base est vide au premier démarrage, le backend crée automatiquement :

| Email | Mot de passe | Rôle |
|--------|----------------|------|
| `admin@align.local` | `Test1234` | ADMIN |
| `orga@align.local` | `Test1234` | ORGANISATEUR |
| `presta.ok@align.local` | `Test1234` | PRESTATAIRE (profil **déjà approuvé**) |
| `presta.attente@align.local` | `Test1234` | PRESTATAIRE (profil **en attente** de validation) |

Un événement « Mariage » existe déjà pour l’organisateur `orga@align.local`.

Si ces comptes existent déjà, le seed ne se relance pas — tu peux quand même suivre le scénario avec ces identifiants.

**Important :** ouvre le front avec **`http://localhost:4200`** (ou `127.0.0.1` après correction CORS). L’API est **`http://localhost:8080`**.

---

## Ordre conseillé pour la prof (15–20 min)

### 1) Admin — qualité du catalogue (3–4 min)

1. Connexion : `admin@align.local` / `Test1234`.
2. Montrer les **indicateurs** en haut (utilisateurs, réservations, etc.).
3. Section **Validation prestataires** : approuver **« DJ et Son — en attente »** (`presta.attente@align.local`).
4. Section **Comptes** : faire défiler la liste (plusieurs rôles).

**Phrase clé :** *« Seuls les prestataires validés apparaissent dans le catalogue organisateur. »*

### 2) Organisateur — besoin client → événement (4–5 min)

1. Déconnexion → connexion : `orga@align.local` / `Test1234`.
2. **Événements** : montrer l’événement déjà créé (type, date, participants, **budget en DT**).
3. Option : créer vite un second événement du même type pour montrer le formulaire.

**Phrase clé :** *« Le besoin du client est structuré comme un projet d’événement dans l’application. »*

### 3) Recommandations IA (5 min) — nécessite FastAPI

1. Menu **Recommandations**.
2. Choisir l’événement → tableau : score, probabilité, **texte d’explication**.
3. Expliquer : *« Le backend envoie les données au service Python ; celui-ci calcule le classement. »*

Si Python n’est pas lancé : soit activer le mode Java (`recommendation.python.enabled: false`), soit lancer `uvicorn` avant la démo.

### 4) Catalogue et réservation (4–5 min)

1. **Catalogue** : filtres, cartes prestataires (dont celui qu’on vient d’approuver si tu actualises).
2. **Réserver** : choisir un prestataire + un événement → envoyer la demande.
3. **Mes demandes** : statut « en attente ».

**Phrase clé :** *« L’organisateur formalise une demande ; le prestataire décide ensuite. »*

### 5) Prestataire — réponse (3 min)

1. Déconnexion → `presta.ok@align.local` ou `presta.attente@align.local` / `Test1234`.
2. **Demandes reçues** : **Accepter** ou **Refuser** la réservation créée à l’étape 4.

### 6) Conclusion (1 min)

Spring Boot + PostgreSQL + Angular + microservice Python pour le scoring ; JWT et rôles ; démo bout-en-bout alignée avec le rapport.

---

## Si quelque chose est vide

- **Pas de prestataires en attente** : normal si tout est déjà approuvé ; créer un nouveau compte PRESTATAIRE depuis l’inscription, remplir le profil, puis retour admin pour valider.
- **Pas de recommandations** : vérifier FastAPI + `enabled: true` dans `application.yml`.
- **Admin en erreur** : voir section ci-dessous.

---

## Check-list démo express

- [ ] PostgreSQL up  
- [ ] `mvn spring-boot:run` (port 8080)  
- [ ] `python -m uvicorn ...` (port 8000) si démo IA complète  
- [ ] `npx ng serve` — URL **localhost:4200** ou **127.0.0.1:4200**  
- [ ] Connexion admin → liste utilisateurs visible  
