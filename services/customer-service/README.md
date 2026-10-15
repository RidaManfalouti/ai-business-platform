# Customer Service (`services/customer-service`)

Microservice de gestion des clients pour la plateforme **ai-business-platform**.

---

## 1. Caractéristiques techniques

- **Java** : 21 (OpenJDK)
- **Spring Boot** : 4.1.1
- **Base de données** : PostgreSQL (`customer_db`)
- **ORM** : Spring Data JPA / Hibernate
- **Migrations DB** : Flyway (`spring-boot-starter-flyway` 4.1.1)
- **Documentation API** : SpringDoc OpenAPI 3 / Swagger UI (`3.1.1`)
- **Surveillance** : Spring Boot Actuator (`/actuator/health`)
- **Port d'écoute** : `8083` (configurable via `PORT` ou `SERVER_PORT`)

---

## 2. Endpoints REST (`/api/customers`)

| Méthode | Endpoint | Description | Code Succès | Erreurs possibles |
|---|---|---|---|---|
| `POST` | `/api/customers` | Créer un nouveau client | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/customers/{id}` | Consulter un client par ID | `200 OK` | `404 Not Found` |
| `GET` | `/api/customers` | Lister les clients (pagination & tri) | `200 OK` | - |
| `GET` | `/api/customers/search?name={query}` | Rechercher par prénom ou nom (insensible à la casse) | `200 OK` | `400 Bad Request` (si vide) |
| `GET` | `/api/customers/search?email={query}` | Rechercher par e-mail (insensible à la casse) | `200 OK` | `400 Bad Request` (si vide) |
| `PUT` | `/api/customers/{id}` | Mettre à jour les informations d'un client | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/customers/{id}` | Supprimer un client | `204 No Content` | `404 Not Found` |

### Paramètres de pagination et tri
- `page` : numéro de la page (commence à 0, défaut : 0).
- `size` : taille de la page (défaut : 10).
- `sort` : champ et direction du tri (ex: `lastName,asc`, `createdAt,desc`).

---

## 3. Endpoints de Surveillance et Documentation

- **Santé Actuator** : `GET http://localhost:8083/actuator/health` → `{"status": "UP"}`
- **OpenAPI JSON** : `GET http://localhost:8083/v3/api-docs`
- **Swagger UI** : `http://localhost:8083/swagger-ui.html`

---

## 4. Modèle de données & Règles de validation

- `firstName` : obligatoire, non vide, max 50 caractères.
- `lastName` : obligatoire, non vide, max 50 caractères.
- `email` : obligatoire, format valide, max 100 caractères, unique en base et au niveau applicatif (`409 Conflict` en cas de doublon), indexé.
- `phone` : optionnel, max 20 caractères.
- `company` : optionnel, max 100 caractères.
- `createdAt` : date et heure de création (gérée automatiquement, non modifiable).
- `updatedAt` : date et heure de dernière modification (mise à jour lors d'un `PUT`).

---

## 5. Intégration API Gateway

L'API Gateway (port `8080`) achemine les requêtes :
- Préfixe `/api/customers/**` → `http://localhost:8083/api/customers/**`
- Configuration dynamique via variable d'environnement : `CUSTOMER_SERVICE_URL` (défaut : `http://localhost:8083`).
- Préservation des fallbacks `503 Service Unavailable` pour les services encore non implémentés (`sales`, `analytics`).

---

## 6. État de la sécurité et limitations actuelles

- **Transmission JWT par la Gateway** : L'API Gateway transmet fidèlement l'en-tête HTTP `Authorization: Bearer <token>`.
- **Validation cryptographique locale non implémentée** : À ce stade de l'architecture, `customer-service` ne possède pas de filtre Spring Security pour vérifier la signature ou la validité du JWT émis par `auth-service`.
- **État actuel** : Les endpoints de `customer-service` sont accessibles sans authentification directe sur le port interne 8083.
- **Évolution prévue** : Introduction d'une couche d'authentification partagée (bibliothèque commune de validation JWT ou filtre d'authentification centralisé dans l'API Gateway) pour sécuriser l'ensemble des services en aval.

---

## 7. Commandes de démarrage et tests

```bash
# Se placer dans le répertoire du module
cd ~/Projects/ai-business-platform/services/customer-service

# Exécuter l'ensemble des tests (24 tests : 1 contexte, 11 service Mockito, 12 contrôleur MockMvc)
mvn test

# Démarrer le microservice en local (port 8083)
mvn spring-boot:run

# Démarrer avec des variables d'environnement personnalisées
PORT=8083 POSTGRES_PORT=5433 POSTGRES_DB=customer_db mvn spring-boot:run
```
