# Google Meet Clone — 5 Microservices Architecture (Spring Boot)

This repository contains the complete 5-microservice architecture for a full-featured Google Meet clone built with Java and Spring Boot.

## Microservices Overview

| Microservice | Port | Database / State | Key Responsibilities |
| :--- | :--- | :--- | :--- |
| **identity-service** | 8081 | PostgreSQL (`identity_db`) | Auth (JWT, Refresh tokens), OAuth2 (Google), User profile management, Kafka user events |
| **meeting-service** | 8082 | PostgreSQL (`meeting_db`) | Meeting creation, short join codes, participants, chat persistence, Feign client for tokens |
| **signaling-service** | 8083 | Redis + Kurento | WebSocket signaling, SDP Offer/Answer, ICE candidates, MediaPipeline handling, host controls |
| **recording-service** | 8084 | PostgreSQL (`recording_db`) | Kurento RecorderEndpoint management, AWS S3 upload, signed playback URLs |
| **notification-service**| 8085 | PostgreSQL (`notification_db`) + Redis | User presence, WebSocket alerts, waiting room admission, Email/Push notifications |

## Architecture Notes
- **Media Plane**: Powered by Kurento Media Server driven via the official `kurento-client` Java library.
- **Inter-service Communication**:
  - Event-driven via Kafka (`user-events`, `meeting-events`, `recording-events`).
  - Synchronous calls via OpenFeign for fast room token generation and waiting room approvals.
- **Security**: Local JWT public key verification on each service (no network bottleneck per request).
- **Database per Service**: Independent schemas (`identity_db`, `meeting_db`, `recording_db`, `notification_db`).

## Running with Docker & Nginx

### 1. Run Backing Infrastructure Only (Local IDE Dev Mode)
If you are developing inside your IDE and only need Postgres, Redis, Kafka, and Kurento:
```bash
docker compose -f docker-compose.infra.yml up -d
```

### 2. Run Full Cluster (Microservices + Infra + Nginx Gateway)
To start all 5 microservices behind the unified Nginx API Gateway on port `80`:
```bash
docker compose --env-file .env.docker up -d --build
```

### 3. Nginx API Gateway Routing (Port 80)
- `http://localhost/auth/**` -> `identity-service:8081`
- `http://localhost/users/**` -> `identity-service:8081`
- `http://localhost/meetings/**` -> `meeting-service:8082`
- `ws://localhost/ws/**` -> `signaling-service:8083` (WebRTC signaling)
- `http://localhost/recordings/**` -> `recording-service:8084`
- `http://localhost/notifications/**` -> `notification-service:8085`
- `http://localhost/internal/**` -> Blocked (403 Forbidden)
- `http://localhost/health` -> Gateway health status
