# INDEPENDENT SECURITY AUDIT REPORT: MULTI-TENANCY IMPLEMENTATION
**Target System:** AI Business Platform (`ai-business-platform`)  
**Repository:** `~/Projects/ai-business-platform` (`RidaManfalouti/ai-business-platform`)  
**Audited Git Commit:** `72a1fc0` (Merged Multi-Tenancy Architecture, Base: `1f08104`)  
**Audit Date:** November 13, 2026  
**Auditor Roles:** Senior Application Security Engineer & Spring Boot Microservices Architect  
**Classification:** Confidential — Internal Security Assessment  

---

## 1. EXECUTIVE SUMMARY & VERDICT

### 1.1 Final Audit Verdict

| VERDICT | JUSTIFICATION |
|:---:|---|
| **DO NOT RELEASE** | **BLOCKING VULNERABILITY IDENTIFIED.** While the multi-tenant architecture implements robust cryptographic JWT derivation, perimeter header stripping, and row-level query scoping across business microservices, a **CRITICAL privilege escalation flaw (SEC-MT-01)** in `JwtService.java` allows any company administrator (Tenant Admin) to automatically acquire platform-level administrator privileges (`ROLE_ADMIN`). This enables unauthorized access to platform-wide user management endpoints and arbitrary user role elevation, violating Non-Negotiable Security Principle 11. Release is blocked until the hotfix is applied. |

---

### 1.2 Evaluation Scorecard against Non-Negotiable Principles

| # | Security Principle | Status | Evaluation Summary |
|---|---|:---:|---|
| 1 | Never trust a tenant ID provided by frontend | **PASS** | Gateway strips client headers; services derive context strictly from verified JWT claims. |
| 2 | Never rely exclusively on frontend filtering | **PASS** | Microservices enforce server-side query filters on every data access routine. |
| 3 | Never rely exclusively on API Gateway validation | **PASS** | Microservices operate as independent OAuth2 Resource Servers verifying HMAC signatures. |
| 4 | Every backend service must independently enforce tenant isolation | **PASS** | All 4 business services (`product`, `customer`, `sales`, `analytics`) validate JWT independently. |
| 5 | Derive active tenant from verified JWT claim & authorized membership | **PASS** | `TenantService.switchTenant` enforces active membership before signing a new token. |
| 6 | A user must not access a tenant merely by changing header/URL/body | **PASS** | Parameter tampering fails; missing/mismatched JWT claims result in HTTP 403 or 404. |
| 7 | Every business database query must be tenant-scoped | **PASS** | Repositories use `WHERE tenant_id = :tenantId`; aggregations and joins are tenant-bound. |
| 8 | Cross-tenant relations must be impossible | **PASS / CONDITIONAL** | Cross-tenant orders are blocked (404 on customer/product lookup); schema lacks composite FKs. |
| 9 | Tenant context must survive internal calls and saga operations | **PASS** | `RestClientConfig` and `SagaRecoveryWorker` explicitly preserve and propagate tenant identity. |
| 10 | Missing tenant context must fail closed for tenant operations | **FAIL** | `InternalTokenService.java` silently defaults to `DEFAULT_LEGACY_TENANT_ID` (SEC-MT-02). |
| 11 | Platform-level operations must be explicitly separated from tenant permissions | **FAIL** | Tenant `ADMIN` role is mapped directly to global `ROLE_ADMIN` in JWT roles (SEC-MT-01). |
| 12 | Never expose secrets, JWTs or sensitive customer data in logs | **PASS** | Verified that loggers sanitize tokens and payloads; correlation IDs used across hops. |

---

## 2. VULNERABILITY FINDINGS LOG

### Finding SEC-MT-01: Platform Privilege Escalation via Tenant Admin Role (CRITICAL)

- **Vulnerability Type:** Broken Object / Function Level Authorization (OWASP API5:2023)
- **Severity:** **CRITICAL** (CVSS 9.1: `CVSS:3.1/AV:N/AC:L/PR:H/UI:N/S:C/C:H/I:H/A:N`)
- **Impacted Files:**
  - `services/auth-service/src/main/java/com/aibi/auth_service/security/JwtService.java` (Lines 58–68)
  - `services/auth-service/src/main/java/com/aibi/auth_service/controller/UserController.java` (Lines 47, 62)

#### Detailed Mechanics
In `JwtService.java`, when a token is generated during login, registration, or tenant switching, the method `generateToken(...)` accepts `user.getRole()` (Platform Role) and `tenantRole` (Organization Role). Lines 58–68 process `tenantRole`:

```java
if (tenantRole != null && !tenantRole.isBlank()) {
    String tr = tenantRole.trim().toUpperCase();
    String normalizedRole = tr.startsWith("ROLE_") ? tr : "ROLE_" + tr;
    if (!authorities.contains(normalizedRole)) {
        authorities.add(normalizedRole); // <--- VULNERABILITY: ADDS "ROLE_ADMIN" FOR A TENANT ADMIN
    }
    String tenantPrefixed = "ROLE_TENANT_" + normalizedRole.replace("ROLE_", "");
    if (!authorities.contains(tenantPrefixed)) {
        authorities.add(tenantPrefixed);
    }
}
```

When a standard user (`user.role == USER`) is granted the `ADMIN` role within their specific tenant organization (e.g. `tenantRole = "ADMIN"`), `normalizedRole` evaluates to `"ROLE_ADMIN"`. `JwtService` appends `"ROLE_ADMIN"` into the `roles` claim of the JWT.

In `UserController.java`, the platform administrative endpoints are guarded by:
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
    List<UserSummaryResponse> users = userRepository.findAll().stream()... // DUMPS ALL PLATFORM USERS
}

@PatchMapping("/{id}/role")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<UserSummaryResponse> updateUserRole(...) // ELEVATES ANY USER TO PLATFORM ADMIN
```

#### Exploit Scenario & Impact
1. An attacker registers an account on the platform (platform role `USER`).
2. The user creates their own company (`POST /api/auth/tenants`), where they naturally become `OWNER` or `ADMIN` of that tenant.
3. The server issues a JWT containing `"roles": ["ROLE_USER", "ROLE_ADMIN", "ROLE_TENANT_ADMIN"]`.
4. The user calls `GET /api/users`. Spring Security's `SecurityContext` evaluates `hasRole('ADMIN')` to `true`.
5. The attacker successfully extracts the list of all registered platform users across all companies (emails, IDs, usernames).
6. The attacker invokes `PATCH /api/users/{id}/role` to change their platform role (or any other user's role) to `ADMIN`.
7. **Result:** Total compromise of the SaaS platform and complete bypass of tenant boundaries.

#### Remediation
Tenant roles must NEVER share namespace or authorities with platform roles. 
Modify `JwtService.java` to prefix tenant roles strictly with `ROLE_TENANT_`:
```java
// Remediated JwtService.java:
if (tenantRole != null && !tenantRole.isBlank()) {
    String tr = tenantRole.trim().toUpperCase();
    String baseRole = tr.startsWith("ROLE_") ? tr.substring(5) : tr;
    String tenantPrefixed = "ROLE_TENANT_" + baseRole;
    if (!authorities.contains(tenantPrefixed)) {
        authorities.add(tenantPrefixed);
    }
    // DO NOT add baseRole or ROLE_<tenantRole> to global authorities
}
```
In `UserController.java`, ensure platform administrative endpoints explicitly check `hasAuthority('ROLE_ADMIN')` representing the global user platform role.

---

### Finding SEC-MT-02: Insecure Default Fallback to Legacy Tenant in `InternalTokenService` (HIGH)

- **Vulnerability Type:** Insecure Default / Failure to Fail Closed (CWE-209 / Non-Negotiable Principle 10)
- **Severity:** **HIGH** (CVSS 7.5: `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N`)
- **Impacted Files:**
  - `services/sales-service/src/main/java/com/aibi/sales_service/security/InternalTokenService.java` (Lines 28, 36–39)
  - `services/analytics-service/src/main/java/com/aibi/analytics_service/security/InternalTokenService.java` (Lines 27–29)

#### Detailed Mechanics
In `sales-service`, `InternalTokenService.java` provides system tokens for internal asynchronous calls:
```java
private static final UUID DEFAULT_LEGACY_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

public String getInternalServiceToken() {
    UUID tenantId = TenantContext.getTenantId();
    return getInternalServiceToken(tenantId != null ? tenantId : DEFAULT_LEGACY_TENANT_ID);
}
```
Similarly, in `analytics-service`, `InternalTokenService.java` generates a token without a `tenant_id` claim if `TenantContext.getTenantId()` is null.

#### Impact
If an asynchronous job, scheduled task, or background worker executes without initializing `TenantContext`, `sales-service` silently assumes the identity of tenant `00000000-0000-0000-0000-000000000001` (the deterministic legacy company).
This directly violates Security Principle 10 ("Missing tenant context must fail closed for tenant-protected operations"). Any un-scoped internal request could mutate or read legacy tenant data without triggering an exception.

#### Remediation
Remove `DEFAULT_LEGACY_TENANT_ID` fallback completely. Enforce fail-closed behavior:
```java
public String getInternalServiceToken() {
    UUID tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
        throw new MissingTenantContextException("Impossible de générer un jeton de service interne sans contexte locataire actif.");
    }
    return getInternalServiceToken(tenantId);
}
```

---

### Finding SEC-MT-03: Cross-Tenant DoS via Global Unique Constraint on `stock_reservations` (MEDIUM-HIGH)

- **Vulnerability Type:** Cross-Tenant Resource Contention / Denial of Service (CWE-400 / CWE-200)
- **Severity:** **MEDIUM-HIGH** (CVSS 6.5: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:H`)
- **Impacted Files:**
  - `services/product-service/src/main/resources/db/migration/V2__create_stock_reservations.sql` (Line 8)
  - `services/product-service/src/main/resources/db/migration/V4__add_tenant_id_to_products_and_reservations.sql`

#### Detailed Mechanics
In `product-service`, migration `V2__create_stock_reservations.sql` declared:
```sql
CREATE TABLE IF NOT EXISTS stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    reservation_id VARCHAR(100) NOT NULL UNIQUE,
...
```
When multi-tenancy was introduced in `V4__add_tenant_id_to_products_and_reservations.sql`, the migration added `tenant_id` and created composite index `idx_stock_res_tenant ON stock_reservations(tenant_id, reservation_id)`.
However, unlike `products.name` where `products_name_key` was dropped and replaced by `uq_products_tenant_name (tenant_id, name)`, **the global unique constraint on `reservation_id` was never dropped**.

#### Impact
1. **Cross-Tenant Collision / DoS:** If Tenant A and Tenant B happen to submit a reservation with the same identifier (e.g. predictable pattern `RES-20261113-001` or standard order ID), Tenant B's request will fail with a PostgreSQL `DataIntegrityViolationException` (`stock_reservations_reservation_id_key` violation).
2. **Reservation Enumeration:** An attacker in Tenant A can probe whether a specific reservation ID exists in Tenant B by attempting to create it and observing whether a 409 Conflict / 500 DB error is returned.

#### Remediation
Add a Flyway migration `V5__fix_stock_reservations_unique_constraint.sql`:
```sql
ALTER TABLE stock_reservations DROP CONSTRAINT IF EXISTS stock_reservations_reservation_id_key;
ALTER TABLE stock_reservations ADD CONSTRAINT uq_stock_reservations_tenant_id_res_id UNIQUE (tenant_id, reservation_id);
```

---

### Finding SEC-MT-04: Lack of Token Revocation for Suspended Tenants or Revoked Memberships (MEDIUM)

- **Vulnerability Type:** Improper Session Management / Revocation (OWASP API2:2023)
- **Severity:** **MEDIUM** (CVSS 5.4: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:L/A:N`)
- **Impacted Components:** All microservices (`JwtDecoder`, `TenantService.java`, Redis layer).

#### Detailed Mechanics
JWTs are signed with a validity period of 24 hours (`jwt.expiration: 86400000`). Downstream microservices (`product-service`, `customer-service`, `sales-service`, `analytics-service`) validate incoming JWTs strictly statelessly using the HMAC secret key.
While `TenantService.switchTenant(...)` actively verifies that the tenant and membership are `ACTIVE`, already-issued tokens are not checked against a revocation list.

#### Impact
If an organization is suspended by platform admins, or if a rogue employee's membership is revoked or suspended in `auth-service`, that user's existing JWT remains fully functional across all microservices for up to 24 hours. They can continue reading, modifying, or deleting company data until the JWT expires.

#### Remediation
1. Reduce JWT expiration for tenant sessions to 15–30 minutes, backed by refresh tokens.
2. Implement a distributed Redis blocklist or version stamp (e.g. `tenant_version` or `membership_version` claim verified against Redis or local cache).

---

### Finding SEC-MT-05: Residual Un-scoped Methods in `StockReservationRepository` (LOW)

- **Vulnerability Type:** Dead Code / Latent Insecure API (CWE-561)
- **Severity:** **LOW** (CVSS 3.1)
- **Impacted Files:**
  - `services/product-service/src/main/java/com/aibi/product_service/repository/StockReservationRepository.java` (Lines 40–47)

#### Detailed Mechanics
`StockReservationRepository` retains legacy methods:
```java
Optional<StockReservation> findByReservationId(String reservationId);
Optional<StockReservation> findByReservationIdForUpdate(String reservationId);
boolean existsByReservationId(String reservationId);
```
While inspection confirms these methods are not currently invoked by `StockReservationService` (which calls `findByTenantIdAndReservationId`), leaving global un-scoped query methods in repository interfaces poses a risk of future developer error.

#### Remediation
Remove all un-scoped methods from `StockReservationRepository.java`.

---

## 3. COMPREHENSIVE SECTION-BY-SECTION AUDIT

### Audit 1: JWT & Authentication

#### 1.1 Token Structure and Claims
- Inspection of `JwtService.java` reveals the following token structure:
  - `sub`: User email (e.g., `user@domain.com`)
  - `user_id`: Numeric ID of the user
  - `tenant_id`: UUID of the active tenant (e.g., `00000000-0000-0000-0000-000000000001`)
  - `tenant_slug`: Alphanumeric slug of the tenant
  - `tenant_role`: Organization role (`OWNER`, `ADMIN`, `MANAGER`, `SELLER`, `VIEWER`)
  - `roles`: Authority list evaluated by Spring Security
- **Cryptographic Signature:** Tokens are signed with HMAC-SHA256 (`HS256`) using a 256-bit secret key decoded from Base64 (`jwt.secret`). In production, this key is validated at startup by `ProductionSecurityValidator` to ensure it is not a weak or placeholder string.
- **Tampering Resistance:** An attacker modifying the `tenant_id` claim in the JWT payload will immediately invalidate the cryptographic signature. All microservices use `NimbusJwtDecoder` and reject tampered tokens with HTTP 401 Unauthorized.

#### 1.2 Tenant Switching Endpoint
- Located at `POST /api/auth/tenants/switch` in `TenantController.java` / `TenantService.java`.
- Verification sequence:
  1. Authenticates calling user via SecurityContext (`userId`).
  2. Executes `membershipRepository.findByTenantIdAndUserIdAndStatus(targetTenantId, userId, ACTIVE)`.
  3. Executes `tenantRepository.findById(targetTenantId)` and checks `status == ACTIVE`.
  4. Generates a new cryptographically signed JWT containing the new `tenant_id`, `tenant_slug`, and verified `tenant_role`.
- **Verdict:** Secure against unauthorized switching. A user cannot switch to a company they do not belong to, nor to a suspended company.

---

### Audit 2: Authorization & Role Model

#### 2.1 Perimeter Defense (API Gateway)
- `AuthorizationHeaderForwardingFilter.java` executes at highest precedence (`Ordered.HIGHEST_PRECEDENCE + 10`).
- Lines 37–46 explicitly strip:
  - `X-Tenant-Id`, `X-Tenant-ID`, `X-TenantId`
  - `X-Role`, `X-Roles`, `X-User-Role`, `X-Authorities`
- **Result:** Header injection attacks targeting tenant identity or role elevation are completely neutralized at the ingress point.

#### 2.2 Microservice Security Context
- Each microservice implements `TenantContextFilter` extending `OncePerRequestFilter`.
- `TenantContextFilter` extracts the `tenant_id` claim exclusively from the verified `JwtAuthenticationToken`.
- If a request lacks an Authorization header or provides an invalid token, `TenantContext` remains empty (`null`).
- Downstream services call `TenantContext.getRequiredTenantId()`, which throws `MissingTenantContextException` (fails closed).
- **Caveat:** The vulnerability identified in SEC-MT-01 demonstrates that role authorization (`hasRole('ADMIN')`) was conflated between platform admins and tenant admins, undermining the authorization layer at `auth-service`.

---

### Audit 3: Database Isolation & Architecture

#### 3.1 Logical Row-Level Isolation vs Partitioning
> [!IMPORTANT]
> The database implementation utilizes **Application-Managed Logical Row-Level Isolation** with a tenant discriminator column (`tenant_id UUID NOT NULL`).
> It does **NOT** use PostgreSQL physical table partitioning (`PARTITION BY ...`) nor PostgreSQL Row-Level Security (`ENABLE ROW LEVEL SECURITY`).

- **Disk & Storage Model:** All tenants share identical PostgreSQL tables within each service database. Isolation is enforced through `tenant_id` query predicates and composite indexing.

#### 3.2 Flyway Migrations Audit

| Service | Migration File | `tenant_id` Column | Backfill Applied | Composite Index | Unique Constraints Scoped |
|---|---|:---:|:---:|:---:|:---:|
| `auth-service` | `V2__create_tenants_and_memberships.sql` | Primary Key | Seeded `...0001` | `(tenant_id, user_id)` | `uq_memberships_tenant_user` |
| `product-service` | `V4__add_tenant_id_to_products_and_reservations.sql` | `NOT NULL` | Updated `...0001` | `(tenant_id, name)` | `uq_products_tenant_name` (Note: `stock_reservations` missing drop) |
| `customer-service` | `V2__add_tenant_id_to_customers.sql` | `NOT NULL` | Updated `...0001` | `(tenant_id, email)` | `uq_customers_tenant_email` |
| `sales-service` | `V3__add_tenant_id_to_sales_and_sagas.sql` | `NOT NULL` | Updated `...0001` | `(tenant_id, reference)` | `uq_sales_tenant_reference`, `uq_sagas_tenant_saga_id` |
| `analytics-service`| `V2__add_tenant_id_to_analytics_snapshots.sql` | `NOT NULL` | Updated `...0001` | `(tenant_id, snapshot_date)` | `uq_analytics_tenant_date` |

#### 3.3 Query Scoping Inspection
Every JPA repository was examined for potential un-scoped queries:
- **`ProductRepository`:** All queries (`findByTenantIdAndId`, `decrementStock`, `incrementStock`, `findByTenantIdAndNameContainingIgnoreCase`) strictly bind `tenantId`.
- **`CustomerRepository`:** All methods (`findByTenantIdAndId`, `searchByTenantIdAndName`, `existsByTenantIdAndEmailIgnoreCase`) strictly bind `tenantId`.
- **`SaleRepository`:** Native SQL queries (`sumCompletedRevenue`, `countCompletedSales`, `getDailyTrendsNative`, `getTopProductsNative`) include explicit `WHERE tenant_id = :tenantId`.
- **`SagaRepository`:** Sagas and saga events are queried via `findByTenantIdAndSagaId` and `findByTenantIdAndStatus`.

---

### Audit 4: ThreadLocal & Microservice Propagation

#### 4.1 Lifecycle and Memory Leak Prevention
- `TenantContext.java` wraps a static `ThreadLocal<UUID>`.
- In all microservices (`product-service`, `customer-service`, `sales-service`, `analytics-service`), `TenantContextFilter.java` wraps request execution in a strict `try-finally` block:
  ```java
  try {
      // Extract from verified JWT and set TenantContext
      filterChain.doFilter(request, response);
  } finally {
      TenantContext.clear(); // Invokes CURRENT_TENANT.remove()
  }
  ```
- **Thread Reuse Safety:** Calling `.remove()` in `finally` guarantees that Tomcat worker threads returned to the thread pool cannot carry stale tenant IDs into subsequent HTTP requests.

#### 4.2 Asynchronous Execution and Background Workers
- `SagaRecoveryWorker.java` in `sales-service` runs on a scheduled background thread pool (`@Scheduled`).
- Because background threads lack HTTP request context, `SagaRecoveryWorker` explicitly binds the tenant context from the persisted saga entity before invoking downstream APIs:
  ```java
  for (Saga saga : eligibleSagas) {
      try {
          TenantContext.setTenantId(saga.getTenantId());
          // execute recovery and compensation
      } finally {
          TenantContext.clear();
      }
  }
  ```
- This guarantees that compensation calls to `product-service` (`releaseStock`) are properly scoped to the owning tenant.

#### 4.3 Inter-Service Token Propagation
- `RestClientConfig.java` defines an interceptor:
  - If a user HTTP request is active, it forwards the client's `Authorization: Bearer <jwt>`.
  - If no user request is active (e.g. background worker), it calls `internalTokenService.getInternalServiceToken()`.
- **Defect (SEC-MT-02):** As detailed in Section 2, fallback to `DEFAULT_LEGACY_TENANT_ID` when `TenantContext` is null compromises fail-closed integrity.

---

### Audit 5: Sales & Saga Distributed Isolation

#### 5.1 Pre-Sale Validation & Atomic Reservations
When `SalesService.createSale(request)` is executed:
1. **Customer Ownership Check:** `customerServiceClient.getCustomerById(request.customerId())` calls `customer-service`. `customer-service` validates `WHERE tenant_id = :tenantId AND id = :id`. If the customer belongs to another tenant, it returns HTTP 404, causing `sales-service` to immediately abort with `CustomerNotFoundException`.
2. **Product Ownership & Stock Reservation:** `productServiceClient.reserveStock(...)` calls `product-service`. `product-service` checks `WHERE tenant_id = :tenantId AND id = :productId`. Any item belonging to another tenant triggers `ProductNotFoundException` (404) or `InsufficientStockException` (409).
3. **Pessimistic Concurrency:** Stock reservations sort items by `productId` ascending to prevent database deadlocks and use `SELECT ... FOR UPDATE` during compensation release.
4. **Saga State Isolation:** Saga records and saga history events in `sagas` and `saga_events` store `tenant_id NOT NULL`.

---

### Audit 6: Redis & Analytics Isolation

#### 6.1 Redis Analysis
- `docker-compose.yml` provisions a Redis 7 instance (`ai-platform-redis:6379`).
- **Code Inspection:** None of the Spring Boot microservices currently include `spring-boot-starter-data-redis` or have `@EnableCaching` configured. All database reads are served directly by PostgreSQL.
- **Assessment:** There is currently zero risk of cross-tenant data leakage via Redis cache poisoning because Redis is not active in the application layer.
- **Architectural Directive:** When Redis caching is implemented, cache key generation must mandate a tenant namespace: `tenant:{tenantId}:{entity}:{id}`.

#### 6.2 Analytics Isolation
- `AnalyticsService.java` aggregates metrics by calling downstream services via `SalesServiceClient` and `ProductServiceClient`.
- Downstream HTTP requests forward the caller's JWT, ensuring that the analytics engine only receives data belonging to the authenticated tenant.
- Snapshots stored in `analytics_snapshots` have `tenant_id NOT NULL` and a unique constraint `uq_analytics_tenant_date (tenant_id, snapshot_date)`.

---

### Audit 7: Frontend Multi-Tenancy Review

#### 7.1 State Storage & Header Hygiene
- `frontend/src/context/AuthContext.tsx` stores the active token in browser `localStorage` (`ai_platform_token`).
- `frontend/src/api/client.ts` attaches `Authorization: Bearer <token>` and `X-Correlation-Id`.
- **Verified:** Frontend code never attempts to set `X-Tenant-Id` or pass `tenantId` in request query strings or JSON bodies for business entities (`products`, `customers`, `sales`).

#### 7.2 Tenant Switching & Cache Invalidation
- When a user switches companies via `<TenantSwitcher />`:
  1. `switchTenant(targetTenantId)` is executed.
  2. The new JWT is saved to `localStorage`.
  3. **Critical Defense:** `queryClient.clear()` is called immediately!
  4. All cached TanStack Query caches (product lists, customer tables, sales history, KPIs) are completely purged from browser memory before new requests are dispatched.
- **Result:** No risk of cross-tenant data ghosting or stale display in the UI.

---

## 4. ADVERSARIAL TEST SUITE SPECIFICATION

The following 14 adversarial test scenarios must be executed against the test harness to systematically verify tenant boundaries:

| Test ID | Objective | Attack Vector / Procedure | Expected Result | Assertion |
|---|---|---|---|---|
| **ADV-01** | Cross-Tenant Product Read | Tenant A requests `GET /api/products/{tenantB_productId}` using Tenant A's JWT. | HTTP 404 Not Found | Data is not leaked; 404 returned. |
| **ADV-02** | Cross-Tenant Product Modification | Tenant A sends `PUT /api/products/{tenantB_productId}` with modified name/price. | HTTP 404 Not Found | No modification; Tenant B data remains untouched. |
| **ADV-03** | Cross-Tenant Product Deletion | Tenant A sends `DELETE /api/products/{tenantB_productId}`. | HTTP 404 Not Found | Record remains intact in database. |
| **ADV-04** | Cross-Tenant Customer Exfiltration | Tenant A sends `GET /api/customers?email={tenantB_email}`. | HTTP 200 with Empty List | Filter `tenant_id = :tenantA` excludes Tenant B record. |
| **ADV-05** | Cross-Tenant Sale Hijack | Tenant A sends `POST /api/sales` referencing `customerId` belonging to Tenant B. | HTTP 404 Not Found | `CustomerNotFoundException` aborts sale creation. |
| **ADV-06** | Cross-Tenant Stock Depletion | Tenant A sends `POST /api/sales` with item `productId` belonging to Tenant B. | HTTP 404 Not Found | `ProductNotFoundException` aborts before stock decrement. |
| **ADV-07** | Direct Stock Reservation Probe | Tenant A sends `POST /api/products/reservations` for Tenant B's product ID. | HTTP 404 Not Found | Reservation rejected; stock remains unaffected. |
| **ADV-08** | Ingress Header Spoofing | Client sends `X-Tenant-Id: <tenantB_UUID>` with Tenant A's JWT through Gateway. | HTTP 200 scoped to Tenant A | Gateway strips header; service reads JWT claim only. |
| **ADV-09** | JWT Claim Tampering | Client alters `tenant_id` inside JWT payload without valid HMAC signature. | HTTP 401 Unauthorized | Cryptographic signature verification fails. |
| **ADV-10** | Unauthorized Tenant Switch | User A calls `POST /api/auth/tenants/switch` with `tenantId` of Company B (not a member). | HTTP 403 Forbidden | Membership check rejects request; no token issued. |
| **ADV-11** | Suspended Tenant Access | User calls `POST /api/auth/tenants/switch` targeting a suspended organization. | HTTP 403 Forbidden | "Cette entreprise est actuellement suspendue." |
| **ADV-12** | ThreadLocal Pollution Test | 100 concurrent threads alternating requests between Tenant A and Tenant B. | 100% Correct Scoping | Zero data cross-contamination; `.remove()` cleans threads. |
| **ADV-13** | Reservation ID Collision (SEC-MT-03) | Tenant B submits reservation with ID identical to Tenant A's existing reservation. | Current: 500 DB Error<br>**Target:** 201/200 scoped | Demonstrates need to drop global unique constraint. |
| **ADV-14** | Privilege Escalation Probe (SEC-MT-01) | Tenant Admin calls `GET /api/users` and `PATCH /api/users/{id}/role`. | Current: 200 OK (Vulnerable)<br>**Target:** 403 Forbidden | Demonstrates need to remove `ROLE_ADMIN` from tenant roles. |

---

## 5. REMEDIATION ROADMAP

### Step 1: Hotfix Privilege Escalation (Finding SEC-MT-01) — PRIORITY 1
In `services/auth-service/src/main/java/com/aibi/auth_service/security/JwtService.java`:
- Remove lines that add `normalizedRole` directly to authorities when processing `tenantRole`.
- Map tenant roles strictly to `ROLE_TENANT_<ROLE>` (e.g. `ROLE_TENANT_ADMIN`, `ROLE_TENANT_OWNER`).
- In `UserController.java`, ensure `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` only accepts the platform administrator role.

### Step 2: Enforce Fail-Closed in Internal Token Services (Finding SEC-MT-02) — PRIORITY 2
In `services/sales-service/src/main/java/com/aibi/sales_service/security/InternalTokenService.java` and `analytics-service`:
- Delete `DEFAULT_LEGACY_TENANT_ID`.
- Throw `MissingTenantContextException` whenever `TenantContext.getTenantId()` is null.

### Step 3: Scope Stock Reservation Uniqueness (Finding SEC-MT-03) — PRIORITY 3
In `services/product-service/src/main/resources/db/migration/V5__fix_stock_reservations_unique_constraint.sql`:
- Execute:
  ```sql
  ALTER TABLE stock_reservations DROP CONSTRAINT IF EXISTS stock_reservations_reservation_id_key;
  ALTER TABLE stock_reservations ADD CONSTRAINT uq_stock_reservations_tenant_reservation_id UNIQUE (tenant_id, reservation_id);
  ```

### Step 4: Token Revocation / Blacklist Architecture (Finding SEC-MT-04) — PRIORITY 4
- Introduce a lightweight Redis check or reduced token lifetime (15 mins) to guarantee that organization or membership suspensions take immediate effect across microservices.

---

## 6. CONCLUSION & SIGN-OFF

The multi-tenant architecture designed for `ai-business-platform` is fundamentally robust in its core data-isolation patterns, distributed saga compensation, and gateway protections. However, the discovery of **SEC-MT-01** represents a critical privilege escalation path that allows tenant administrators to usurp platform administrative power.

Until **SEC-MT-01** and **SEC-MT-02** are resolved, the system cannot be certified for production deployment. Following the implementation of the recommended hotfixes and the execution of the adversarial test suite, the platform will satisfy enterprise multi-tenant isolation standards.
