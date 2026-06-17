# Données ML (étape 2)

Fichiers générés localement — **non versionnés** (voir `.gitignore`).

## Générer le dataset

Depuis `ai/python-service/` :

```bash
pip install -r requirements.txt
python scripts/export_training_data.py
```

Produit : `data/reservations_training.csv`

## Prérequis

- PostgreSQL avec la base `eventdb` (profil Spring `dev`)
- Au moins quelques réservations en statut **ACCEPTEE** ou **REFUSEE** (pas seulement EN_ATTENTE)

## Colonnes principales

| Colonne | Rôle |
|---------|------|
| `label` | **Cible ML** : 1 = acceptée, 0 = refusée |
| `event_type`, `participant_count`, `budget` | Contexte événement |
| `provider_*` | Contexte prestataire au moment de la demande |
| `event_type_match`, `budget_ratio`, `capacity_in_range` | Features dérivées (étape 3) |
| `score_type`, `score_capacity`, `score_budget` | Sous-scores règles (référence / baseline) |
