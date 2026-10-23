# AI Business Platform

Plateforme microservices modulaire pour la gestion commerciale et d'entreprise, développée avec **Spring Boot 3.4 (Spring Cloud 2024)**, **Java 21**, **PostgreSQL 16**, **Redis 7**, et orchestrée avec **Docker Compose**.

---

## 1. Architecture & Services

La plateforme est articulée autour des microservices suivants :

| Service | Port Interne | Port Externe | Description |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | `8080` | Point d'entrée unique (Spring Cloud Gateway WebFlux), routage, CORS, reverse proxy |
| **Auth Service** | `8081` | *Interne* | Gestion des comptes utilisateurs et émission des tokens JWT HMAC-SHA256 |
| **Customer Service** | `8083` | *Interne* | Gestion de la relation client (CRM) et coordonnées des entreprises |
| **Product Service** | `8082` | *Interne* | Catalogue de produits, inventaire et **réservations atomiques de stock** |
| **Sales Service** | `8084` | *Interne* | Commandes clients, recalcul serveur, **idempotence** et **Saga compensatoire** |
| **PostgreSQL** | `5432` | `5433` | Base relationnelle multi-bases (`auth_db`, `customer_db`, `product_db`, `sales_db`) |
| **Redis** | `6379` | `6379` | Cache distribué et sessions |

> **Sécurité réseau :** Seul le service **API Gateway** expose son port HTTP sur la machine hôte (`8080`). Les microservices métier communiquent de manière isolée via le réseau interne Docker Compose par leurs noms de service DNS (`http://auth-service:8081`, `http://product-service:8082`, etc.).

---

## 2. Prérequis

- **Docker** Engine 24+ et **Docker Compose** v2.20+
- **Java 21** & **Maven 3.9+** (uniquement en cas de développement / exécution locale hors conteneur)

---

## 3. Configuration de l'Environnement

Un fichier modèle `.env.example` est fourni à la racine du projet. Créez votre fichier local `.env` :

```bash
cp .env.example .env
```

Vérifiez les variables d'environnement principales dans `.env` :
- `POSTGRES_USER` : Utilisateur de la base de données (défaut: `ai_platform`).
- `POSTGRES_PASSWORD` : Mot de passe sécurisé.
- `JWT_SECRET` : Clé secrète partagée HMAC-SHA256 (encodée en Base64, min. 256 bits).
- `GATEWAY_PORT` : Port d'écoute public de l'API Gateway (défaut: `8080`).

---

## 4. Démarrage de la Plateforme (Docker Compose)

### Construire et démarrer tous les services :

```bash
docker compose up -d --build
```

### Ordre de démarrage et sondes de préparation (Readiness Checks) :
Le fichier `docker-compose.yml` configure un ordonnancement strict basé sur les healthchecks :
1. **PostgreSQL** s'initialise et devient `healthy` via `pg_isready`.
2. **Auth Service**, **Customer Service** et **Product Service** démarrent simultanément et exécutent leurs migrations Flyway.
3. **Sales Service** démarre une fois que PostgreSQL, Product Service et Customer Service sont prêts (`healthy`).
4. **API Gateway** démarre une fois que tous les services cibles sont opérationnels.

---

## 5. Surveillance & Santé des Services

### Vérifier le statut de tous les conteneurs :

```bash
docker compose ps
```

Tous les conteneurs doivent afficher le statut `(healthy)`.

### Consulter les logs en temps réel :

```bash
# Logs de l'ensemble de la plateforme
docker compose logs -f

# Logs d'un service spécifique
docker compose logs -f api-gateway
docker compose logs -f sales-service
docker compose logs -f product-service
```

### Vérifier la santé via l'API Gateway :

```bash
curl http://localhost:8080/actuator/health
```

---

## 6. Guide de Test Rapide (Smoke Test)

Toutes les requêtes s'effectuent à travers l'**API Gateway** (`http://localhost:8080`) :

### A. Inscription et obtention d'un jeton JWT
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"dev_user","email":"dev@example.com","password":"Password123!"}' \
  | jq -r '.token')
echo "Token: $TOKEN"
```

### B. Créer un client
```bash
CUSTOMER_ID=$(curl -s -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"firstName":"Jean","lastName":"Dupont","email":"jean@example.com","company":"Acme Corp"}' \
  | jq -r '.id')
```

### C. Créer un produit avec un stock initial de 10
```bash
PRODUCT_ID=$(curl -s -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"ThinkPad X1","description":"Ultrabook","price":1500.00,"stockQuantity":10,"category":"PC"}' \
  | jq -r '.id')
```

### D. Créer une vente de 2 unités avec clé d'idempotence
```bash
curl -s -X POST "http://localhost:8080/api/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: CMD-DEV-001" \
  -d "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":2}]}"
```
*Le stock du produit dans `product-service` passe immédiatement de 10 à 8 de manière atomique.*

### E. Vérifier le rejet d'une survente (Overselling)
```bash
# Tenter de réserver 50 unités alors que 8 sont disponibles
curl -s -X POST "http://localhost:8080/api/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":50}]}"
```
*Réponse : HTTP 409 Conflict (Stock insuffisant).*

---

## 7. Procédures de Réinitialisation & Nettoyage

### Arrêt standard (conservation des données) :
```bash
docker compose down
```
*Les volumes `postgres_data` et `redis_data` sont conservés intacts.*

### Réinitialisation complète pour environnement jetable :
Pour réinitialiser complètement la base de données et repartir d'un environnement propre :
```bash
docker compose down -v
```
*Au prochain `docker compose up -d`, le script `infrastructure/postgres/init-databases.sh` recréera automatiquement toutes les bases de données et Flyway réappliquera l'ensemble des migrations.*

---

## 8. Dépannage (Troubleshooting)

- **Erreur 401 Unauthorized systématique :**
  Vérifiez que la variable `JWT_SECRET` dans `.env` est strictement identique entre `auth-service` et les services ressources (`product-service`, `customer-service`, `sales-service`).
- **Erreur de connexion base de données au démarrage :**
  Assurez-vous que le conteneur PostgreSQL est sain (`docker compose ps postgres`). Si les bases n'existent pas, exécutez `infrastructure/postgres/init-databases.sh` ou réinitialisez le volume avec `docker compose down -v`.
- **Conflit de port sur l'hôte :**
  Si le port 8080 ou 5433 est déjà utilisé par une autre application locale, modifiez `GATEWAY_PORT` ou `POSTGRES_PORT` dans votre fichier `.env`.

---

## 9. Intégration Continue (CI/CD) & Assurance Qualité

Le pipeline GitHub Actions est configuré dans `.github/workflows/ci.yml`.

### Comportement du Workflow CI

Le workflow est déclenché sur :
- Tout **Pull Request** ciblant la branche `main`
- Tout **Push** sur `main`, `develop` et les branches de fonctionnalités `feature/**`

Il comprend 3 jobs distincts :

1. **`security-checks` (Qualité & Sécurité) :**
   - **Gitleaks (Vérification bloquante) :** Analyse l'historique Git et les fichiers de configuration à l'aide de `.gitleaks.toml` pour détecter et bloquer toute fuite de clés ou jetons réels.
   - **Trivy (Avertissement informationnel) :** Analyse les dépendances de chaque module Java pour détecter les vulnérabilités CVE critiques et élevées sans bloquer arbitrairement la livraison en cas de dépendance transitive sans correctif disponible.
2. **`maven-build-and-test` (Matrice Parallèle de Tests) :**
   - Provisionne un service de test PostgreSQL 16 éphémère.
   - Initialise les 5 bases de données (`auth_db`, `customer_db`, `product_db`, `sales_db`, `analytics_db`).
   - Exécute les tests Maven pour chaque microservice en parallèle via une matrice GitHub Actions (`auth-service`, `api-gateway`, `product-service`, `customer-service`, `sales-service`).
   - Archive les rapports de tests Surefire (`surefire-reports-*`) sous forme d'artefacts conservés 7 jours.
3. **`docker-compose-validate` (Validation Docker & Smoke Test) :**
   - Valide la syntaxe Compose via `docker compose config`.
   - Construit les 5 images Docker via leurs builds multi-étapes.
   - Démarre la stack complète en conditions réelles et attend la préparation (`healthy`) de tous les conteneurs.
   - Exécute un test de fumée (Smoke Test) de bout en bout à travers l'API Gateway : authentification, création client, création produit, réservation atomique, rejet d'une survente et compensation Saga lors d'une annulation.

### Exécuter les tests localement

Pour exécuter les tests d'un service spécifique :
```bash
mvn -f services/product-service/pom.xml test
```

Pour exécuter les tests de l'ensemble du monorepo :
```bash
for service in auth-service api-gateway product-service customer-service sales-service; do
  echo ">>> Exécution des tests pour : $service"
  mvn -B -f "services/$service/pom.xml" test
done
```

> **Note :** Les tests d'intégration nécessitent que le conteneur PostgreSQL soit actif sur le port `5433` (exécuter `docker compose up -d postgres`).

### Diagnostiquer un échec de job CI GitHub Actions

1. Accédez à l'onglet **Actions** sur GitHub : `https://github.com/RidaManfalouti/ai-business-platform/actions`.
2. Cliquez sur l'exécution en échec.
3. Repérez le job spécifique en erreur dans la matrice (ex: `Build & Test (sales-service)`).
4. Déroulez l'étape **Compile & Run Maven Tests** pour visualiser la stacktrace.
5. En bas de page du workflow, téléchargez l'artefact `surefire-reports-<service>` contenant les fichiers XML et TXT détaillés pour chaque test échoué.

### Reproduire et déboguer les builds Docker en local

Si une étape de build Docker échoue dans la CI :
```bash
# Valider la configuration Compose
docker compose --env-file .env.example config

# Reconstruire un service spécifique avec affichage verbeux
docker compose build --progress=plain <service-name>

# Exemple pour product-service :
docker compose build --progress=plain product-service
```

### Paramétrage recommandé de la protection de branche sur GitHub

> **Important :** La protection de branche doit être configurée dans les paramètres de votre dépôt GitHub (Settings > Branches) :

1. Définir une règle de protection de branche sur `main` :
   - Cocher **"Require a pull request before merging"** (1 approbation requise).
   - Cocher **"Require status checks to pass before merging"** :
     - Ajouter les statuts requis :
       - `Quality & Security Checks`
       - `Build & Test (auth-service)`
       - `Build & Test (api-gateway)`
       - `Build & Test (product-service)`
       - `Build & Test (customer-service)`
       - `Build & Test (sales-service)`
       - `Docker Compose Validation & Smoke Test`
   - Cocher **"Require branches to be up to date before merging"**.
   - Cocher **"Require linear history"** (évite les commits de merge désordonnés).
   - Interdire les suppressions et les pushs forcés (**"Do not allow force pushes"**).
