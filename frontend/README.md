# Frontend - AI Business Platform

Application web moderne, réactive et sécurisée de gestion commerciale et supervision des microservices pour **AI Business Platform**.

Développée avec **React 18**, **TypeScript**, **Vite**, **Tailwind CSS**, **React Router v6**, **TanStack Query v5**, **Axios**, **React Hook Form**, **Zod** et **Lucide React**.

---

## 1. Prérequis & Environnement

* **Node.js** : `>= 20.x` (testé et validé avec Node.js `v26.9.0`)
* **npm** : `>= 10.x` (testé avec npm `11.19.1`)
* **Passerelle API (API Gateway)** : Doit être en cours d'exécution sur `http://localhost:8080`.

---

## 2. Configuration & Variables d'Environnement

L'application communique exclusivement avec la passerelle API (`api-gateway`) qui dispatche les requêtes vers les microservices sous-jacents (`auth-service`, `product-service`, `customer-service`, `sales-service`).

Créez un fichier `.env` dans le dossier `frontend/` (un modèle est fourni dans `.env.example`) :

```bash
cp .env.example .env
```

### Variables disponibles

| Variable | Description | Valeur par défaut |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | Point d'entrée unique de la passerelle API Spring Cloud Gateway | `http://localhost:8080` |
| `VITE_API_TIMEOUT_MS` | Délai d'expiration maximal des requêtes HTTP (millisecondes) | `15000` (15 secondes) |

> **Sécurité** : Ne committez jamais de fichier `.env` contenant des clés sensibles. Le fichier `.gitignore` racine ignore automatiquement `.env` et `.env.*`.

---

## 3. Installation et Démarrage Local

### Installation des dépendances

```bash
cd frontend
npm install
```

### Lancement du serveur de développement

```bash
npm run dev
```

L'application est accessible par défaut sur `http://localhost:5173`.

### Compilation de production

```bash
npm run build
```

Les fichiers statiques optimisés sont générés dans le dossier `dist/`.

### Prévisualisation de la version de production

```bash
npm run preview
```

---

## 4. Tests et Qualité de Code

### Exécution des tests unitaires (Vitest + Testing Library)

```bash
npm test
```

### Vérification des types TypeScript

```bash
npm run typecheck
```

---

## 5. Fonctionnalités et Écrans

1. **Authentification & Session** :
   * Inscription (`/register`) avec validation stricte conforme aux contraintes backend (`username` 3-50 car., `email` max 100 car., `password` 8-72 car.).
   * Connexion (`/login`) avec persistance sécurisée du JWT et redirection automatique.
   * Déconnexion client (suppression du token en mémoire locale).
   * Gestion de l'expiration du token et interception globale des erreurs 401.

2. **Tableau de Bord (`/`)** :
   * Métriques KPI réelles calculées strictement à partir des réponses des microservices (Catalogue, Clients, Commandes).
   * État transparent sur `/api/analytics/**` (réponse 503 contrôlée documentée).
   * Synthèse des commandes récentes et répartition des statuts (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`).

3. **Catalogue de Produits & Stock (`/products`)** :
   * Liste paginée des produits avec pagination 0-indexed Spring Boot.
   * Recherche en direct avec debounce.
   * Indicateurs visuels de niveau de stock (Disponible, Stock faible $\le 5$, Rupture de stock $= 0$).
   * Ajout, modification et suppression de produits (réservés à `ROLE_ADMIN`).

4. **Gestion des Clients (`/customers`)** :
   * Recherche multicritère par nom ou adresse email via l'endpoint dédié `/api/customers/search`.
   * Création et mise à jour de fiches clients.
   * Consultation de l'historique d'achats du client (`/api/sales/customer/{id}`).
   * Suppression sécurisée de fiche client (réservée à `ROLE_ADMIN`).

5. **Gestion des Ventes & Commandes (`/sales` & `/sales/:id`)** :
   * Création de vente avec sélection de client, ajout dynamique de lignes de commande, calcul automatique des sous-totaux.
   * Génération d'une **clé d'idempotence** unique par tentative de commande (`CMD-YYYYMMDD-XXXXXX`), réutilisée lors des réessais de la même tentative pour prévenir les doublons.
   * Validation préventive du stock disponible avant soumission (autorité finale assurée par le backend).
   * Machine à états de commande : `PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `COMPLETED` ou `CANCELLED`.
   * Annulation de vente avec notification de compensation Saga automatique du stock réservé.

6. **Administration, Sagas & Réconciliation (`/admin/sagas` & `/admin/users`)** *(ROLE_ADMIN)* :
   * Supervision des orchestrations Saga distribuées (SagaId, type, statut, historique des tentatives, journal d'événements).
   * Déclenchement d'un réessai manuel de compensation (`/api/sales/sagas/{id}/retry`).
   * Résolution administrative manuelle avec note d'audit (`/api/sales/sagas/{id}/resolve`).
   * Déclenchement à la demande de l'audit de réconciliation de stock (`/api/sales/reconciliation/run`) et consultation du dernier rapport.
   * Gestion et promotion des rôles utilisateurs (`USER` $\leftrightarrow$ `ADMIN`).

---

## 6. Architecture & Sécurité

### Matrice des endpoints consommés

| Ressource | Méthode HTTP | Route API Gateway | Rôle minimal requis |
| :--- | :--- | :--- | :--- |
| **Authentification** | `POST` | `/api/auth/register` | Public |
| **Authentification** | `POST` | `/api/auth/login` | Public |
| **Profil utilisateur** | `GET` | `/api/users/me` | `ROLE_USER` |
| **Gestion des utilisateurs**| `GET` | `/api/users` | `ROLE_ADMIN` |
| **Modification de rôle** | `PATCH` | `/api/users/{id}/role` | `ROLE_ADMIN` |
| **Catalogue produits** | `GET` | `/api/products` | `ROLE_USER` |
| **Fiche produit** | `GET` | `/api/products/{id}` | `ROLE_USER` |
| **Création produit** | `POST` | `/api/products` | `ROLE_ADMIN` |
| **Mise à jour produit** | `PUT` | `/api/products/{id}` | `ROLE_ADMIN` |
| **Suppression produit** | `DELETE` | `/api/products/{id}` | `ROLE_ADMIN` |
| **Liste des clients** | `GET` | `/api/customers` | `ROLE_USER` |
| **Recherche clients** | `GET` | `/api/customers/search` | `ROLE_USER` |
| **Création client** | `POST` | `/api/customers` | `ROLE_USER` |
| **Mise à jour client** | `PUT` | `/api/customers/{id}` | `ROLE_USER` |
| **Suppression client** | `DELETE` | `/api/customers/{id}` | `ROLE_ADMIN` |
| **Liste des ventes** | `GET` | `/api/sales` | `ROLE_USER` |
| **Détail vente** | `GET` | `/api/sales/{id}` | `ROLE_USER` |
| **Ventes par client** | `GET` | `/api/sales/customer/{id}` | `ROLE_USER` |
| **Création de vente** | `POST` | `/api/sales` | `ROLE_USER` |
| **Transition de statut** | `PATCH` | `/api/sales/{id}/status` | `ROLE_USER` |
| **Annulation de vente** | `POST` | `/api/sales/{id}/cancel` | `ROLE_USER` |
| **Orchestrations Saga** | `GET` | `/api/sales/sagas` | `ROLE_ADMIN` |
| **Détail Saga & Logs** | `GET` | `/api/sales/sagas/{id}` | `ROLE_ADMIN` |
| **Réessai Saga** | `POST` | `/api/sales/sagas/{id}/retry` | `ROLE_ADMIN` |
| **Résolution Saga** | `POST` | `/api/sales/sagas/{id}/resolve` | `ROLE_ADMIN` |
| **Rapport Réconciliation** | `GET` | `/api/sales/reconciliation/latest`| `ROLE_ADMIN` |
| **Lancement Réconciliation**| `POST` | `/api/sales/reconciliation/run` | `ROLE_ADMIN` |
| **Dashboard Analytique**| `GET` | `/api/analytics/dashboard` | `ROLE_USER` |
| **Indicateurs KPI** | `GET` | `/api/analytics/kpi` | `ROLE_USER` |
| **Tendances Ventes & CA**| `GET` | `/api/analytics/trends` | `ROLE_USER` |
| **Meilleures Ventes** | `GET` | `/api/analytics/top-products` | `ROLE_USER` |
| **Distribution Statuts** | `GET` | `/api/analytics/status-distribution` | `ROLE_USER` |
| **Alertes Stock Faible** | `GET` | `/api/analytics/low-stock` | `ROLE_USER` |

### Considérations de sécurité

1. **Autorisation réelle vs affichage client** : Les contrôles de rôles côté frontend (`ProtectedRoute adminOnly`, masquage de boutons) sont strictement destinés à l'ergonomie utilisateur. L'autorité de contrôle d'accès est exclusivement exercée par les contrôleurs Spring Boot (`@PreAuthorize("hasRole('ADMIN')")`).
2. **Stockage du Token JWT** : Le token est conservé dans `localStorage` pour persister la session au rafraîchissement. Pour limiter les risques de vol de session en cas de vulnérabilité XSS :
   * L'application n'utilise aucun rendu HTML brut non échappé (`dangerouslySetInnerHTML`).
   * Tous les textes et entrées utilisateurs sont échappés par le moteur React.
3. **CORS** : En environnement de développement local, la passerelle configure `allowedOrigins: "*"`. Pour la mise en production, il est impératif de restreindre cette directive au nom de domaine du frontend hébergé.
4. **Pas de révocation de token côté backend** : Comme le microservice `auth-service` ne dispose pas d'endpoint de révocation (blacklist Redis ou invalidation de token), la déconnexion détruit le token côté client. L'expiration naturelle du JWT (`jwt.expiration`) limite la fenêtre de validité.
