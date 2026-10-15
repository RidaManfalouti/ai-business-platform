# Product Service (`services/product-service`)

Microservice de gestion du catalogue de produits pour la plateforme **ai-business-platform**.

---

## 1. Caractéristiques techniques

- **Java** : 21 (OpenJDK)
- **Spring Boot** : 4.1.1
- **Base de données** : PostgreSQL (`product_db`)
- **ORM** : Spring Data JPA / Hibernate
- **Migrations DB** : Flyway (`spring-boot-starter-flyway` 4.1.1)
- **Documentation API** : SpringDoc OpenAPI 3 / Swagger UI (`3.1.1`)
- **Port d'écoute** : `8082` (configurable via `PORT`)

---

## 2. Endpoints REST (`/api/products`)

| Méthode | Endpoint | Description | Code Succès | Erreurs possibles |
|---|---|---|---|---|
| `POST` | `/api/products` | Créer un nouveau produit | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/products/{id}` | Consulter un produit par ID | `200 OK` | `404 Not Found` |
| `GET` | `/api/products` | Lister avec pagination et tri (`?page=0&size=10&sort=name,asc`) | `200 OK` | - |
| `GET` | `/api/products/search?name={q}` | Rechercher des produits par nom | `200 OK` | - |
| `PUT` | `/api/products/{id}` | Mettre à jour un produit | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/products/{id}` | Supprimer un produit | `204 No Content` | `404 Not Found` |

---

## 3. Endpoints de Surveillance et Documentation

- **Santé Actuator** : `GET http://localhost:8082/actuator/health` → `{"status": "UP"}`
- **OpenAPI JSON** : `GET http://localhost:8082/v3/api-docs`
- **Swagger UI** : `http://localhost:8082/swagger-ui.html`

---

## 4. Règles de validation métier

- `name` : obligatoire, non vide, max 100 caractères, unique dans le catalogue (`409 Conflict` en cas de doublon).
- `price` : obligatoire, positif ou nul (`>= 0.00`).
- `stockQuantity` : obligatoire, entier non négatif (`>= 0`).
- `category` : obligatoire, non vide, max 50 caractères.
- `description` : optionnel, max 1000 caractères.

---

## 5. Commandes de démarrage et tests

```bash
# Se placer dans le répertoire du module
cd ~/Projects/ai-business-platform/services/product-service

# Exécuter les tests unitaires et d'intégration (22 tests)
mvn test

# Démarrer le service en local
mvn spring-boot:run

# Démarrer avec des variables d'environnement personnalisées
PORT=8082 POSTGRES_PORT=5433 POSTGRES_DB=product_db mvn spring-boot:run
```
