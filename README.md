# LTOA Platform — Documentation Technique Complète

> **Projet** : LTOA — Plateforme multi-tarificateur pour l'assurance rapatriement de corps  
> **Développeur** : Badr KORICHI — INSA Lyon  
> **Version actuelle** : 1.0.0 (Phase 0 — Fondations entreprise)  
> **Dépôt** : https://gitlab.com/korichi/ltoa.git  

---

## Table des matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [Stack technique choisie](#2-stack-technique-choisie)
3. [Architecture de l'application](#3-architecture-de-lapplication)
4. [Sécurisation complète](#4-sécurisation-complète)
5. [Base de données et migrations](#5-base-de-données-et-migrations)
6. [Intégration AGIS — Assistance Diaspora](#6-intégration-agis--assistance-diaspora)
7. [Intégration CIEL — Ciel Assurances](#7-intégration-ciel--ciel-assurances)
8. [Intégration AssurMax](#8-intégration-assurmax)
9. [Frontend — Architecture et fonctionnement](#9-frontend--architecture-et-fonctionnement)
10. [Routes et endpoints définis](#10-routes-et-endpoints-définis)
11. [Documentation API (Swagger)](#11-documentation-api-swagger)
12. [Prochaines étapes](#12-prochaines-étapes)

---

## 1. Présentation du projet

### Contexte métier

LTOA est un **courtier en assurance** spécialisé dans l'**assurance rapatriement de corps** pour la diaspora africaine et méditerranéenne en France. Quand une personne décède à l'étranger, le rapatriement du corps vers le pays d'origine est une opération coûteuse (entre 3 000 € et 10 000 €+). L'assurance rapatriement de corps couvre ces frais.

### Problématique

LTOA travaille avec **plusieurs partenaires assureurs** (Ciel Assurances, AGIS/Assistance Diaspora, ECA, REPAM). Chaque partenaire a son propre système de tarification : certains ont une API, d'autres un site web avec formulaires. Actuellement, pour obtenir un devis, le courtier doit :

1. Se connecter manuellement sur chaque site partenaire
2. Remplir le même formulaire sur chaque plateforme
3. Comparer les résultats à la main

### Solution

La plateforme LTOA est un **multi-tarificateur unifié** : le courtier remplit un **seul formulaire**, et l'application interroge **tous les partenaires simultanément** via leurs APIs ou par reverse-engineering de leurs formulaires web. Les résultats sont agrégés, comparés, et le meilleur devis est recommandé automatiquement.

---

## 2. Stack technique choisie

### Backend — Java / Spring Boot

| Technologie | Version | Rôle |
|---|---|---|
| **Java** | 17 (compilé), 20 (exécution) | Langage backend |
| **Spring Boot** | 3.2.3 | Framework principal |
| **Spring Security** | (via Spring Boot) | Authentification / Autorisation |
| **Spring Data JPA** | (via Spring Boot) | ORM / Accès base de données |
| **Flyway** | 9.22.3 | Migrations de base de données versionnées |
| **H2 Database** | 2.2.224 | Base de données embarquée pour le dev |
| **PostgreSQL** | 16 | Base de données pour la production |
| **JJWT** | 0.12.5 | Librairie JWT (JSON Web Tokens) |
| **Jsoup** | 1.17.2 | Parsing HTML pour le web scraping |
| **Lombok** | (via Spring Boot) | Réduction du code boilerplate Java |
| **SpringDoc OpenAPI** | 2.3.0 | Documentation Swagger UI |
| **Maven** | 3.9.12 | Gestionnaire de build et dépendances |

**Pourquoi Spring Boot ?**
- Framework Java le plus mature et le plus utilisé en entreprise
- Écosystème complet (sécurité, persistance, cache, messaging)
- Configuration par convention : démarrage rapide
- Support natif des profils (dev/prod) pour la configuration multi-environnement
- Production-ready : métriques (Actuator), santé, monitoring intégré

### Frontend — Next.js / React

| Technologie | Version | Rôle |
|---|---|---|
| **Next.js** | 16.1.6 | Framework React full-stack |
| **React** | 19.2.3 | Bibliothèque UI |
| **TypeScript** | (via Next.js) | Typage statique pour JavaScript |
| **Tailwind CSS** | v4 | Framework CSS utilitaire |

**Pourquoi Next.js ?**
- **App Router** : système de routage basé sur les dossiers (intuitif)
- **API Routes** : proxy serveur-side intégré (les requêtes vers le backend passent par le serveur Next.js, ce qui cache l'URL du backend au client)
- **Server-Side Rendering (SSR)** : meilleure performance et SEO
- Écosystème React très large

### Pourquoi cette combinaison ?
- **Séparation des responsabilités** : le backend gère la logique métier, la sécurité et les intégrations partenaires. Le frontend gère l'interface utilisateur.
- **Proxy API** : le frontend ne communique jamais directement avec le backend. Toutes les requêtes passent par les API Routes de Next.js qui ajoutent le token JWT et transmettent au backend. Ainsi, l'URL du backend n'est **jamais exposée** au navigateur de l'utilisateur.

---

## 3. Architecture de l'application

### Architecture globale

```
┌──────────────────────────────────────────────────────────────────┐
│                         NAVIGATEUR                               │
│   (React - SPA avec pages : dashboard, clients, comparateur...) │
└──────────────┬───────────────────────────────────────────────────┘
               │ HTTP (port 3000)
               │ Cookies JWT
               ▼
┌──────────────────────────────────────────────────────────────────┐
│                    NEXT.JS (Frontend Server)                     │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │  API Routes (proxy)                                      │  │
│   │  /api/agis/devis  → backend /api/agis/devis              │  │
│   │  /api/ciel/devis  → backend /api/ciel/devis              │  │
│   │  /api/auth/login  → backend /api/auth/login              │  │
│   │  etc.                                                     │  │
│   │  Chaque route lit le JWT du cookie et l'ajoute            │  │
│   │  dans le header Authorization: Bearer <token>             │  │
│   └──────────────────────────────────────────────────────────┘  │
└──────────────┬───────────────────────────────────────────────────┘
               │ HTTP (port 8080)
               │ Header: Authorization: Bearer <JWT>
               ▼
┌──────────────────────────────────────────────────────────────────┐
│                 SPRING BOOT (Backend Server)                     │
│   ┌────────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│   │  JwtAuthFilter │→ │  Controllers │→ │  Services          │  │
│   │  (vérifie JWT) │  │  (endpoints) │  │  (logique métier)  │  │
│   └────────────────┘  └──────────────┘  └────────┬───────────┘  │
│                                                   │              │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │  JPA Repositories → H2/PostgreSQL (Base de données)      │  │
│   └──────────────────────────────────────────────────────────┘  │
└──────────────┬───────────────────────────────────────────────────┘
               │ HTTP/HTTPS (appels externes)
               ▼
┌──────────────────────────────────────────────────────────────────┐
│            PARTENAIRES EXTERNES                                  │
│   AGIS (assistance-diaspora.com)                                 │
│   CIEL (ciel-assurances.fr)                                      │
│   AssurMax (api.groupe-assurmax.com)                             │
└──────────────────────────────────────────────────────────────────┘
```

### Architecture du Backend — Monolithe modulaire

Le backend est organisé en **packages fonctionnels** (Domain-Driven Design simplifié). Chaque domaine métier a son propre package avec ses entités, repositories, services et contrôleurs :

```
com.ltoa.platform/
├── LtoaPlatformApplication.java      # Point d'entrée Spring Boot
│
├── common/                            # Code partagé
│   ├── entity/
│   │   └── BaseEntity.java           # Classe mère (id UUID, timestamps)
│   └── exception/
│       ├── GlobalExceptionHandler.java # Gestion centralisée des erreurs
│       ├── BusinessException.java      # Exception métier (400)
│       ├── ResourceNotFoundException.java # Ressource introuvable (404)
│       └── ErrorResponse.java          # Format de réponse d'erreur
│
├── config/                            # Configuration Spring
│   ├── SecurityConfig.java           # Sécurité (CORS, JWT, rôles)
│   ├── AsyncConfig.java             # Pools de threads asynchrones
│   ├── DataInitializer.java         # Création du compte admin au démarrage
│   ├── OpenApiConfig.java           # Configuration Swagger UI
│   └── RedisConfig.java             # Cache Redis (production)
│
├── security/                          # Domaine Sécurité
│   ├── auth/                         # DTOs d'authentification
│   │   ├── LoginRequest.java        # { email, password }
│   │   ├── RefreshTokenRequest.java # { refreshToken }
│   │   └── AuthResponse.java       # { accessToken, refreshToken, user }
│   ├── controller/
│   │   └── AuthController.java      # POST /api/auth/login, /refresh, /logout
│   ├── service/
│   │   └── AuthService.java         # Logique : vérif password, génère tokens
│   ├── jwt/
│   │   ├── JwtTokenProvider.java    # Génération + validation des JWT
│   │   ├── JwtAuthFilter.java       # Filtre HTTP : extrait et vérifie le JWT
│   │   └── JwtProperties.java      # Configuration JWT (secret, durée)
│   ├── encryption/
│   │   └── AesEncryptionService.java # Chiffrement AES-256-GCM
│   ├── entity/
│   │   ├── RefreshToken.java        # Token de rafraîchissement (BDD)
│   │   └── AuditLog.java           # Journal d'audit (BDD)
│   └── repository/
│       ├── RefreshTokenRepository.java
│       └── AuditLogRepository.java
│
├── user/                              # Domaine Utilisateur
│   ├── entity/
│   │   ├── User.java                # Entité utilisateur
│   │   └── Role.java               # Enum : ADMIN, COLLABORATEUR, APPORTEUR, CLIENT
│   └── repository/
│       └── UserRepository.java
│
├── client/                            # Domaine Client (assuré)
│   ├── entity/
│   │   └── Client.java
│   └── repository/
│       └── ClientRepository.java
│
├── tarification/                      # Domaine Tarification
│   └── entity/
│       ├── Tarification.java        # Demande de tarification
│       └── Formule.java             # Enum : INDIVIDUELLE, FAMILLE
│
├── resultat/                          # Domaine Résultat
│   └── entity/
│       ├── Resultat.java            # Résultat d'un partenaire
│       └── StatutDevis.java         # Enum : TARIFE, DEVIS, CONTRAT, ERREUR...
│
├── document/                          # Domaine Document
│   └── entity/
│       ├── Document.java            # Fichier PDF/attestation
│       └── TypeDocument.java        # Enum : DEVIS, CG, IPID, ATTESTATION...
│
├── connector/                         # Abstraction partenaires (Phase 2)
│   └── common/
│       ├── PartnerConnector.java    # Interface : tarifer(), creerDevis()...
│       ├── ConnectorRequest.java    # Requête unifiée
│       ├── ConnectorResult.java     # Résultat unifié
│       ├── ConnectorDocument.java   # Document unifié
│       ├── Partenaire.java          # Enum : CIEL, AGIS, ECA, REPAM
│       └── PartnerCredential.java   # Credentials chiffrés en BDD
│
├── controller/                        # Contrôleurs partenaires (actuels)
│   ├── AgisController.java          # POST /api/agis/devis
│   ├── CielController.java          # POST /api/ciel/devis
│   └── AssurMaxController.java      # POST /api/assurmax/devis
│
├── service/                           # Services partenaires (actuels)
│   ├── AgisService.java             # Appel API AGIS
│   ├── CielService.java            # Web scraping CIEL
│   └── AssurMaxService.java         # API REST AssurMax
│
├── dto/                               # Data Transfer Objects
│   ├── AgisDevisRequest.java / AgisDevisResponse.java
│   ├── CielDevisRequest.java / CielDevisResponse.java
│   └── AssurMaxDevisRequest.java / AssurMaxDevisResponse.java
│
└── audit/
    └── AuditService.java             # Journalisation asynchrone
```

### Pattern utilisé : Service / Controller / Repository

Chaque domaine suit le pattern **MVC** (Model-View-Controller) adapté aux API REST :

1. **Controller** (`@RestController`) : reçoit la requête HTTP, la valide, appelle le service, retourne la réponse
2. **Service** (`@Service`) : contient la logique métier, orchestre les appels aux repositories et services externes
3. **Repository** (`@Repository` via Spring Data JPA) : interface avec la base de données (CRUD automatique)
4. **Entity** (`@Entity`) : mapping objet-relationnel entre les classes Java et les tables en base

---

## 4. Sécurisation complète

La sécurité est un pilier fondamental de la plateforme. Voici tous les mécanismes mis en place :

### 4.1 Authentification JWT (JSON Web Token)

#### Qu'est-ce que JWT ?

JWT est un standard (RFC 7519) pour transmettre des informations de manière sécurisée entre deux parties sous forme d'un objet JSON signé. Le token est auto-contenu : il contient les informations de l'utilisateur (email, rôle) et une signature cryptographique qui garantit qu'il n'a pas été modifié.

#### Flux d'authentification complet

```
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│  NAVIGATEUR │                    │   NEXT.JS   │                    │ SPRING BOOT │
│  (React)    │                    │   (Proxy)   │                    │  (Backend)  │
└──────┬──────┘                    └──────┬──────┘                    └──────┬──────┘
       │                                  │                                  │
       │  1. POST /api/auth/login         │                                  │
       │  { email, password }             │                                  │
       │ ─────────────────────────────────>│                                  │
       │                                  │  2. POST /api/auth/login         │
       │                                  │  { email, password }             │
       │                                  │ ─────────────────────────────────>│
       │                                  │                                  │
       │                                  │                    3. Vérifie :  │
       │                                  │                    - email existe?│
       │                                  │                    - compte actif?│
       │                                  │                    - password     │
       │                                  │                      BCrypt OK?  │
       │                                  │                                  │
       │                                  │  4. { accessToken, refreshToken, │
       │                                  │       user: {email, nom, rôle} } │
       │                                  │ <─────────────────────────────────│
       │                                  │                                  │
       │  5. Set-Cookie:                  │                                  │
       │     ltoa_token = accessToken     │                                  │
       │     ltoa_refresh = refreshToken  │                                  │
       │     ltoa_user = {nom, rôle}      │                                  │
       │ <─────────────────────────────────│                                  │
       │                                  │                                  │
       │  6. GET /api/agis/devis (requête métier ultérieure)                 │
       │     Cookie: ltoa_token=eyJ...    │                                  │
       │ ─────────────────────────────────>│                                  │
       │                                  │  7. Lit le cookie, ajoute :      │
       │                                  │     Authorization: Bearer eyJ... │
       │                                  │ ─────────────────────────────────>│
       │                                  │                                  │
       │                                  │                    8. JwtAuthFilter:
       │                                  │                    - Extrait le token
       │                                  │                    - Vérifie signature
       │                                  │                    - Extrait email+rôle
       │                                  │                    - Crée le contexte
       │                                  │                      d'authentification
       │                                  │                                  │
       │                                  │  9. Réponse métier (devis AGIS)  │
       │                                  │ <─────────────────────────────────│
       │  10. Réponse au navigateur       │                                  │
       │ <─────────────────────────────────│                                  │
```

#### Détails techniques

**Génération du token** (`JwtTokenProvider.java`) :
- Algorithme de signature : **HMAC-SHA512** (clé symétrique de 256+ bits)
- Le token contient : `subject` (email), `claim("role")` (rôle), `issuedAt` (date création), `expiration` (date expiration)
- **Access token** : durée de vie **15 minutes** (900 000 ms)
- **Refresh token** : durée de vie **7 jours** (604 800 000 ms), stocké en base de données

**Vérification du token** (`JwtAuthFilter.java`) :
- C'est un filtre HTTP (`OncePerRequestFilter`) qui s'exécute sur **chaque requête**
- Il extrait le header `Authorization: Bearer <token>`
- Il vérifie la signature avec la même clé secrète
- Si valide → il crée un objet `Authentication` dans le `SecurityContext` de Spring
- Si invalide → la requête continue sans authentification (Spring Security la bloquera après)

**Refresh token** (`AuthService.java`) :
- Quand l'access token expire (après 15 min), le frontend peut demander un nouveau token en envoyant le refresh token
- L'ancien refresh token est **révoqué** (marqué `revoked=true` en BDD)
- Un nouveau couple `accessToken + refreshToken` est généré
- Cela donne une rotation automatique des tokens (sécurité renforcée)

### 4.2 Hashage des mots de passe — BCrypt

Les mots de passe ne sont **jamais stockés en clair** en base de données. On utilise **BCrypt** avec un **coût de 12** (= 2^12 = 4096 itérations de hachage).

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // Force 12
}
```

BCrypt produit un hash du type : `$2a$12$LJ3m4ym.../...` qui est irréversible. À la connexion, BCrypt re-calcule le hash du mot de passe envoyé et le compare avec celui en base.

### 4.3 Chiffrement AES-256-GCM

Les credentials des partenaires (identifiants API, mots de passe des comptes partenaires) sont **chiffrés en base de données** avec l'algorithme **AES-256-GCM** :

- **AES-256** : Advanced Encryption Standard avec une clé de 256 bits — standard industriel, utilisé par les banques et les gouvernements
- **GCM** (Galois/Counter Mode) : mode de chiffrement authentifié — il garantit à la fois la **confidentialité** (les données sont illisibles) et l'**intégrité** (les données n'ont pas été modifiées)
- La clé de chiffrement est dans la configuration (variable d'environnement en production, jamais dans le code source)

### 4.4 Contrôle d'accès basé sur les rôles (RBAC)

Chaque utilisateur a un **rôle** (enum `Role`) :

| Rôle | Permissions |
|---|---|
| `ADMIN` | Tout accès, gestion des utilisateurs, paramétrage |
| `COLLABORATEUR` | Tarifications, clients, documents |
| `APPORTEUR` | Tarifications, documents (pas de gestion client directe) |
| `CLIENT` | Accès lecture à ses propres données |

Les règles sont définies dans `SecurityConfig.java` :

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()           // Login : pas de JWT requis
    .requestMatchers("/actuator/health").permitAll()        // Santé : public
    .requestMatchers("/api/admin/**").hasRole("ADMIN")     // Admin uniquement
    .requestMatchers("/api/tarifications/**").hasAnyRole("ADMIN", "COLLABORATEUR", "APPORTEUR")
    .requestMatchers("/api/clients/**").hasAnyRole("ADMIN", "COLLABORATEUR")
    .anyRequest().authenticated()                          // Tout le reste : JWT valide requis
)
```

### 4.5 Protection CORS

**CORS** (Cross-Origin Resource Sharing) contrôle quels domaines peuvent appeler notre API :

```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",        // Frontend dev
    "https://ltoa-assurances.fr"   // Frontend production
));
```

Seuls ces deux domaines sont autorisés. Toute requête depuis un autre domaine sera rejetée par le navigateur.

### 4.6 Protection CSRF

CSRF (Cross-Site Request Forgery) est **désactivé** car nous utilisons des tokens JWT dans les headers (et pas des cookies de session classiques). Le token JWT doit être explicitement envoyé dans le header `Authorization`, ce qu'un site malveillant ne peut pas faire automatiquement.

### 4.7 Sessions stateless

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Le serveur ne garde **aucune session en mémoire**. Chaque requête doit contenir son token JWT. Cela rend l'application **scalable horizontalement** : on peut ajouter des serveurs sans se soucier de la synchronisation des sessions.

### 4.8 Gestion centralisée des erreurs

Le `GlobalExceptionHandler` intercepte toutes les exceptions et renvoie des réponses structurées :

| Exception | Code HTTP | Signification |
|---|---|---|
| `BadCredentialsException` | **401** | Email/mot de passe incorrect |
| `AccessDeniedException` | **403** | Pas les droits suffisants |
| `ResourceNotFoundException` | **404** | Ressource non trouvée |
| `BusinessException` | **400** | Erreur métier (validation) |
| `MethodArgumentNotValidException` | **400** | Données invalides |
| `Exception` (générique) | **500** | Erreur serveur inattendue |

### 4.9 Audit et traçabilité

Chaque action sensible est enregistrée dans la table `audit_logs` de manière **asynchrone** (ne ralentit pas la requête) :

```java
@Service
@RequiredArgsConstructor
public class AuditService {
    @Async("taskExecutor")
    public void log(String userId, String action, String entityType, String entityId, String details) {
        // Sauvegarde dans audit_logs
    }
}
```

### 4.10 Middleware frontend

Le fichier `middleware.ts` de Next.js protège les pages côté client :

```typescript
export function middleware(request: NextRequest) {
    const token = request.cookies.get("ltoa_token");
    if (!token) {
        return NextResponse.redirect(new URL("/login", request.url));
    }
}
```

Toute page (sauf `/login`) redirige vers la page de connexion si le cookie JWT est absent.

---

## 5. Base de données et migrations

### Stratégie multi-environnement

| Environnement | Base de données | Raison |
|---|---|---|
| **Développement** | H2 (fichier local `./data/ltoadb`) | Pas d'installation nécessaire, démarrage instantané |
| **Production** | PostgreSQL 16 | Performance, fiabilité, JSONB, fonctions avancées |

### Migrations Flyway

**Flyway** est un outil de migration de base de données versionnée. Au lieu de créer les tables à la main, on écrit des scripts SQL numérotés :

```
db/migration/h2/V1__initial_schema.sql         # Pour H2 (développement)
db/migration/postgresql/V1__initial_schema.sql  # Pour PostgreSQL (production)
```

À chaque démarrage de l'application, Flyway vérifie quelles migrations ont déjà été appliquées et exécute celles qui manquent. Cela garantit que **tous les environnements** ont exactement le même schéma.

### Schéma de la base — 8 tables

```
┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────┐
│     users        │     │    tarifications     │     │    resultats     │
├─────────────────┤     ├─────────────────────┤     ├─────────────────┤
│ id (UUID PK)    │     │ id (UUID PK)        │     │ id (UUID PK)    │
│ email (UNIQUE)  │◄────│ user_id (FK)        │     │ tarif_id (FK)───│──► tarifications
│ password_hash   │     │ client_id (FK)──────│──►  │ partenaire      │
│ role            │     │ formule             │     │ prix_ttc        │
│ nom, prenom     │     │ pays_rapatriement   │     │ garanties       │
│ telephone       │     │ date_naissance      │     │ commission      │
│ active          │     │ age                 │     │ reponse_brute   │
│ created/updated │     │ donnees_formulaire  │     │ statut_devis    │
└────────┬────────┘     │ statut              │     │ devis_reference │
         │              │ date_tarification   │     │ erreur          │
         │              └─────────────────────┘     └─────────────────┘
         │
         │              ┌─────────────────────┐     ┌─────────────────┐
         └──────────────│     clients          │     │    documents     │
                        ├─────────────────────┤     ├─────────────────┤
                        │ id (UUID PK)        │     │ id (UUID PK)    │
                        │ nom, prenom         │     │ resultat_id(FK) │
                        │ date_naissance      │     │ tarif_id (FK)   │
                        │ pays_rapatriement   │     │ type_document   │
                        │ telephone, email    │     │ nom_fichier     │
                        │ adresse             │     │ chemin_stockage │
                        │ created_by_id (FK)──│──►  │ taille_octets   │
                        │   → users           │     │ content_type    │
                        └─────────────────────┘     └─────────────────┘

┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────────────┐
│   refresh_tokens     │  │    audit_logs        │  │ partner_credentials  │
├─────────────────────┤  ├─────────────────────┤  ├──────────────────────┤
│ id (UUID PK)        │  │ id (BIGINT AUTO PK) │  │ id (UUID PK)        │
│ user_id (FK→users)  │  │ user_id             │  │ partenaire           │
│ token (UNIQUE)      │  │ action              │  │ url_extranet         │
│ expires_at          │  │ entity_type         │  │ credentials_encrypted│
│ revoked             │  │ entity_id           │  │ config_json          │
│ created_at          │  │ details             │  │ last_verified        │
└─────────────────────┘  │ ip_address          │  │ active               │
                         │ user_agent          │  └──────────────────────┘
                         │ created_at          │
                         └─────────────────────┘
```

### Entité de base — `BaseEntity`

Toutes les entités principales héritent de `BaseEntity` :

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;           // UUID généré automatiquement

    private LocalDateTime createdAt;   // Date de création (auto)
    private LocalDateTime updatedAt;   // Date de modification (auto)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Cela garantit que chaque enregistrement a un **identifiant unique universel** (UUID) et des **horodatages automatiques**.

---

## 6. Intégration AGIS — Assistance Diaspora

### Partenaire

**AGIS** (marque commerciale : Assistance Diaspora) est un assureur accessible via le site `assistance-diaspora.com`. Ils proposent un tarificateur en ligne sous forme de formulaire web.

### Méthode d'intégration : Appel API direct (Reverse Engineering)

L'intégration AGIS fonctionne par **appel direct à l'endpoint de leur tarificateur**. On a identifié que leur formulaire web envoie une requête POST vers :

```
URL : https://www.assistance-diaspora.com/tf/recherche
Méthode : POST
Content-Type : application/x-www-form-urlencoded
```

### Comment ça marche — étape par étape

1. **Le courtier** remplit un formulaire sur LTOA avec : date de naissance, pays de rapatriement, zone, option (rapatriement ou inhumation)

2. **Le backend LTOA** (`AgisService.java`) construit une requête HTTP identique à celle qu'enverrait le navigateur sur le site AGIS :

```java
// On simule un navigateur
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...");
headers.set("Origin", "https://www.assistance-diaspora.com");
headers.set("Referer", "https://www.assistance-diaspora.com/tarificateur");
headers.set("Cookie", "apporteur=28429");  // Code apporteur LTOA
```

3. **Les données du formulaire** sont envoyées sous forme URL-encodée :
   - `personne--datenaiss` : date de naissance
   - `option-rapatriement` : "Oui"
   - `choixprofil` : "Individuel" ou "Famille"
   - `provenance_pays` : code ISO du pays (ex: "MA" pour Maroc)
   - `date-depart` / `date-fin` : période de couverture (1 an par défaut)
   - etc.

4. **AGIS répond en JSON** avec un tableau de formules disponibles :

```json
[
  {
    "produit": { "label": "Assistance Financière Décès", "tarif": 120.50 },
    "formule": { "ID": "F1", "label": "Individuel" },
    "garanties": [
      { "code_garantie": "OPT1AFA", "valeur": "3 200€" },
      { "code_garantie": "option-rapatriement", "valeur": "Oui" }
    ]
  }
]
```

5. **Le service parse la réponse** et la transforme en objet `AgisDevisResponse` avec la liste des formules, prix, garanties, etc.

### Mapping des pays

AGIS utilise des codes ISO pays (MA, DZ, TN...) mais attend le nom en anglais. Le service maintient deux maps de correspondance :

```java
Map<String, String> PAYS_NAME_MAP = Map.of("MA", "Morocco", "DZ", "Algeria", "TN", "Tunisia"...);
Map<String, String> PAYS_FR_MAP = Map.of("MA", "Maroc", "DZ", "Algérie", "TN", "Tunisie"...);
```

### Code apporteur

Le code `apporteur=28429` est un identifiant unique attribué à LTOA par AGIS. Il est envoyé dans le cookie de la requête et permet à AGIS d'identifier que le devis vient de LTOA (pour les commissions).

---

## 7. Intégration CIEL — Ciel Assurances

### Partenaire

**Ciel Assurances** est un assureur accessible via `ciel-assurances.fr`. Contrairement à AGIS, Ciel n'a **pas d'API**. Leur tarificateur est un formulaire web classique qui retourne du HTML.

### Méthode d'intégration : Web Scraping avec Jsoup

L'intégration CIEL utilise le **web scraping** : on simule un navigateur qui va sur le site, remplit le formulaire, et parse le HTML de la page de résultats.

### Comment ça marche — étape par étape

1. **Phase 1 — Obtenir une session** :
   - Le service envoie un GET sur `https://ciel-assurances.fr/tarificateur/`
   - Il récupère les **cookies de session** (PHPSESSID, etc.)
   - Ces cookies sont nécessaires pour que le serveur CIEL accepte le POST du formulaire

```java
Connection.Response sessionResponse = Jsoup.connect(BASE_URL + "/tarificateur/")
    .method(Connection.Method.GET)
    .execute();
Map<String, String> cookies = sessionResponse.cookies();
```

2. **Phase 2 — Soumettre le formulaire** :
   - On envoie un POST avec les données du formulaire et les cookies de session
   - Les données : date de naissance (format DD/MM/YYYY), code pays
   - Le site CIEL répond avec une **redirection 302** vers la page de résultats

```java
Connection.Response formResponse = Jsoup.connect(BASE_URL + "/tarificateur/")
    .cookies(cookies)
    .data("date_naissance", request.getDateNaissance())     // "15/06/1985"
    .data("pays_rapatriement", request.getPaysCode())       // "MA"
    .method(Connection.Method.POST)
    .followRedirects(false)
    .execute();
```

3. **Phase 3 — Suivre la redirection et parser le HTML** :
   - On suit la redirection vers la page de résultats
   - La page contient un **tableau HTML** avec les formules et les prix
   - **Jsoup** parse le HTML et extrait les données des cellules du tableau

```java
Document resultPage = Jsoup.connect(redirectUrl)
    .cookies(allCookies)
    .get();

// Chercher le tableau des résultats
Elements rows = resultPage.select("table.tarifs tbody tr");
for (Element row : rows) {
    String formule = row.select("td:nth-child(1)").text();
    String prix = row.select("td:nth-child(2)").text();
    // Parser et créer l'objet CielFormule...
}
```

4. **Fallback regex** : si le parsing CSS ne trouve pas le tableau, un second parser à base de **regex** cherche les prix directement dans le HTML brut.

### Pourquoi le web scraping ?

Ciel Assurances n'expose **aucune API** publique ou privée. Le web scraping est la seule méthode pour automatiser l'obtention de devis. C'est une technique courante dans le courtage d'assurance, où de nombreuses compagnies n'ont pas encore d'API.

### Différences de format avec AGIS

| | AGIS | CIEL |
|---|---|---|
| Méthode | POST → JSON | POST → HTML (scraping) |
| Format date | YYYY-MM-DD | DD/MM/YYYY |
| Réponse | JSON structuré | Tableau HTML |
| Session | Cookie apporteur | Cookies PHP session |

---

## 8. Intégration AssurMax

### Partenaire

**AssurMax** (Groupe AssurMax) dispose d'une **véritable API REST** moderne avec authentification JWT.

### Méthode d'intégration : API REST standard

1. **Phase 1 — Authentification** :
   - POST sur `/api/users/authenticate` avec Basic Auth (email + password dans le header)
   - L'API retourne un **JWT token** d'accès
   - Ce token est **mis en cache** pendant 5 heures pour éviter de se réauthentifier à chaque requête

2. **Phase 2 — Demande de devis** :
   - POST sur `/api/quotations/estimation` avec le JWT en header
   - Corps JSON avec : formule, option, capital, profession, nombre d'assurés, etc.
   - Réponse JSON structurée avec le prix et les détails

### État actuel

L'intégration AssurMax est **codée et fonctionnelle** au niveau du code, mais l'API externe rejette actuellement les identifiants (erreur 401). Ce n'est pas un bug dans notre code — c'est un problème de credentials côté AssurMax qui doit être résolu avec eux directement.

---

## 9. Frontend — Architecture et fonctionnement

### Structure Next.js avec App Router

Le frontend utilise le **App Router** de Next.js. Le routage est basé sur l'arborescence des dossiers :

```
src/app/
├── layout.tsx         # Layout racine (Providers + AppShell)
├── page.tsx           # "/" → redirige vers /dashboard
├── login/
│   └── page.tsx       # Page de connexion
├── dashboard/
│   └── page.tsx       # Tableau de bord
├── clients/
│   ├── page.tsx       # Liste des clients
│   ├── nouveau/
│   │   └── page.tsx   # Créer un nouveau client
│   └── [id]/
│       └── page.tsx   # Détail d'un client
├── resultats/
│   └── page.tsx       # Résultats des devis
├── comparateur/
│   └── page.tsx       # Comparaison multi-partenaires
├── prevoyance/
│   └── page.tsx       # Prévoyance (à venir)
├── souscriptions/
│   └── page.tsx       # Souscriptions (à venir)
└── parametres/
    └── page.tsx       # Paramètres (à venir)
```

### Système d'authentification frontend

Le frontend gère l'authentification via **3 cookies** stockés dans le navigateur :
- `ltoa_token` : le JWT access token
- `ltoa_refresh` : le refresh token
- `ltoa_user` : les informations de l'utilisateur (nom, prénom, rôle) en JSON

**`AuthProvider.tsx`** est un React Context qui :
- Lit les cookies au chargement pour restaurer l'état d'authentification
- Fournit les fonctions `login()` et `logout()` à tous les composants
- **Rafraîchit automatiquement le token** toutes les 13 minutes (avant l'expiration des 15 min)

**`middleware.ts`** protège les pages :
- Si le cookie `ltoa_token` est absent → redirection vers `/login`
- Les routes `/api/*`, `/_next/*`, et les assets statiques sont exclus de cette protection

### Proxy API — Le pattern central

**Aucune requête ne va directement du navigateur vers Spring Boot.** Toutes passent par les API Routes de Next.js :

```
Navigateur → Next.js /api/agis/devis → Spring Boot /api/agis/devis
```

Chaque route API Next.js :
1. Lit le cookie `ltoa_token`
2. Ajoute le header `Authorization: Bearer <token>`
3. Transmet la requête à Spring Boot
4. Retourne la réponse au navigateur

Cela apporte :
- **Sécurité** : l'URL du backend n'est jamais visible côté client
- **Flexibilité** : on peut ajouter de la logique (retry, timeout, logs) dans le proxy
- **CORS simplifié** : le navigateur communique uniquement avec Next.js (même domaine)

### Moteur de devis (`devis-engine.ts`)

Le moteur de devis orchestre les appels vers tous les partenaires **en parallèle** :

```typescript
async function lancerAnalyse(client: ClientInfo): Promise<AnalyseClient> {
    // Lance AGIS et CIEL en parallèle
    const [agisResults, cielResults] = await Promise.all([
        fetchAgisResults(client),
        fetchCielResults(client),  // Avec retry automatique (2 tentatives)
    ]);

    // Fusionne tous les résultats
    const allDevis = [...agisResults, ...cielResults];

    // Trie par prix croissant
    allDevis.sort((a, b) => a.prixAnnuel - b.prixAnnuel);

    // Le premier = la recommandation (meilleur prix)
    return { client, devis: allDevis, recommendation: allDevis[0] };
}
```

Les appels CIEL ont un **mécanisme de retry** (2 tentatives) car le web scraping peut parfois échouer si le site est lent ou temporairement indisponible.

---

## 10. Routes et endpoints définis

### Backend — Endpoints Spring Boot (port 8080)

#### Authentification (publics — pas de JWT requis)

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/auth/login` | Connexion → retourne access + refresh tokens |
| POST | `/api/auth/refresh` | Rafraîchir le token → nouveau couple de tokens |
| POST | `/api/auth/logout` | Déconnexion → révoque tous les refresh tokens |

#### Tarification partenaires (JWT requis)

| Méthode | URL | Description |
|---|---|---|
| POST | `/api/agis/devis` | Obtenir un devis AGIS |
| POST | `/api/ciel/devis` | Obtenir un devis Ciel Assurances |
| POST | `/api/assurmax/devis` | Obtenir un devis AssurMax |

#### Monitoring (publics)

| Méthode | URL | Description |
|---|---|---|
| GET | `/actuator/health` | État de santé de l'application |
| GET | `/swagger-ui.html` | Documentation interactive de l'API |
| GET | `/v3/api-docs` | Spécification OpenAPI 3 en JSON |

### Frontend — API Routes Next.js (port 3000, proxy vers backend)

#### Auth (proxy)

| Méthode | URL Frontend | → Backend |
|---|---|---|
| POST | `/api/auth/login` | → `/api/auth/login` |
| POST | `/api/auth/refresh` | → `/api/auth/refresh` |

#### Partenaires (proxy avec JWT)

| Méthode | URL Frontend | → Backend |
|---|---|---|
| POST | `/api/agis/devis` | → `/api/agis/devis` |
| GET | `/api/agis/health` | → `/actuator/health` |
| POST | `/api/ciel/devis` | → `/api/ciel/devis` |
| GET | `/api/ciel/health` | → `/actuator/health` |
| POST | `/api/assurmax/devis` | → `/api/assurmax/devis` |

#### Données métier (proxy avec JWT)

| Méthode | URL Frontend | → Backend |
|---|---|---|
| GET / POST | `/api/clients` | → `/api/clients` |
| GET / PUT / DELETE | `/api/clients/[id]` | → `/api/clients/<id>` |
| GET / POST | `/api/analyses` | → `/api/analyses` |
| GET / DELETE | `/api/analyses/[clientId]` | → `/api/analyses/<clientId>` |

### Frontend — Pages (navigateur)

| URL | Page |
|---|---|
| `/login` | Page de connexion |
| `/` | Redirection vers `/dashboard` |
| `/dashboard` | Tableau de bord |
| `/clients` | Liste des clients |
| `/clients/nouveau` | Nouveau client |
| `/clients/[id]` | Détail d'un client |
| `/resultats` | Résultats des devis |
| `/comparateur` | Comparateur multi-partenaires |
| `/prevoyance` | Prévoyance (à venir) |
| `/souscriptions` | Suivi des souscriptions (à venir) |
| `/parametres` | Configuration (à venir) |

---

## 11. Documentation API (Swagger)

L'API est documentée automatiquement via **SpringDoc OpenAPI 3** :

- **URL d'accès** : `http://localhost:8080/swagger-ui.html`
- **Authentification dans Swagger** : bouton "Authorize" → saisir le JWT (format : `Bearer eyJ...`)
- **Fonctionnalités** : test interactif de tous les endpoints, schémas des DTOs, codes de réponse

La configuration (`OpenApiConfig.java`) définit :
- Le titre, la description, la version de l'API
- Le schéma de sécurité JWT Bearer

---

## 12. Prochaines étapes

### Phase 2A — Refactoring des connecteurs (prochaine)

Actuellement, chaque partenaire a son propre Service et Controller (AgisService, CielService, AssurMaxService). La Phase 2 prévoit de les refactorer en implémentations de l'interface `PartnerConnector` :

```java
public interface PartnerConnector {
    Partenaire getPartenaire();
    boolean isAvailable();
    ConnectorResult tarifer(ConnectorRequest request);
    ConnectorResult creerDevis(ConnectorRequest request);
    // ...
}
```

Cela permettra :
- Un **TarificationService** unique qui appelle tous les connecteurs en parallèle
- L'ajout de nouveaux partenaires (ECA, REPAM) sans modifier le code existant
- Le cache Redis des résultats en production

### Phase 2B — Intégrations ECA et REPAM

Ajout des deux derniers partenaires prévus dans le cahier des charges.

### Phase 3 — Production

- Déploiement Docker (Dockerfile + docker-compose)
- Base PostgreSQL en production
- Cache Redis activé
- Variables d'environnement pour les secrets
- CI/CD GitLab

---

## Résumé technique

| Composant | Choix | Justification |
|---|---|---|
| Langage backend | Java 17 | Robuste, typé, écosystème entreprise |
| Framework backend | Spring Boot 3.2.3 | Standard industrie, sécurité intégrée |
| Langage frontend | TypeScript | Fiabilité grâce au typage statique |
| Framework frontend | Next.js 16 / React 19 | App Router, SSR, API Routes intégrées |
| Authentification | JWT (HMAC-SHA512) | Stateless, scalable, standard |
| Hashage mots de passe | BCrypt (coût 12) | Standard, résistant aux attaques brute-force |
| Chiffrement données | AES-256-GCM | Standard bancaire, confidentialité + intégrité |
| Base dev | H2 (fichier) | Zéro configuration, embarqué |
| Base prod | PostgreSQL 16 | Fiable, performant, JSONB |
| Migrations | Flyway | Versionnées, reproductibles |
| Documentation API | Swagger / OpenAPI 3 | Standard, test interactif |
| Web scraping | Jsoup 1.17.2 | Parsing HTML robuste pour CIEL |
| Build | Maven 3.9.12 | Standard Java, gestion des dépendances |
