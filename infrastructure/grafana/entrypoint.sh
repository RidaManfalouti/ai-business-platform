#!/bin/bash
set -e

# ==============================================================================
# Entrypoint de sécurité Grafana - Contrôle Fail-Fast
# ==============================================================================

IS_PROD=0
APP_ENV_LOWER="${APP_ENV:-development}"
APP_ENV_LOWER="$(echo "$APP_ENV_LOWER" | tr '[:upper:]' '[:lower:]')"
if [ "$APP_ENV_LOWER" = "production" ] || [ "$APP_ENV_LOWER" = "prod" ]; then
    IS_PROD=1
fi

# Liste des mots de passe faibles, communs et des placeholders documentés interdits
FORBIDDEN_PASSWORDS=(
    "admin"
    "password"
    "root"
    "123456"
    "12345678"
    "123456789"
    "0000"
    "000000"
    "00000000"
    "secret"
    "grafana"
    "changeme"
    "change_me"
    "your_secure_grafana_password_here"
    "your_secure_grafana_password"
    "your_secure_password_here"
    "your_secure_password"
    "your_password_here"
    "your_password"
    "postgres"
    "default"
    "guest"
    "qwerty"
    "placeholder"
)

PASSWORD="${GF_SECURITY_ADMIN_PASSWORD:-${GRAFANA_ADMIN_PASSWORD}}"
PASSWORD_TRIMMED="$(echo -n "${PASSWORD}" | sed -e 's/^[[:space:]]*//;s/[[:space:]]*$//')"

if [ -z "$PASSWORD_TRIMMED" ]; then
    if [ $IS_PROD -eq 1 ]; then
        echo "ERREUR CRITIQUE DE SÉCURITÉ : GRAFANA_ADMIN_PASSWORD doit obligatoirement être défini et non vide en environnement de production." >&2
        exit 1
    else
        echo "AVERTISSEMENT SÉCURITÉ : Aucun mot de passe GRAFANA_ADMIN_PASSWORD configuré ou valeur vide." >&2
    fi
else
    IS_INVALID=0

    # 1. Contrôle de longueur minimale (< 8 caractères effectifs)
    if [ ${#PASSWORD_TRIMMED} -lt 8 ]; then
        IS_INVALID=1
    fi

    # 2. Contrôle insensible à la casse contre les mots de passe interdits et placeholders connus
    if [ $IS_INVALID -eq 0 ]; then
        PASSWORD_LOWER="$(echo -n "${PASSWORD_TRIMMED}" | tr '[:upper:]' '[:lower:]')"
        for forbidden in "${FORBIDDEN_PASSWORDS[@]}"; do
            if [ "$PASSWORD_LOWER" = "$forbidden" ]; then
                IS_INVALID=1
                break
            fi
        done
    fi

    # 3. Contrôle des motifs de placeholders génériques ou d'exemples documentés
    if [ $IS_INVALID -eq 0 ]; then
        case "$PASSWORD_LOWER" in
            *your_secure_*|*your_password*|*_password_here*|*placeholder*|*changeme*|*change_me*)
                IS_INVALID=1
                ;;
        esac
    fi

    # 4. Contrôle contre la répétition triviale d'un seul et unique caractère (ex: "aaaaaaaa", "11111111")
    if [ $IS_INVALID -eq 0 ]; then
        FIRST_CHAR="${PASSWORD_TRIMMED:0:1}"
        if [ -z "${PASSWORD_TRIMMED//"$FIRST_CHAR"/}" ]; then
            IS_INVALID=1
        fi
    fi

    if [ $IS_PROD -eq 1 ]; then
        if [ $IS_INVALID -eq 1 ]; then
            echo "ERREUR CRITIQUE DE SÉCURITÉ : Le mot de passe administrateur Grafana est trop faible (< 8 caractères), correspond à un placeholder ou à une valeur interdite par défaut." >&2
            exit 1
        fi
        echo "Configuration de sécurité Grafana validée avec succès en mode PRODUCTION."
    else
        if [ $IS_INVALID -eq 1 ]; then
            echo "AVERTISSEMENT SÉCURITÉ : Le mot de passe Grafana utilise une valeur par défaut, un placeholder ou est trop faible. Cette valeur DOIT être changée en production."
        fi
    fi
fi

# Exécution du script de démarrage officiel Grafana
RUN_CMD="${GRAFANA_RUN_CMD:-/run.sh}"
exec "$RUN_CMD" "$@"
