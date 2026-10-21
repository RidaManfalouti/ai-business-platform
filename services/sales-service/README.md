# Sales Service (`services/sales-service`)

Microservice de gestion des commandes et ventes pour la plateforme **ai-business-platform**.

---

## 1. Caractéristiques techniques

- **Java** : 21 (OpenJDK)
- **Spring Boot** : 4.1.1
- **Base de données** : PostgreSQL (`sales_db`)
- **ORM** : Spring Data JPA / Hibernate
- **Migrations DB** : Flyway (`spring-boot-starter-flyway` 4.1.1)
- **Communication interservices** : HTTP via `RestClient` avec timeouts explicites
- **Documentation API** : SpringDoc OpenAPI 3 / Swagger UI (`3.1.1`)
- **Port d'écoute** : `8084` (configurable via `PORT`)

---

## 2. Endpoints REST (`/api/sales`)

| Méthode | Endpoint | Description | Code Succès | Erreurs possibles |
|---|---|---|---|---|
| `POST` | `/api/sales` | Créer une nouvelle vente avec snapshot prix et recalcul serveur | `201 Created` | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `502 Bad Gateway`, `503 Service Unavailable` |
| `GET` | `/api/sales/{id}` | Consulter une vente par ID avec le détail de ses lignes | `200 OK` | `404 Not Found` |
| `GET` | `/api/sales` | Lister les ventes avec pagination et tri (`?page=0&size=10&sort=createdAt,desc`) | `200 OK` | - |
| `GET` | `/api/sales/customer/{customerId}` | Consulter l'historique des ventes d'un client avec pagination | `200 OK` | - |
| `PATCH` | `/api/sales/{id}/status` | Modifier le statut selon les transitions autorisées | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict` |

---

## 3. Endpoints de Surveillance et Documentation

- **Santé Actuator** : `GET http://localhost:8084/actuator/health` → `{"status": "UP"}`
- **OpenAPI JSON** : `GET http://localhost:8084/v3/api-docs`
- **Swagger UI** : `http://localhost:8084/swagger-ui.html`

---

## 4. Règles métier et intégrité

- **Gestion des montants monétaires** : Utilisation stricte de `BigDecimal` avec arrondi `HALF_UP` à 2 décimales. Aucun calcul monétaire en `double`.
- **Recalcul serveur & snapshot** : Le client transmet uniquement `customerId`, les couples `(productId, quantity)` et optionnellement une `reference`. Le serveur interroge `Product Service`, fige le nom et le prix unitaire au moment de la vente (`product_name_snapshot`, `unit_price`), calcule les sous-totaux de ligne et le total général. Les totaux envoyés par le client sont ignorés.
- **Machine à états des statuts** :
  - `PENDING` → `CONFIRMED` ou `CANCELLED`
  - `CONFIRMED` → `COMPLETED` ou `CANCELLED`
  - `COMPLETED` : état terminal non modifiable (`409 Conflict`)
  - `CANCELLED` : état terminal non modifiable (`409 Conflict`)
- **Stratégie d'idempotence et anti-doublon** :
  - Le client peut transmettre une référence métier ou clé d'idempotence (`reference`).
  - En son absence, une référence unique au format `SAL-YYYYMMDD-XXXXXXXX` est générée.
  - La contrainte d'unicité PostgreSQL `idx_sales_reference` protège contre les requêtes concurrentes.
- **Gestion des stocks & limitation connue** :
  - Le `Product Service` actuel ne fournit aucun endpoint de réservation ou de décrémentation atomique de stock.
  - La vérification du stock (`stockQuantity >= quantity`) effectuée par Sales Service est donc **consultative (read-only)**.
  - Elle n'assure pas une transaction distribuée atomique. L'évolution préconisée consiste à introduire dans `Product Service` un endpoint dédié du type `POST /api/products/{id}/reserve` ou `POST /api/products/{id}/decrement-stock`.

---

## 5. Commandes de démarrage et tests

```bash
# Se placer dans le répertoire du module
cd ~/Projects/ai-business-platform/services/sales-service

# Exécuter l'ensemble des tests unitaires et d'intégration (54 tests)
mvn test

# Démarrer le service en local (port 8084)
mvn spring-boot:run

# Démarrer avec des variables d'environnement personnalisées
PORT=8084 POSTGRES_PORT=5433 POSTGRES_DB=sales_db mvn spring-boot:run
```
