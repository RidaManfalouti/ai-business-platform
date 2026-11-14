# AI Service — AI Business Copilot

Service microservice Spring Boot 3 / Java 21 dédié à l'assistant d'entreprise intelligent (AI Copilot) pour la plateforme SaaS multi-tenant `ai-business-platform`.

## Fonctionnalités
- Dialogue en langage naturel sur les données d'entreprise
- Abstraction indépendante des fournisseurs LLM (Google Gemini, OpenAI, Mock local pour tests)
- Isolation stricte multi-locataire garantie par JWT et validation cryptographique
- Exécution sécurisée d'outils métiers (KPIs, tendances de vente, alertes stock faible, top produits, clients)
- Persistance des conversations et des messages en PostgreSQL
- Rate limiting par locataire/utilisateur
- Traçabilité distribuée avec X-Correlation-ID
- OpenAPI Swagger documentation et métriques Prometheus

## Configuration & Variables d'Environnement

| Variable | Description | Valeur par défaut |
| :--- | :--- | :--- |
| `PORT` | Port d'écoute HTTP du service | `8086` |
| `POSTGRES_HOST` | Hôte PostgreSQL | `localhost` |
| `POSTGRES_PORT` | Port PostgreSQL | `5433` (dev) / `5432` (docker) |
| `POSTGRES_DB` | Nom de la base de données | `ai_db` |
| `POSTGRES_USER` | Utilisateur PostgreSQL | `ai_platform` |
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL | `0000` |
| `JWT_SECRET` | Clé secrète HMAC-SHA256 (Base64) | *Requise* |
| `AI_PROVIDER` | Fournisseur LLM actif (`mock`, `gemini`, `openai`) | `mock` |
| `GEMINI_API_KEY` | Clé d'API Google Gemini | *Optionnelle en mode mock* |
| `GEMINI_MODEL` | Modèle Gemini | `gemini-1.5-flash` |
| `OPENAI_API_KEY` | Clé d'API OpenAI | *Optionnelle en mode mock* |
| `OPENAI_MODEL` | Modèle OpenAI | `gpt-4o-mini` |
| `AI_RATE_LIMIT_RPM` | Limite de requêtes par minute / utilisateur | `30` |

## Endpoints REST
- `POST /api/ai/chat` : Envoi d'un message à l'assistant
- `GET /api/ai/conversations` : Liste paginée des conversations de l'utilisateur
- `GET /api/ai/conversations/{id}` : Détail des messages d'une conversation
- `DELETE /api/ai/conversations/{id}` : Suppression d'une conversation
- `/actuator/health` : Bilan de santé liveness & readiness
- `/v3/api-docs` & `/swagger-ui.html` : Documentation OpenAPI interactive
