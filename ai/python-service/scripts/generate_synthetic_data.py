#!/usr/bin/env python3
"""
generate_synthetic_data.py
==========================
Génère des données synthétiques réalistes pour l'entraînement du modèle ML
de recommandation EventSpace.

Le script :
  1. Crée 3 organisateurs + 6 prestataires via l'API Spring Boot
  2. Crée les profils prestataires (spécialités variées)
  3. Les approuve via le compte admin
  4. Crée 25 événements de types et tailles variés
  5. Insère 90 décisions ACCEPTEE / REFUSEE directement en SQL
     avec des patterns réalistes (type, capacité, budget)

Prérequis :
  - Spring Boot running sur localhost:8080
  - PostgreSQL accessible sur localhost:5432/eventdb
  - pip install requests psycopg2-binary

Usage :
  cd ai/python-service
  python scripts/generate_synthetic_data.py
"""
from __future__ import annotations

import os
import random
import sys
from datetime import date, timedelta
from typing import Any

# ── Dépendances ──────────────────────────────────────────────────────────────
try:
    import requests
except ImportError:
    sys.exit("❌  pip install requests")

try:
    import psycopg2
except ImportError:
    sys.exit("❌  pip install psycopg2-binary")

# ── Configuration ─────────────────────────────────────────────────────────────
API   = os.getenv("API_URL",      "http://localhost:8080")
DB    = {
    "host":     os.getenv("DB_HOST",     "localhost"),
    "port":     int(os.getenv("DB_PORT", "5432")),
    "dbname":   os.getenv("DB_NAME",     "eventdb"),
    "user":     os.getenv("DB_USER",     "postgres"),
    "password": os.getenv("DB_PASSWORD", "1234567890"),
}
ADMIN_EMAIL    = os.getenv("ADMIN_EMAIL",    "admin@align.local")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", "Test1234")
TEST_PASSWORD  = "Test1234!"

random.seed(42)  # Reproductibilité

# ── Données de référence ──────────────────────────────────────────────────────
ORGANISATEURS = [
    {"fullName": "Sophie Martin",   "email": "sophie.martin@synth.local"},
    {"fullName": "Karim Benali",    "email": "karim.benali@synth.local"},
    {"fullName": "Clara Dupont",    "email": "clara.dupont@synth.local"},
]

PRESTATAIRES = [
    {
        "fullName": "Élégance Traiteur",
        "email": "elegance@synth.local",
        "profile": {
            "businessName":      "Élégance Traiteur",
            "minCapacity":       50,
            "maxCapacity":       300,
            "minimumPrice":      2000,
            "acceptedEventTypes": ["Mariage", "Gala", "Cocktail", "Anniversaire"],
            "availabilityNotes": "Disponible vendredi-dimanche",
        },
    },
    {
        "fullName": "TechConf Solutions",
        "email": "techconf@synth.local",
        "profile": {
            "businessName":      "TechConf Solutions",
            "minCapacity":       20,
            "maxCapacity":       200,
            "minimumPrice":      1200,
            "acceptedEventTypes": ["Séminaire", "Conférence", "Formation", "Team Building"],
            "availabilityNotes": "Équipement audiovisuel inclus",
        },
    },
    {
        "fullName": "Sono & Lumières Pro",
        "email": "sonolum@synth.local",
        "profile": {
            "businessName":      "Sono & Lumières Pro",
            "minCapacity":       80,
            "maxCapacity":       500,
            "minimumPrice":      2500,
            "acceptedEventTypes": ["Concert", "Gala", "Mariage", "Soirée d'entreprise"],
            "availabilityNotes": "Montage la veille obligatoire",
        },
    },
    {
        "fullName": "Instants Précieux Photo",
        "email": "instants@synth.local",
        "profile": {
            "businessName":      "Instants Précieux Photo",
            "minCapacity":       10,
            "maxCapacity":       150,
            "minimumPrice":      600,
            "acceptedEventTypes": ["Mariage", "Anniversaire", "Cocktail", "Conférence"],
            "availabilityNotes": "Galerie en ligne sous 7 jours",
        },
    },
    {
        "fullName": "Formateurs Agiles",
        "email": "formateurs@synth.local",
        "profile": {
            "businessName":      "Formateurs Agiles",
            "minCapacity":       8,
            "maxCapacity":       60,
            "minimumPrice":      400,
            "acceptedEventTypes": ["Formation", "Team Building", "Séminaire"],
            "availabilityNotes": "Matériel pédagogique fourni",
        },
    },
    {
        "fullName": "Grand Château Events",
        "email": "chateau@synth.local",
        "profile": {
            "businessName":      "Grand Château Events",
            "minCapacity":       100,
            "maxCapacity":       600,
            "minimumPrice":      5000,
            "acceptedEventTypes": ["Mariage", "Gala", "Soirée d'entreprise", "Concert"],
            "availabilityNotes": "Hébergement possible sur site",
        },
    },
]

EVENEMENTS_TEMPLATE = [
    {"eventType": "Mariage",             "participantCount": 120, "budget": 15000},
    {"eventType": "Séminaire",           "participantCount": 45,  "budget": 3500},
    {"eventType": "Anniversaire",        "participantCount": 60,  "budget": 2000},
    {"eventType": "Concert",             "participantCount": 250, "budget": 20000},
    {"eventType": "Conférence",          "participantCount": 90,  "budget": 6000},
    {"eventType": "Gala",                "participantCount": 180, "budget": 25000},
    {"eventType": "Cocktail",            "participantCount": 70,  "budget": 4500},
    {"eventType": "Formation",           "participantCount": 25,  "budget": 1800},
    {"eventType": "Team Building",       "participantCount": 35,  "budget": 2200},
    {"eventType": "Soirée d'entreprise", "participantCount": 200, "budget": 18000},
    {"eventType": "Mariage",             "participantCount": 80,  "budget": 8000},
    {"eventType": "Séminaire",           "participantCount": 15,  "budget": 900},
    {"eventType": "Anniversaire",        "participantCount": 200, "budget": 500},
    {"eventType": "Concert",             "participantCount": 50,  "budget": 2000},
    {"eventType": "Conférence",          "participantCount": 160, "budget": 5000},
    {"eventType": "Gala",                "participantCount": 30,  "budget": 3000},
    {"eventType": "Cocktail",            "participantCount": 300, "budget": 4000},
    {"eventType": "Formation",           "participantCount": 70,  "budget": 1500},
    {"eventType": "Mariage",             "participantCount": 150, "budget": 22000},
    {"eventType": "Séminaire",           "participantCount": 100, "budget": 8000},
    {"eventType": "Team Building",       "participantCount": 55,  "budget": 3000},
    {"eventType": "Concert",             "participantCount": 400, "budget": 35000},
    {"eventType": "Anniversaire",        "participantCount": 30,  "budget": 1200},
    {"eventType": "Soirée d'entreprise", "participantCount": 90,  "budget": 7000},
    {"eventType": "Conférence",          "participantCount": 20,  "budget": 1500},
]

# ── Helpers API ───────────────────────────────────────────────────────────────
def _headers(token: str | None = None) -> dict:
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = f"Bearer {token}"
    return h


def _post(path: str, body: dict, token: str | None = None) -> Any:
    r = requests.post(f"{API}{path}", json=body, headers=_headers(token), timeout=15)
    if r.status_code >= 400:
        raise RuntimeError(f"POST {path} → {r.status_code}: {r.text[:200]}")
    return r.json()


def _put(path: str, body: dict, token: str | None = None) -> Any:
    r = requests.put(f"{API}{path}", json=body, headers=_headers(token), timeout=15)
    if r.status_code >= 400:
        raise RuntimeError(f"PUT {path} → {r.status_code}: {r.text[:200]}")
    return r.json()


def _patch(path: str, token: str | None = None) -> Any:
    r = requests.patch(f"{API}{path}", headers=_headers(token), timeout=15)
    if r.status_code >= 400:
        raise RuntimeError(f"PATCH {path} → {r.status_code}: {r.text[:200]}")
    return r.json()


def login(email: str, password: str) -> str:
    data = _post("/api/auth/login", {"email": email, "password": password})
    return data["token"]


def register_or_login(full_name: str, email: str, role: str) -> tuple[str, int]:
    """Crée le compte ou se connecte si déjà existant. Retourne (token, userId)."""
    try:
        data = _post("/api/auth/register", {
            "fullName": full_name,
            "email":    email,
            "password": TEST_PASSWORD,
            "role":     role,
        })
    except RuntimeError:
        data = _post("/api/auth/login", {"email": email, "password": TEST_PASSWORD})
    return data["token"], data["userId"]


# ── Scoring local simplifié pour décisions réalistes ─────────────────────────
def _decision(event: dict, profile: dict) -> str:
    """Simule la décision d'un prestataire (ACCEPTEE / REFUSEE) de façon réaliste."""
    et = event["eventType"].strip().lower()
    accepted_types = [t.strip().lower() for t in profile["acceptedEventTypes"]]

    score = 0.0
    # Type
    type_ok = et in accepted_types
    score += 35 if type_ok else 0

    # Capacité
    p = event["participantCount"]
    mn, mx = profile["minCapacity"], profile["maxCapacity"]
    if mn <= p <= mx:
        score += 30
    elif p < mn:
        score += max(0, 15 * (p / mn))
    else:
        score += max(0, 10 * (mx / p))

    # Budget
    budget = event["budget"]
    min_price = profile["minimumPrice"]
    ratio = budget / max(min_price, 1)
    if ratio >= 1.15:
        score += 25
    elif ratio >= 1.0:
        score += 20
    elif ratio >= 0.7:
        score += 10

    # Seuil + bruit aléatoire
    threshold = 45 + random.gauss(0, 8)
    return "ACCEPTEE" if score >= threshold else "REFUSEE"


# ── Main ──────────────────────────────────────────────────────────────────────
def main() -> None:
    print("\n🚀  Génération des données synthétiques EventSpace\n")

    # ── 1. Authentification admin ──────────────────────────────────────────────
    print("1/5  Connexion admin…")
    try:
        admin_token = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    except Exception as exc:
        sys.exit(f"❌  Impossible de se connecter en admin : {exc}\n"
                 "    Vérifiez que Spring Boot tourne sur localhost:8080")
    print("     ✓ Admin connecté")

    # ── 2. Organisateurs ────────────────────────────────────────────────────────
    print("\n2/5  Création des organisateurs…")
    orga_tokens: list[tuple[str, int]] = []
    for o in ORGANISATEURS:
        try:
            tok, uid = register_or_login(o["fullName"], o["email"], "ORGANISATEUR")
            orga_tokens.append((tok, uid))
            print(f"     ✓ {o['fullName']} (id={uid})")
        except Exception as exc:
            print(f"     ⚠  {o['fullName']} : {exc}")

    if not orga_tokens:
        sys.exit("❌  Aucun organisateur disponible.")

    # ── 3. Prestataires + profils ───────────────────────────────────────────────
    print("\n3/5  Création des prestataires et profils…")
    provider_profiles: list[dict] = []

    for p in PRESTATAIRES:
        try:
            tok, uid = register_or_login(p["fullName"], p["email"], "PRESTATAIRE")
            # Créer / mettre à jour le profil
            try:
                _put("/api/providers/me", p["profile"], token=tok)
            except Exception:
                pass
            # Récupérer l'id profile via admin
            try:
                _patch(f"/api/admin/providers/{uid}/approve", token=admin_token)
            except Exception:
                pass
            provider_profiles.append({"userId": uid, **p["profile"]})
            print(f"     ✓ {p['profile']['businessName']} (id={uid}) — approuvé")
        except Exception as exc:
            print(f"     ⚠  {p['fullName']} : {exc}")

    # Aussi approuver les prestataires existants
    try:
        import requests as _req
        r = _req.get(f"{API}/api/admin/pending-providers",
                     headers=_headers(admin_token), timeout=10)
        if r.status_code == 200:
            for pending in r.json():
                try:
                    _patch(f"/api/admin/providers/{pending['providerUserId']}/approve",
                           token=admin_token)
                except Exception:
                    pass
    except Exception:
        pass

    if not provider_profiles:
        sys.exit("❌  Aucun prestataire disponible.")

    # ── 4. Événements ────────────────────────────────────────────────────────────
    print("\n4/5  Création des événements…")
    events_created: list[dict] = []
    start_date = date.today() + timedelta(days=60)

    for i, tmpl in enumerate(EVENEMENTS_TEMPLATE):
        orga_tok, orga_id = orga_tokens[i % len(orga_tokens)]
        ev_date = (start_date + timedelta(days=30 * (i % 12) + random.randint(0, 20))).isoformat()
        body = {
            "eventType":       tmpl["eventType"],
            "eventDate":       ev_date,
            "participantCount": tmpl["participantCount"],
            "budget":          tmpl["budget"],
            "preferences":     f"Généré automatiquement — profil {i+1}",
        }
        try:
            ev = _post("/api/events", body, token=orga_tok)
            events_created.append({**ev, "organizerUserId": orga_id})
            print(f"     ✓ [{ev['id']}] {ev['eventType']} — {ev['participantCount']} pers. — {ev['budget']} DT")
        except Exception as exc:
            print(f"     ⚠  Événement {i+1} : {exc}")

    if not events_created:
        sys.exit("❌  Aucun événement créé.")

    # ── 5. Réservations en SQL ───────────────────────────────────────────────────
    print(f"\n5/5  Insertion des décisions de réservation…")

    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    # Détection du nom de table réel
    cur.execute("""
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name IN ('reservations','reservation_requests')
        LIMIT 1
    """)
    row = cur.fetchone()
    if not row:
        conn.close()
        sys.exit("❌  Table reservations / reservation_requests introuvable.")
    TABLE = row[0]
    print(f"     Table détectée : {TABLE}")

    # Détection du nom de colonne organizer
    cur.execute(f"""
        SELECT column_name FROM information_schema.columns
        WHERE table_name='{TABLE}'
          AND column_name IN ('organizer_user_id','organizer_id')
        LIMIT 1
    """)
    row2 = cur.fetchone()
    ORGA_COL = row2[0] if row2 else "organizer_user_id"

    inserted = 0
    for ev in events_created:
        for prov in provider_profiles:
            decision = _decision(ev, prov)
            try:
                cur.execute(
                    f"""
                    INSERT INTO {TABLE}
                        (event_id, {ORGA_COL}, provider_user_id, status,
                         created_at, updated_at)
                    VALUES (%s, %s, %s, %s,
                            NOW() - INTERVAL '1 day' * %s,
                            NOW() - INTERVAL '1 day' * %s)
                    ON CONFLICT DO NOTHING
                    """,
                    (
                        ev["id"],
                        ev["organizerUserId"],
                        prov["userId"],
                        decision,
                        random.randint(1, 90),
                        random.randint(0, 5),
                    ),
                )
                inserted += 1
            except Exception as exc:
                conn.rollback()
                print(f"     ⚠  Insert échoué : {exc}")
                continue

    conn.commit()
    cur.close()
    conn.close()

    # ── Résumé ──────────────────────────────────────────────────────────────────
    total = len(events_created) * len(provider_profiles)
    print(f"\n{'─'*55}")
    print(f"✅  Terminé !")
    print(f"   Organisateurs  : {len(orga_tokens)}")
    print(f"   Prestataires   : {len(provider_profiles)}")
    print(f"   Événements     : {len(events_created)}")
    print(f"   Décisions SQL  : {inserted} / {total} tentatives")
    print(f"{'─'*55}")
    print("\n📌  Prochaine étape :")
    print("   curl -X POST http://127.0.0.1:8000/api/v1/model/train")
    print("   curl http://127.0.0.1:8000/api/v1/model/info\n")


if __name__ == "__main__":
    main()