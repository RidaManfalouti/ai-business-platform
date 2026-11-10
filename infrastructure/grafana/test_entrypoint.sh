#!/bin/bash
set -euo pipefail

# ==============================================================================
# Suite de tests de validation de sécurité de l'entrypoint Grafana
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENTRYPOINT_SCRIPT="${SCRIPT_DIR}/entrypoint.sh"

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}  Suite de tests de sécurité : Grafana Production Secret Validation           ${NC}"
echo -e "${BLUE}==============================================================================${NC}"

run_test() {
    local test_name="$1"
    local app_env="$2"
    local has_password="$3"
    local password_value="$4"
    local expected_status="$5" # 0 = acceptation, 1 = refus
    local expected_output_pattern="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "Test $TOTAL_TESTS: [$app_env] $test_name ... "

    local stdout_file
    local stderr_file
    stdout_file="$(mktemp)"
    stderr_file="$(mktemp)"

    local exit_code=0
    (
        export APP_ENV="$app_env"
        export GRAFANA_RUN_CMD="true"
        if [ "$has_password" = "yes" ]; then
            export GF_SECURITY_ADMIN_PASSWORD="$password_value"
            unset GRAFANA_ADMIN_PASSWORD || true
        else
            unset GF_SECURITY_ADMIN_PASSWORD || true
            unset GRAFANA_ADMIN_PASSWORD || true
        fi
        bash "$ENTRYPOINT_SCRIPT" >"$stdout_file" 2>"$stderr_file"
    ) || exit_code=$?

    local test_passed=1
    local failure_reason=""

    # 1. Vérification du code de retour
    if [ "$exit_code" -ne "$expected_status" ]; then
        test_passed=0
        failure_reason="Code de retour $exit_code (attendu: $expected_status)"
    fi

    # 2. Vérification du message attendu
    local combined_output
    combined_output="$(cat "$stdout_file" "$stderr_file")"
    if [ -n "$expected_output_pattern" ]; then
        if ! echo "$combined_output" | grep -qE "$expected_output_pattern"; then
            test_passed=0
            failure_reason="${failure_reason:-Motif de log non trouvé: '$expected_output_pattern'}"
        fi
    fi

    # 3. Règle de sécurité critique : AUCUN secret spécifique ne doit jamais fuiter dans les logs
    if [ "$has_password" = "yes" ] && [ -n "$password_value" ] && [ ${#password_value} -ge 8 ]; then
        # Exclure les termes légitimes du template de message (ex: "grafana", "placeholder")
        local pw_lower
        pw_lower="$(echo -n "$password_value" | tr '[:upper:]' '[:lower:]')"
        if [ "$pw_lower" != "grafana" ] && [ "$pw_lower" != "placeholder" ]; then
            if echo "$combined_output" | grep -Fq "$password_value"; then
                test_passed=0
                failure_reason="FAILLE DE SÉCURITÉ : La valeur du secret a fuité dans les logs !"
            fi
        fi
    fi

    if [ "$test_passed" -eq 1 ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "${GREEN}SUCCÈS${NC} (code: $exit_code)"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}ÉCHEC${NC} - $failure_reason"
        echo "  Sortie observée :"
        sed 's/^/    /' <(echo "$combined_output")
    fi

    rm -f "$stdout_file" "$stderr_file"
}

echo -e "\n${YELLOW}--- 1. TESTS EN ENVIRONNEMENT PRODUCTION (APP_ENV=production) ---${NC}"
run_test "Variable absente" \
    "production" "no" "" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*GRAFANA_ADMIN_PASSWORD doit obligatoirement être défini"

run_test "Variable vide" \
    "production" "yes" "" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*GRAFANA_ADMIN_PASSWORD doit obligatoirement être défini et non vide"

run_test "Variable avec uniquement des espaces" \
    "production" "yes" "     " 1 "ERREUR CRITIQUE DE SÉCURITÉ.*GRAFANA_ADMIN_PASSWORD doit obligatoirement être défini et non vide"

run_test "Mot de passe interdit: 'admin'" \
    "production" "yes" "admin" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: 'your_secure_grafana_password_here' (Placeholder bypass)" \
    "production" "yes" "your_secure_grafana_password_here" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible.*placeholder"

run_test "Mot de passe interdit: 'changeme'" \
    "production" "yes" "changeme" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: 'password'" \
    "production" "yes" "password" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: 'root'" \
    "production" "yes" "root" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: '123456'" \
    "production" "yes" "123456" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: 'secret'" \
    "production" "yes" "secret" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Mot de passe interdit: 'grafana'" \
    "production" "yes" "grafana" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Ancien placeholder documenté: 'your_secure_grafana_password'" \
    "production" "yes" "your_secure_grafana_password" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible.*placeholder"

run_test "Placeholder générique: 'your_secure_password_here'" \
    "production" "yes" "your_secure_password_here" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible.*placeholder"

run_test "Mot de passe trop court (< 8 caractères, ex: 'Abc1!')" \
    "production" "yes" "Abc1!" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Répétition triviale d'un unique caractère (ex: 'aaaaaaaa')" \
    "production" "yes" "aaaaaaaa" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Variante de casse d'un placeholder: 'Your_Secure_Grafana_Password_Here'" \
    "production" "yes" "Your_Secure_Grafana_Password_Here" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible.*placeholder"

run_test "Variante de casse: 'ChangeMe'" \
    "production" "yes" "ChangeMe" 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible"

run_test "Placeholder avec espaces de padding: '  your_secure_grafana_password_here  '" \
    "production" "yes" "  your_secure_grafana_password_here  " 1 "ERREUR CRITIQUE DE SÉCURITÉ.*trop faible.*placeholder"

run_test "Mot de passe fort de test" \
    "production" "yes" "Str0ng-Grafana-P@ssw0rd-2026!" 0 "Configuration de sécurité Grafana validée avec succès en mode PRODUCTION"


echo -e "\n${YELLOW}--- 2. TESTS EN ENVIRONNEMENT DÉVELOPPEMENT (APP_ENV=development) ---${NC}"
run_test "Variable absente en développement" \
    "development" "no" "" 0 "AVERTISSEMENT SÉCURITÉ : Aucun mot de passe GRAFANA_ADMIN_PASSWORD configuré"

run_test "Variable vide en développement" \
    "development" "yes" "" 0 "AVERTISSEMENT SÉCURITÉ : Aucun mot de passe GRAFANA_ADMIN_PASSWORD configuré ou valeur vide"

run_test "Mot de passe faible 'admin' en développement" \
    "development" "yes" "admin" 0 "AVERTISSEMENT SÉCURITÉ : Le mot de passe Grafana utilise une valeur par défaut"

run_test "Placeholder 'your_secure_grafana_password_here' en développement" \
    "development" "yes" "your_secure_grafana_password_here" 0 "AVERTISSEMENT SÉCURITÉ : Le mot de passe Grafana utilise une valeur par défaut"

run_test "Mot de passe faible 'changeme' en développement" \
    "development" "yes" "changeme" 0 "AVERTISSEMENT SÉCURITÉ : Le mot de passe Grafana utilise une valeur par défaut"

run_test "Mot de passe fort de test en développement (démarrage sans avertissement)" \
    "development" "yes" "Str0ng-Grafana-P@ssw0rd-2026!" 0 ""


echo -e "\n${BLUE}==============================================================================${NC}"
echo -e "Total: $TOTAL_TESTS | Succès: ${GREEN}$PASSED_TESTS${NC} | Échecs: ${RED}$FAILED_TESTS${NC}"
echo -e "${BLUE}==============================================================================${NC}"

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo -e "${GREEN}TOUS LES TESTS SONT PASSÉS AVEC SUCCÈS !${NC}\n"
    exit 0
else
    echo -e "${RED}DES TESTS ONT ÉCHOUÉ.${NC}\n"
    exit 1
fi
