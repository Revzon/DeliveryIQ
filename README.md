# DeliveryIQ

Enterprise logistics platform for shipment tracking, route optimization, driver assignment, and operational intelligence.

## Stack

- Java 21 / Spring Boot 3
- PostgreSQL + Flyway
- Redis cache
- Apache Kafka (tracking events)
- React + TypeScript
- Docker Compose / Kubernetes
- GitHub Actions CI

## Modules

| Area | Highlights |
|------|------------|
| Tracking | Create/get/track shipments, timeline, status transitions, Kafka consumer |
| Routing | Route planner, ETA, driver assignment, conflict detection |
| Analytics | On-time %, route efficiency, delayed stats, dashboard KPIs |
| Security | JWT login/validate, role-based method security |
| Cache | Redis-backed dashboard and route optimization cache |

## Quick start

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- UI: `http://localhost:5173`

Demo users (`POST /api/auth/login`): `dispatcher` / `dispatch123`, `analyst` / `analyst123`, `admin` / `admin123`.

## Team

- Olha Revzon — Tech Lead
- Pavlo Kislov — Backend
- Anatolii Sichkar — Frontend
- Oleksandr Bekshaiev — QA / DevOps
