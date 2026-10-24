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

## 6. Guide de Test Rapide (Smoke Test & RBAC)

Toutes les requêtes s'effectuent à travers l'**API Gateway** (`http://localhost:8080`) :

### A. Vérifier le rejet d'un accès anonyme (HTTP 401 Unauthorized)
```bash
curl -i -s "http://localhost:8080/api/products"
```
*Réponse attendue : `HTTP/1.1 401 Unauthorized` avec le corps standardisé `{"error":"UNAUTHORIZED","message":"..."}`.*

### B. Inscription d'un utilisateur standard (Rôle `USER`)
```bash
USER_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"dev_user","email":"dev@example.com","password":"Password123!"}' \
  | jq -r '.token')
echo "User Token: $USER_TOKEN"
```

### C. Vérifier la protection d'un endpoint privilégié (HTTP 403 Forbidden)
Une tentative de création de produit dans le catalogue par un compte utilisateur standard `USER` est bloquée :
```bash
curl -i -s -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"name":"ThinkPad X1","description":"Ultrabook","price":1500.00,"stockQuantity":10,"category":"PC"}'
```
*Réponse attendue : `HTTP/1.1 403 Forbidden` avec `{"error":"FORBIDDEN","message":"Access Denied: insufficient permissions"}`.*

### D. Connexion d'un compte Administrateur (Rôle `ADMIN`)
En utilisant les identifiants administrateur amorcés au démarrage (`INITIAL_ADMIN_*` configuré dans `.env` / `docker-compose.yml`) :
```bash
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"AdminSecurePassword123!"}' \
  | jq -r '.token')
echo "Admin Token: $ADMIN_TOKEN"
```

### E. Création d'un produit par l'administrateur (Succès HTTP 201)
```bash
PRODUCT_ID=$(curl -s -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"ThinkPad X1","description":"Ultrabook","price":1500.00,"stockQuantity":10,"category":"PC"}' \
  | jq -r '.id')
echo "Product ID: $PRODUCT_ID"
```

### F. Opérations métier par l'utilisateur standard (Client, Vente & Réservation)
L'utilisateur standard `USER` peut créer un client, consulter le catalogue et passer commande :
```bash
# 1. Créer un client
CUSTOMER_ID=$(curl -s -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{"firstName":"Jean","lastName":"Dupont","email":"jean@example.com","company":"Acme Corp"}' \
  | jq -r '.id')

# 2. Créer une vente de 2 unités avec clé d'idempotence
SALE_ID=$(curl -s -X POST "http://localhost:8080/api/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Idempotency-Key: CMD-DEV-001" \
  -d "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":2}]}" \
  | jq -r '.id')
```
*Le jeton Bearer de l'utilisateur est propagé de manière sécurisée de `sales-service` vers `product-service` et `customer-service` ; le stock passe de 10 à 8 de façon atomique.*

### G. Vérifier le rejet d'une survente (Overselling)
```bash
# Tenter de réserver 50 unités alors que 8 sont disponibles
curl -s -X POST "http://localhost:8080/api/sales" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{\"customerId\":$CUSTOMER_ID,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":50}]}"
```
*Réponse attendue : `HTTP/1.1 409 Conflict` (Stock insuffisant).*

### H. Annulation de commande et compensation Saga
```bash
curl -s -X POST "http://localhost:8080/api/sales/$SALE_ID/cancel" \
  -H "Authorization: Bearer $USER_TOKEN"
```
*Le stock du produit est automatiquement restauré à 10 par la Saga compensatoire.*

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

---

## 10. Sécurité & Contrôle d'Accès Basé sur les Rôles (RBAC)

La plateforme met en œuvre un contrôle d'accès basé sur les rôles (**RBAC**) rigoureux, conforme aux exigences de sécurité d'entreprise et aux principes de moindre privilège et de défense en profondeur (*Defense in Depth*).

### 10.1. Modèle de Rôles

Le modèle est minimal, explicite et sécurisé :
- **`ROLE_USER`** : Rôle standard par défaut accordé à tout nouvel utilisateur lors de son inscription. Permet d'effectuer des opérations métier normales : gestion de son profil personnel, consultation du catalogue de produits, gestion des fiches clients (CRM) et passage de commandes de vente avec réservation atomique de stock.
- **`ROLE_ADMIN`** : Rôle à privilèges élevés. Requis pour les opérations administratives : gestion du catalogue produit (création, mise à jour, suppression de produits), suppression définitive de fiches clients, consultation de l'ensemble des utilisateurs et modification de leurs rôles.

#### Persistance et Intégrité en Base de Données
- Les rôles sont persistés dans la table `users` de la base `auth_db`.
- Une migration Flyway (`V1__create_users_table.sql`) garantit la rétrocompatibilité :
  - La colonne `role VARCHAR(20) NOT NULL DEFAULT 'USER'` est ajoutée de manière idempotente.
  - Tout utilisateur préexistant sans rôle se voit assigner `USER` (moindre privilège strict).
  - Une contrainte SQL `CHECK (role IN ('USER', 'ADMIN'))` empêche toute valeur incohérente en base.
- **Aucune confiance dans les entrées clientes** :
  - Le DTO d'enregistrement `RegisterRequest` n'expose aucun attribut `role`.
  - L'API Gateway filtre et supprime automatiquement tout en-tête d'autorité forgé par le client (`X-Role`, `X-Roles`, `X-User-Role`, `X-Authorities`) via `AuthorizationHeaderForwardingFilter`.
  - Chaque microservice valide la signature cryptographique du jeton JWT et en extrait les autorités indépendamment de la passerelle.

---

### 10.2. Matrice d'Autorisation

| Service | Méthode & Endpoint | Rôle Requis | Justification Métier |
| :--- | :--- | :--- | :--- |
| **auth-service** | `POST /api/auth/register` | Public (Anonyme) | Inscription d'un nouveau compte utilisateur |
| **auth-service** | `POST /api/auth/login` | Public (Anonyme) | Authentification et obtention du JWT |
| **auth-service** | `GET /api/users/me` | `USER`, `ADMIN` | Consultation du profil de l'utilisateur connecté |
| **auth-service** | `GET /api/users` | `ADMIN` | Supervision et audit de l'annuaire des utilisateurs |
| **auth-service** | `PATCH /api/users/{id}/role` | `ADMIN` | Promotion ou rétrogradation sécurisée d'un utilisateur |
| **product-service** | `GET /api/products` & `/api/products/**` | `USER`, `ADMIN` | Consultation du catalogue de produits |
| **product-service** | `POST /api/products` | `ADMIN` | Ajout d'une nouvelle référence au catalogue |
| **product-service** | `PUT /api/products/**` | `ADMIN` | Modification de fiche produit, prix ou inventaire |
| **product-service** | `DELETE /api/products/**` | `ADMIN` | Retrait d'une référence du catalogue |
| **product-service** | `POST /api/products/reservations` | `USER`, `ADMIN` | Réservation atomique de stock (workflow de vente) |
| **product-service** | `DELETE /api/products/reservations/**` | `USER`, `ADMIN` | Annulation et compensation de réservation de stock |
| **customer-service** | `GET /api/customers` & `/api/customers/**` | `USER`, `ADMIN` | Consultation des fiches clients |
| **customer-service** | `POST /api/customers` | `USER`, `ADMIN` | Création d'une nouvelle fiche client |
| **customer-service** | `PUT /api/customers/**` | `USER`, `ADMIN` | Mise à jour des informations client |
| **customer-service** | `DELETE /api/customers/**` | `ADMIN` | Suppression définitive d'une fiche client |
| **sales-service** | `GET /api/sales` & `/api/sales/**` | `USER`, `ADMIN` | Consultation de l'historique des commandes |
| **sales-service** | `POST /api/sales` | `USER`, `ADMIN` | Passation de commande avec réservation de stock |
| **sales-service** | `POST /api/sales/{id}/cancel` | `USER`, `ADMIN` | Annulation de commande et déclenchement de la Saga compensatoire |

> **Propagation du Jeton Bearer :**
> Lors de la création ou de l'annulation d'une vente, `sales-service` relaie de manière transparente l'en-tête `Authorization: Bearer <token>` du client aux services `product-service` et `customer-service` via un intercepteur `RestClient`. Comme les endpoints de réservation et de vérification client autorisent `USER` et `ADMIN`, le flux commercial s'exécute sans friction avec l'identité de l'utilisateur initiateur.

---

### 10.3. Attribution et Modification Sécurisée des Rôles

1. **Amorçage sécurisé de l'administrateur initial (*Zero Hardcoding*) :**
   Aucun identifiant administrateur n'est codé en dur dans les sources. L'amorce initiale s'effectue au démarrage d'`auth-service` via des variables d'environnement :
   ```env
   INITIAL_ADMIN_ENABLED=true
   INITIAL_ADMIN_EMAIL=admin@example.com
   INITIAL_ADMIN_PASSWORD=VotreMotDePasseComplexe!
   INITIAL_ADMIN_USERNAME=admin
   ```
   La classe `AdminUserInitializer` vérifie si le compte existe : s'il est absent, il est créé avec `role = 'ADMIN'` ; s'il existe avec un autre rôle, il est promu. Si `INITIAL_ADMIN_ENABLED=false` (défaut de production), aucun compte n'est automatiquement créé.

2. **Délégation d'administration via l'API :**
   Un administrateur authentifié peut modifier le rôle de n'importe quel utilisateur :
   ```bash
   curl -s -X PATCH "http://localhost:8080/api/users/42/role" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"role":"ADMIN"}'
   ```
   Toute tentative d'appel à cet endpoint par un compte non-administrateur retourne un code `403 Forbidden`.

3. **Procédure d'urgence (*Break-glass*) :**
   En cas de perte d'accès administrateur en environnement opérationnel, une promotion directe peut être exécutée via SQL sécurisé dans `auth_db` :
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'ops-lead@example.com';
   ```

---

### 10.4. Structure des Réclamations JWT (Claims)

Les jetons émis par `auth-service` utilisent le standard JSON Web Token (RFC 7519) signé en HMAC-SHA256 :

```json
{
  "sub": "user@example.com",
  "roles": [
    "ROLE_USER"
  ],
  "iat": 1729790000,
  "exp": 1729876400
}
```

#### Extraction et Normalisation des Autorités
Chaque microservice ressource (`product-service`, `customer-service`, `sales-service`) intègre la classe `JwtRoleConverter` :
- Extrait la liste de chaînes du claim `roles` (ou `authorities` en fallback).
- Préfixe automatiquement chaque élément par `ROLE_` s'il ne l'est pas déjà, assurant l'interopérabilité directe avec les expressions Spring Security `@PreAuthorize("hasRole('ADMIN')")` et `.hasRole("ADMIN")`.
- **Tolérance aux pannes et sécurité :** Si le claim `roles` est absent, vide ou d'un type inattendu, le convertisseur retourne une liste vide d'autorités sans lever d'exception, refusant l'accès aux routes protégées de manière sécurisée (*fail-safe*).

---

### 10.5. Test des Réponses HTTP 401 Unauthorized vs 403 Forbidden

Les microservices et la passerelle renvoient des charges utiles JSON standardisées :

#### HTTP 401 Unauthorized
Renvoyé lorsque le client n'est pas authentifié :
- Jeton `Authorization` manquant
- Jeton mal formé ou signature altérée
- Jeton expiré

```json
{
  "error": "UNAUTHORIZED",
  "message": "Full authentication is required to access this resource"
}
```

*Exemple de commande pour tester le 401 :*
```bash
curl -i -X GET "http://localhost:8080/api/products"
# HTTP/1.1 401 Unauthorized
```

#### HTTP 403 Forbidden
Renvoyé lorsque le client est correctement authentifié mais ne détient pas l'autorité requise pour l'opération :

```json
{
  "error": "FORBIDDEN",
  "message": "Access Denied: insufficient permissions"
}
```

*Exemple de commande pour tester le 403 :*
```bash
# Tentative de suppression d'un client par un compte USER :
curl -i -X DELETE "http://localhost:8080/api/customers/1" \
  -H "Authorization: Bearer $USER_TOKEN"
# HTTP/1.1 403 Forbidden
```

---

### 10.6. Migration et Considérations de Rétrocompatibilité

1. **Migration de la base de données :**
   - La migration `V1__create_users_table.sql` dans `auth-service` utilise `IF NOT EXISTS` et des blocs conditionnels idempotents. Elle est compatible avec les installations neuves ainsi qu'avec les bases préexistantes sans perte de données.
   - Les utilisateurs existants reçoivent le rôle `USER`. Aucun utilisateur préexistant n'obtient de privilèges administrateur par défaut.

2. **Transition des Jetons JWT :**
   - Les anciens jetons JWT émis avant l'implémentation du RBAC ne comportent pas le claim `roles`.
   - Les services ressources continuent à valider cryptographiquement ces jetons, mais leur attribuent zéro autorité. Les détenteurs d'anciens jetons sont invités à se reconnecter via `POST /api/auth/login` pour obtenir un jeton enrichi de leur rôle.
