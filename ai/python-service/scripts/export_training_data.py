"""
Étape 2 ML — Export du dataset d'entraînement depuis PostgreSQL.

Chaque ligne = une réservation déjà traitée (ACCEPTEE ou REFUSEE).
Le label indique si le prestataire a accepté (1) ou refusé (0).

Usage (depuis ai/python-service/) :
  python scripts/export_training_data.py
  python scripts/export_training_data.py --output data/reservations_training.csv

Variables d'environnement (ou fichier .env à la racine de python-service) :
  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
from pathlib import Path

# Permet d'importer app.scoring pour recalculer les sous-scores (alignés règles actuelles)
_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from app.scoring import _score_budget, _score_capacity, _score_event_type  # noqa: E402

try:
    import psycopg2
    from psycopg2.extras import RealDictCursor
except ImportError:
    print(
        "psycopg2 manquant. Installez les dépendances :\n"
        "  pip install -r requirements.txt",
        file=sys.stderr,
    )
    sys.exit(1)


DEFAULT_OUTPUT = _ROOT / "data" / "reservations_training.csv"

# Requête : une ligne par demande terminée, avec contexte événement + prestataire
EXPORT_SQL = """
SELECT
    r.id AS reservation_id,
    r.event_id,
    r.provider_user_id,
    r.organizer_user_id,
    r.status,
    e.event_type,
    e.participant_count,
    e.budget::float AS budget,
    p.min_capacity AS provider_min_capacity,
    p.max_capacity AS provider_max_capacity,
    p.minimum_price::float AS provider_minimum_price,
    p.approved AS provider_approved,
    COALESCE(types.accepted_event_types, '') AS accepted_event_types
FROM reservation_requests r
INNER JOIN events e ON e.id = r.event_id
INNER JOIN provider_profiles p ON p.provider_user_id = r.provider_user_id
LEFT JOIN (
    SELECT provider_profile_id,
           string_agg(event_type, '|' ORDER BY event_type) AS accepted_event_types
    FROM provider_event_types
    GROUP BY provider_profile_id
) types ON types.provider_profile_id = p.id
WHERE r.status IN ('ACCEPTEE', 'REFUSEE')
ORDER BY r.id;
"""


def _load_dotenv() -> None:
    env_path = _ROOT / ".env"
    if not env_path.is_file():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def _db_config() -> dict:
    _load_dotenv()
    return {
        "host": os.environ.get("DB_HOST", "localhost"),
        "port": int(os.environ.get("DB_PORT", "5432")),
        "dbname": os.environ.get("DB_NAME", "eventdb"),
        "user": os.environ.get("DB_USER", "postgres"),
        "password": os.environ.get("DB_PASSWORD", "1234567890"),
    }


def _parse_accepted_types(raw: str) -> list[str]:
    if not raw:
        return []
    return [t.strip() for t in raw.split("|") if t.strip()]


def _enrich_row(row: dict) -> dict:
    """Ajoute label et features dérivées (mêmes idées que scoring.py)."""
    accepted = _parse_accepted_types(row.pop("accepted_event_types", "") or "")
    event_type = row.get("event_type") or ""
    participants = int(row["participant_count"])
    budget = float(row["budget"])
    min_c = int(row["provider_min_capacity"])
    max_c = int(row["provider_max_capacity"])
    min_price = float(row["provider_minimum_price"])

    type_score = _score_event_type(event_type, accepted)
    capacity_score = _score_capacity(participants, min_c, max_c)
    budget_score = _score_budget(budget, min_price)

    status = row["status"]
    label = 1 if status == "ACCEPTEE" else 0

    row["label"] = label
    row["event_type_match"] = 1 if type_score >= 95 else 0
    row["score_type"] = round(type_score, 2)
    row["score_capacity"] = round(capacity_score, 2)
    row["score_budget"] = round(budget_score, 2)
    row["budget_ratio"] = round(budget / min_price, 4) if min_price > 0 else 0.0
    row["capacity_in_range"] = (
        1 if min_c <= participants <= max_c else 0
    )
    return row


def fetch_rows() -> list[dict]:
    cfg = _db_config()
    conn = psycopg2.connect(**cfg)
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(EXPORT_SQL)
            return [dict(r) for r in cur.fetchall()]
    finally:
        conn.close()


def write_csv(rows: list[dict], output: Path) -> None:
    if not rows:
        print(
            "Aucune réservation ACCEPTEE/REFUSEE en base.\n"
            "Pour alimenter le dataset : créez des demandes via l'app, puis\n"
            "connectez-vous prestataire et acceptez ou refusez-les.",
            file=sys.stderr,
        )
        output.parent.mkdir(parents=True, exist_ok=True)
        # Fichier vide avec en-têtes pour documenter le format
        fieldnames = [
            "reservation_id",
            "event_id",
            "provider_user_id",
            "organizer_user_id",
            "status",
            "label",
            "event_type",
            "participant_count",
            "budget",
            "provider_min_capacity",
            "provider_max_capacity",
            "provider_minimum_price",
            "provider_approved",
            "event_type_match",
            "budget_ratio",
            "capacity_in_range",
            "score_type",
            "score_capacity",
            "score_budget",
        ]
        with output.open("w", newline="", encoding="utf-8") as f:
            csv.DictWriter(f, fieldnames=fieldnames).writeheader()
        return

    enriched = [_enrich_row(r) for r in rows]
    fieldnames = list(enriched[0].keys())
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(enriched)


def main() -> None:
    parser = argparse.ArgumentParser(description="Export dataset ML depuis PostgreSQL")
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"Chemin CSV (défaut: {DEFAULT_OUTPUT})",
    )
    args = parser.parse_args()

    print("Connexion PostgreSQL…", file=sys.stderr)
    try:
        rows = fetch_rows()
    except psycopg2.OperationalError as ex:
        print(
            f"Impossible de se connecter à la base : {ex}\n\n"
            "Vérifiez que PostgreSQL tourne et que les variables DB_* "
            "(ou .env) correspondent à application-dev.yml.",
            file=sys.stderr,
        )
        sys.exit(1)

    write_csv(rows, args.output)

    if rows:
        accepted = sum(1 for r in rows if r["status"] == "ACCEPTEE")
        refused = len(rows) - accepted
        print(
            f"Export OK : {len(rows)} lignes → {args.output}\n"
            f"  ACCEPTEE: {accepted}  |  REFUSEE: {refused}",
            file=sys.stderr,
        )
    else:
        print(f"Fichier modèle créé (0 ligne) → {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
