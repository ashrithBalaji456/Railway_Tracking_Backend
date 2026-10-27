<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=220&section=header&text=RailTrack%20India&fontSize=58&fontColor=ffffff&animation=fadeIn&fontAlignY=36&desc=Backend%20API%20%C2%B7%20Live%20Tracker%20%2B%20RailAI&descSize=20&descAlignY=58" alt="RailTrack India Backend" width="100%"/>

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&pause=1200&color=38BDF8&center=true&vCenter=true&width=720&lines=%F0%9F%9A%86+Live+Indian+Railway+tracking;%F0%9F%A4%96+RailAI+assistant+powered+by+Gemini;%F0%9F%94%90+Secured+with+Spring+Security+%2B+JWT;%E2%9A%A1+PostgreSQL+%2B+Redis+%2B+Docker" alt="Typing animation"/>

<br/>

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### 🚆 Indian Railway Live Tracker + RailAI : Backend Service

[🌐 Live App](https://railway-tracking-frontend.vercel.app/) ·
[🎨 Frontend Repo](https://github.com/ashrithBalaji456/Railway_Tracking_Frontend) ·
[💻 Backend Repo](https://github.com/ashrithBalaji456/Railway_Tracking_Backend)

</div>

---

## 📑 Table of Contents

1. [Overview](#-overview)
2. [System Architecture](#-system-architecture)
3. [Tech Stack](#-tech-stack)
4. [Workflow Charts](#-workflow-charts)
5. [Data Charts](#-data-charts)
6. [Project Structure](#-project-structure)
7. [Environment Variables](#-environment-variables)
8. [Getting Started](#-getting-started)
9. [API Documentation](#-api-documentation)
10. [Deployment](#-deployment)
11. [Security Notes](#-security-notes)
12. [Roadmap & TODOs](#-roadmap--todos)

---

## 🌟 Overview

**RailTrack India Backend** (Maven artifact `railtracker`, described in `pom.xml` as *"Indian Railway Live Tracker + RailAI"*) is the REST API behind the RailTrack India web app.

It is responsible for:

| Capability | How it is provided |
|---|---|
| 🚉 Live train data | Calls the **RailRadar** API (`RAILRADAR_BASE_URL`) with an API key |
| 🤖 RailAI assistant | Calls Google's **Gemini** model through `GEMINI_API_URL` |
| 🔐 Authentication | **Spring Security** + **JWT** (jjwt 0.12.5) |
| 💾 Persistence | **PostgreSQL** via Spring Data JPA |
| ⚡ Caching / fast storage | **Redis** (optional, per `.env.example`) |
| 📖 API docs | **springdoc-openapi** Swagger UI |
| ❤️ Health & metrics | **Spring Boot Actuator** |

---

## 🏗 System Architecture

```mermaid
flowchart LR
    U([👤 User Browser]) -->|HTTPS| FE[⚛️ Frontend<br/>Vercel]
    FE -->|REST + JWT<br/>CORS: FRONTEND_URL| API

    subgraph BE [🖥 Backend : Spring Boot 3.3.2 / Java 17]
        direction TB
        API[🌐 REST Controllers<br/>spring-boot-starter-web]
        SEC[🔐 Spring Security<br/>+ JWT filter]
        VAL[✅ Bean Validation]
        JPA[🗃 Spring Data JPA]
        WC[📡 WebClient<br/>spring-boot-starter-webflux]
        ACT[❤️ Actuator]
        DOC[📖 Swagger UI]
        API --> SEC --> VAL
        VAL --> JPA
        VAL --> WC
    end

    JPA --> PG[(🐘 PostgreSQL)]
    API -.optional cache.-> RD[(⚡ Redis)]
    WC -->|API key| RR[🚉 RailRadar API]
    WC -->|API key| GM[🤖 Gemini API]

    classDef store fill:#0b3b5c,stroke:#38bdf8,color:#fff;
    classDef ext fill:#3b2a07,stroke:#f59e0b,color:#fff;
    class PG,RD store;
    class RR,GM ext;
```

> ℹ️ The boxes inside the backend map to dependencies declared in `pom.xml`. The exact controller/service class names are in `src/` (see [TODOs](#-roadmap--todos)).

---

## 🧰 Tech Stack

| Layer | Technology | Version / Source |
|---|---|---|
| Language | Java | 17 (`pom.xml`) |
| Framework | Spring Boot | 3.3.2 (`pom.xml` parent) |
| Web | `spring-boot-starter-web` | managed by Boot |
| Reactive HTTP client | `spring-boot-starter-webflux` | managed by Boot |
| Security | `spring-boot-starter-security` | managed by Boot |
| Validation | `spring-boot-starter-validation` | managed by Boot |
| ORM | `spring-boot-starter-data-jpa` | managed by Boot |
| Cache / KV | `spring-boot-starter-data-redis` | managed by Boot |
| Monitoring | `spring-boot-starter-actuator` | managed by Boot |
| JWT | `jjwt-api / impl / jackson` | 0.12.5 |
| API docs | `springdoc-openapi-starter-webmvc-ui` | 2.5.0 |
| Database | PostgreSQL driver (runtime) | Docker image `postgres:16-alpine` |
| In-memory DB | H2 | declared in `pom.xml` (no scope set) |
| Testing | `spring-boot-starter-test`, `spring-security-test` | test scope |
| Build | Maven Wrapper (`mvnw`, `mvnw.cmd`) | included |
| Containers | Dockerfile + `docker-compose.yml` | included |

---

## 🔄 Workflow Charts

> 📌 **How to read these:** Diagrams 1, 2 and 6 are taken directly from `docker-compose.yml` / `.env.example`. Diagrams 3, 4 and 5 show the *standard flow implied by the declared dependencies*. Please verify them against your actual controllers and adjust endpoint names.

### 1️⃣ Docker Compose Startup Order

```mermaid
sequenceDiagram
    autonumber
    participant D as 🐳 docker compose up
    participant PG as 🐘 postgres (16-alpine)
    participant RD as ⚡ redis (7-alpine)
    participant BE as 🖥 backend (:8080)

    D->>PG: start container
    D->>RD: start container
    loop every 5s (max 5 retries)
        D->>PG: pg_isready -U postgres -d railtrack_db
        D->>RD: redis-cli ping
    end
    PG-->>D: healthy ✅
    RD-->>D: healthy ✅
    D->>BE: start (depends_on: service_healthy)
    BE->>PG: JDBC connect (railtrack_db)
    BE->>RD: connect (redis:6379)
    BE-->>D: listening on 8080 🚀
```

### 2️⃣ Container Network Topology

```mermaid
flowchart TB
    subgraph HOST [💻 Host Machine]
        P8080[":8080"]
        P5432[":5432"]
        P6379[":6379"]
    end
    subgraph NET [🌉 Docker network: railtracker-net bridge]
        B[railtracker-backend]
        P[railtracker-postgres]
        R[railtracker-redis]
    end
    subgraph VOL [💽 Named Volumes]
        V1[(postgres_data)]
        V2[(redis_data)]
    end
    P8080 --- B
    P5432 --- P
    P6379 --- R
    B --> P
    B --> R
    P --- V1
    R --- V2
```

### 3️⃣ JWT Authentication Flow *(conceptual)*

```mermaid
sequenceDiagram
    autonumber
    actor U as 👤 User
    participant FE as ⚛️ Frontend
    participant SEC as 🔐 Security Filter
    participant API as 🌐 Controller
    participant DB as 🐘 PostgreSQL

    U->>FE: Enter credentials
    FE->>API: POST login request
    API->>DB: Load user & verify password
    DB-->>API: User record
    API-->>FE: 200 + signed JWT
    Note over FE: Token stored client-side

    U->>FE: Open protected page
    FE->>SEC: Request + Authorization: Bearer <JWT>
    alt valid token
        SEC->>API: authenticated request
        API-->>FE: 200 data
    else invalid / expired
        SEC-->>FE: 401 / 403
    end
```

### 4️⃣ Live Train Tracking Flow *(conceptual)*

```mermaid
flowchart TD
    A([📱 User searches a train]) --> B[Frontend calls Backend API]
    B --> C{JWT valid?}
    C -- No --> X[❌ 401 Unauthorized]
    C -- Yes --> D[Validate input params]
    D --> E{Cached in Redis?}
    E -- Yes --> H[Return cached response ⚡]
    E -- No --> F[WebClient → RailRadar API<br/>using RAILRADAR_API_KEY]
    F --> G{RailRadar OK?}
    G -- No --> Y[⚠️ Error response]
    G -- Yes --> I[Map to response DTO]
    I --> J[(Store in Redis<br/>if caching is enabled)]
    J --> H
    H --> K([🗺 Frontend renders live position])
```

> Redis is marked *optional* in `.env.example`, so the cache branch may not be wired for every endpoint. Confirm in code.

### 5️⃣ RailAI (Gemini) Assistant Flow *(conceptual)*

```mermaid
sequenceDiagram
    autonumber
    actor U as 👤 User
    participant FE as ⚛️ Frontend
    participant API as 🖥 Backend
    participant RR as 🚉 RailRadar
    participant GM as 🤖 Gemini (gemini-2.5-flash)

    U->>FE: Ask a railway question
    FE->>API: POST chat message (JWT)
    opt context needed
        API->>RR: fetch relevant live data
        RR-->>API: train info
    end
    API->>GM: generateContent (GEMINI_API_KEY)
    GM-->>API: AI answer
    API-->>FE: formatted reply
    FE-->>U: Show answer 💬
```

### 6️⃣ Configuration Resolution & Deployment Pipeline

```mermaid
flowchart LR
    DEV[👨‍💻 Developer] -->|git push main| GH[(GitHub)]
    GH -->|auto deploy| FEH[▲ Vercel<br/>Frontend]
    GH -->|auto deploy / Docker| BEH[☁️ Backend host<br/>Render per .env.example]
    ENV[🔑 Environment Variables<br/>PORT · DATABASE_URL · REDIS_URL<br/>JWT_SECRET · API keys] --> BEH
    BEH --> PGC[(Managed PostgreSQL)]
    BEH --> RDC[(Managed Redis)]
    FEH -->|HTTPS + CORS| BEH
```

> The `.env.example` comments mention **Render**, so Render is shown as the backend host. If you deploy elsewhere, change this node.

---

## 📊 Data Charts

All numbers below were counted directly from the repository files, not estimated.

### 📦 `pom.xml` dependencies by category (15 total)

```mermaid
pie showData title Maven dependencies by category
    "Spring Boot runtime starters" : 7
    "JWT (jjwt)" : 3
    "Databases (PostgreSQL, H2)" : 2
    "Testing" : 2
    "API docs (springdoc)" : 1
```

### 🔑 Environment variables by group (14 total)

```mermaid
pie showData title .env.example variables by group
    "Redis" : 4
    "Database" : 3
    "Server and CORS" : 2
    "RailRadar API" : 2
    "Gemini API" : 2
    "JWT" : 1
```

### 📈 Same data as a bar chart

```mermaid
xychart-beta
    title "Environment variables per group"
    x-axis ["Redis", "Database", "Server", "RailRadar", "Gemini", "JWT"]
    y-axis "Count" 0 --> 5
    bar [4, 3, 2, 2, 2, 1]
```

### 🐳 Docker Compose services

| Service | Image | Host Port | Health Check | Volume |
|---|---|---|---|---|
| `backend` | built from `Dockerfile` | `8080` | waits for DB + Redis healthy | none |
| `postgres` | `postgres:16-alpine` | `5432` | `pg_isready` every 5s, 5 retries | `postgres_data` |
| `redis` | `redis:7-alpine` | `6379` | `redis-cli ping` every 5s, 5 retries | `redis_data` |

### 🧩 Feature map

```mermaid
mindmap
  root((RailTrack<br/>Backend))
    Auth
      Spring Security
      JWT tokens
    Data
      PostgreSQL
      Spring Data JPA
      Redis
    Integrations
      RailRadar live data
      Gemini RailAI
    Ops
      Docker Compose
      Actuator health
      Swagger UI
      Maven Wrapper
```

> 📉 **Not included on purpose:** language-percentage charts, commit-activity charts and API latency charts. I could not read that data, and inventing it would be misleading. Section [Roadmap & TODOs](#-roadmap--todos) explains how to add real ones.

---

## 📁 Project Structure

Root files confirmed from the repository:

```text
Railway_Tracking_Backend/
├── .mvn/wrapper/          # Maven wrapper config
├── src/                   # Application source (main + test)
├── .dockerignore
├── .env.example           # Environment variable template
├── .gitignore
├── Dockerfile
├── docker-compose.yml     # backend + postgres + redis
├── mvnw                   # Maven wrapper (Unix)
├── mvnw.cmd               # Maven wrapper (Windows)
├── pom.xml                # Spring Boot 3.3.2, Java 17
└── system.properties      # Platform runtime config
```

<!-- TODO: expand src/ tree (controller, service, repository, model, security, config) from your actual packages -->

---

## 🔑 Environment Variables

Copy `.env.example` and fill in your own values.

| Variable | Purpose | Notes |
|---|---|---|
| `PORT` | Server port | Default `8080` locally |
| `FRONTEND_URL` | Allowed CORS origins | Comma-separated, e.g. production + `http://localhost:5173` |
| `DATABASE_URL` | PostgreSQL connection | Accepts `postgres://…` or `jdbc:postgresql://…` |
| `DATABASE_USERNAME` | DB user | |
| `DATABASE_PASSWORD` | DB password | |
| `REDIS_URL` | Redis connection URL | Optional |
| `REDIS_HOST` | Redis host | Optional, default `localhost` |
| `REDIS_PORT` | Redis port | Optional, default `6379` |
| `REDIS_PASSWORD` | Redis password | Optional |
| `JWT_SECRET` | JWT signing key | Base64, minimum 256 bits |
| `RAILRADAR_API_KEY` | RailRadar auth | Required for live data |
| `RAILRADAR_BASE_URL` | RailRadar endpoint | `https://api.railradar.in/v1` in template |
| `GEMINI_API_KEY` | Gemini auth | Required for RailAI |
| `GEMINI_API_URL` | Gemini endpoint | Template uses `gemini-2.5-flash` |

---

## 🚀 Getting Started

### Prerequisites

- JDK **17**
- Docker + Docker Compose (for the one-command setup)
- A RailRadar API key and a Gemini API key

### Option A: Docker Compose (recommended)

```bash
git clone https://github.com/ashrithBalaji456/Railway_Tracking_Backend.git
cd Railway_Tracking_Backend

# provide your keys (compose reads these from your shell or a .env file)
export RAILRADAR_API_KEY=your_key
export GEMINI_API_KEY=your_key
export JWT_SECRET=$(openssl rand -base64 32)

docker compose up --build
```

The API will be available at `http://localhost:8080`.

### Option B: Run locally with Maven

```bash
cp .env.example .env     # then edit values
# make sure PostgreSQL (and optionally Redis) are running, then:
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

### Run tests

```bash
./mvnw test
```

---

## 📖 API Documentation

`springdoc-openapi-starter-webmvc-ui` 2.5.0 is installed, so interactive docs should be served at:

| Resource | Typical default path |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Health | `http://localhost:8080/actuator/health` |

> ⚠️ These are the springdoc / Actuator defaults. Your security config or properties may change them, so please verify against the running app.

### Endpoint reference

| Module | Method | Path | Auth | Description |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/v1/auth/register` | ❌ | User registration with username, email, password |
| **Auth** | `POST` | `/api/v1/auth/login` | ❌ | User login returning JWT Bearer token |
| **Auth** | `GET` | `/api/v1/auth/me` | ✅ | Current authenticated user profile |
| **Trains** | `GET` | `/api/v1/trains` | ❌ | Search trains by name or number |
| **Trains** | `GET` | `/api/v1/trains/{trainNumber}` | ❌ | Full train schedule & metadata |
| **Trains** | `GET` | `/api/v1/trains/{trainNumber}/live` | ❌ | Real-time GPS location, run status & delay |
| **Trains** | `GET` | `/api/v1/trains/{trainNumber}/route` | ❌ | Station-by-station route schedule |
| **Trains** | `GET` | `/api/v1/trains/{trainNumber}/delay` | ❌ | Historical delay analysis & punctuality grade |
| **Trains** | `GET` | `/api/v1/trains/{trainNumber}/fare` | ❌ | Class fare breakdown (1A, 2A, 3A, SL, etc.) |
| **Trains** | `GET` | `/api/v1/trains/between` | ❌ | Direct and alternative trains between station pair |
| **Stations** | `GET` | `/api/v1/stations` | ❌ | Station search & autocomplete |
| **Stations** | `GET` | `/api/v1/stations/{code}` | ❌ | Station metadata, platform & junction details |
| **Stations** | `GET` | `/api/v1/stations/{code}/trains` | ❌ | Station departure board trains |
| **Stations** | `GET` | `/api/v1/stations/{code}/live` | ❌ | Real-time station live board (2-8 hours ahead) |
| **Disruptions** | `GET` | `/api/v1/disruptions/exceptions` | ❌ | Active disruptions (cancelled, diverted, rescheduled) |
| **Disruptions** | `GET` | `/api/v1/disruptions/summary` | ❌ | Network disruption counts & category metrics |
| **Journey** | `GET` | `/api/v1/journey/plan` | ❌ | Direct & connecting multi-leg journey solver |
| **Journey** | `POST` | `/api/v1/journey/overlay` | ❌ | Historical delay overlay for journey legs |
| **RailAI** | `POST` | `/api/v1/ai/chat` | ❌ | Intelligent railway assistant powered by Gemini |
| **Bookmarks** | `GET` | `/api/v1/favorites` | ✅ | User saved favorite trains and stations |
| **Bookmarks** | `POST` | `/api/v1/favorites` | ✅ | Add new bookmark |
| **Bookmarks** | `DELETE` | `/api/v1/favorites/{id}` | ✅ | Remove bookmark |
| **Alerts** | `GET` | `/api/v1/alerts` | ✅ | User tracking alerts |
| **History** | `GET` | `/api/v1/history` | ✅ | Cloud-synced user search history |
| **System** | `GET` | `/actuator/health` | ❌ | Spring Boot Actuator health check |

---

## ☁️ Deployment

| Part | Where | Evidence |
|---|---|---|
| Frontend | Vercel | https://railway-tracking-frontend.vercel.app/ |
| Backend | Render (most likely) | wording in `.env.example` |
| Database | Managed PostgreSQL | `.env.example` |
| Cache | Managed Redis (optional) | `.env.example` |

Set every variable from the table above in your host's dashboard. Make sure `FRONTEND_URL` includes the exact Vercel URL so CORS allows the browser.

---

## 🔒 Security Notes

- ⚠️ `docker-compose.yml` and `.env.example` both contain a **default `JWT_SECRET`** value. Anyone who reads the repo knows it. **Generate a new one for every real deployment** (`openssl rand -base64 32`).
- The compose file uses `postgres` / `postgres` as DB credentials. Fine for local dev, not for production.
- Never commit a real `.env`. `.gitignore` is present, but confirm `.env` is listed.
- Keep `RAILRADAR_API_KEY` and `GEMINI_API_KEY` server-side only.
- `h2` has no `<scope>` in `pom.xml`, so it ships in the runtime classpath. Consider `runtime` or `test` scope if it is only for tests.

---

## 🗺 Roadmap & TODOs

- [ ] Fill the **endpoint table** from Swagger
- [ ] Expand the **`src/` tree** with real package names
- [ ] Add an **ER diagram** once entities are confirmed
- [ ] Add a **CI badge** after setting up GitHub Actions
- [ ] Add real **language / commit charts** with a generator, for example GitHub Insights or a stats-card service (verify availability first)
- [ ] Add a `LICENSE` file and a license badge

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=12,20,24&height=100&section=footer" width="100%" alt="footer"/>

Made with ☕ and 🚆 by **[ashrithBalaji456](https://github.com/ashrithBalaji456)**

⭐ Star the repo if it helped you.

</div>
