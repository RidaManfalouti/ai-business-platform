# API Gateway (`services/api-gateway`)

Composant central d'entrée (Reverse Proxy & Passerelle de Routage) de la plateforme **ai-business-platform**, développé avec **Spring Cloud Gateway (WebFlux / Netty)**.

---

## 1. Caractéristiques techniques

- **Java** : 21 (OpenJDK)
- **Spring Boot** : 4.1.1
- **Spring Cloud** : 2025.1.3 (`spring-cloud-dependencies`)
- **Starter Gateway** : `spring-cloud-starter-gateway-server-webflux` (version 5.0.3)
- **Port d'écoute par défaut** : `8080` (surchargeable via `PORT`)

---

## 2. Routage et Microservices

| Service Cible | Préfixe de Route | Destination par défaut | Variable d'environnement |
|---|---|---|---|
| **Auth Service** (Auth) | `/api/auth/**` | `http://localhost:8081` | `AUTH_SERVICE_URL` |
| **Auth Service** (Profil) | `/api/users/**` | `http://localhost:8081` | `AUTH_SERVICE_URL` |
| **Customer Service** | `/api/customers/**` | `forward:/fallback/service-unavailable` | `CUSTOMER_SERVICE_URL` |
| **Product Service** | `/api/products/**` | `forward:/fallback/service-unavailable` | `PRODUCT_SERVICE_URL` |
| **Sales Service** | `/api/sales/**` | `forward:/fallback/service-unavailable` | `SALES_SERVICE_URL` |
| **Analytics Service** | `/api/analytics/**` | `forward:/fallback/service-unavailable` | `ANALYTICS_SERVICE_URL` |

> [!NOTE]
> Les routes vers les services futurs sont préparées. Lorsqu'un service n'est pas encore déployé, la passerelle redirige localement la requête vers le contrôleur interne `/fallback/service-unavailable` qui retourne un statut **HTTP 503 SERVICE_UNAVAILABLE** sans émettre d'appel réseau externe infructueux.

---

## 3. Gestion des Jetons JWT (`Authorization`)

- L'API Gateway implémente un filtre réactif global (`AuthorizationHeaderForwardingFilter`) garantissant que l'en-tête standard `Authorization: Bearer <token>` est fidèlement transmis au microservice cible.
- **Principe de responsabilité unique** : La Gateway ne réalise pas de seconde authentification concurrente et ne stocke pas de secret JWT. La vérification cryptographique et l'autorisation métier restent la prérogative du microservice responsable (`auth-service`).

---

## 4. Gestion des Erreurs et Indisponibilité

- Un gestionnaire d'exception réactif global (`GlobalErrorWebExceptionHandler`) intercepte les interruptions de connexion (`ConnectException`, timeout, service non joignable).
- En cas de panne d'un microservice cible, la Gateway retourne automatiquement un format JSON standardisé avec le code **HTTP 503** :
  ```json
  {
    "timestamp": "2026-10-15T18:00:00.000Z",
    "status": 503,
    "error": "SERVICE_UNAVAILABLE",
    "message": "Le service de destination est actuellement indisponible ou injoignable.",
    "path": "/api/auth/login"
  }
  ```

---

## 5. Endpoints de Surveillance

- **Santé de la passerelle** : `GET http://localhost:8080/actuator/health` → `{"status": "UP"}`
- **Informations Actuator** : `GET http://localhost:8080/actuator/info`

---

## 6. Commandes utiles

### Exécuter les tests unitaires et d'intégration :
```bash
cd services/api-gateway
mvn test
```

### Démarrer le service en local :
```bash
cd services/api-gateway
mvn spring-boot:run
```

### Démarrer avec une surcharge de port ou d'URL :
```bash
PORT=8080 AUTH_SERVICE_URL=http://localhost:8081 mvn spring-boot:run
```
