#!/bin/bash
set -e

# ==============================================================================
# Entrypoint de sécurité Grafana - Contrôle Fail-Fast
# ==============================================================================

IS_PROD=0
APP_ENV_LOWER="$(echo "${APP_ENV:-development}" | tr '[:upper:]' '[:lower:]')"
if [ "$APP_ENV_LOWER" = "production" ] || [ "$APP_ENV_LOWER" = "prod" ]; then
    IS_PROD=1
fi

WEAK_PASSWORDS=("admin" "password" "root" "123456" "secret" "grafana" "changeme")

PASSWORD="${GF_SECURITY_ADMIN_PASSWORD}"

if [ -z "$PASSWORD" ]; then
    if [ $IS_PROD -eq 1 ]; then
        echo "ERREUR CRITIQUE DE SÉCURITÉ : GRAFANA_ADMIN_PASSWORD doit obligatoirement être défini en environnement de production." >&2
        exit 1
    else
        echo "AVERTISSEMENT SÉCURITÉ : Aucun mot de passe GRAFANA_ADMIN_PASSWORD configuré." >&2
    fi
else
    IS_WEAK=0
    for weak in "${WEAK_PASSWORDS[@]}"; do
        if [ "$PASSWORD" = "$weak" ]; then
            IS_WEAK=1
            break
        fi
    done

    if [ ${#PASSWORD} -lt 8 ]; then
        IS_WEAK=1
    fi

    if [ $IS_PROD -eq 1 ]; then
        if [ $IS_WEAK -eq 1 ]; then
            echo "ERREUR CRITIQUE DE SÉCURITÉ : Le mot de passe administrateur Grafana est trop faible (< 8 caractères) ou correspond à une valeur interdite par défaut." >&2
            exit 1
        fi
        echo "Configuration de sécurité Grafana validée avec succès en mode PRODUCTION."
    else
        if [ $IS_WEAK -eq 1 ]; then
            echo "AVERTISSEMENT SÉCURITÉ : Le mot de passe Grafana utilise une valeur par défaut ou faible. Cette valeur DOIT être changée en production."
        fi
    fi
fi

# Exécution du script de démarrage officiel Grafana
exec /run.sh "$@"
