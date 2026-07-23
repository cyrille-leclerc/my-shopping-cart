# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

My Shopping Cart is a microservices-based e-commerce demonstration application designed to showcase OpenTelemetry instrumentation across multiple services. It consists of 5 backend services plus supporting infrastructure (PostgreSQL, Redis, RabbitMQ, OpenTelemetry Collector).

## Architecture

### Services (Java Spring Boot 4.0.6, Java 25)

1. **Frontend** (`frontend/`)
   - REST API endpoints for orders and product management
   - Spring Boot web app with JPA/Hibernate, Redis cache, gRPC client integration
   - OpenTelemetry auto-instrumentation with custom business metrics (OrderValueRecorder)
   - Demonstrates: HTTP traces, custom metrics (Sales, Transactions, Average Order Value), Redis cache hits/misses
   - Port: 8080 (frontend), 8079 (load balancer replica)

2. **Checkout Service** (`checkout/`)
   - Pure gRPC service (no HTTP), protobuf-based communication
   - Communicates with Shipping service via HTTP
   - Uses Prometheus metrics exporter directly (not Spring Boot)
   - Custom implementation without Spring Boot framework
   - Port: 50051 (gRPC), 9400 (Prometheus metrics)

3. **Fraud Detection** (`fraud-detection/`)
   - HTTP REST endpoint for fraud checks
   - Spring Boot web app with JDBC (no JPA)
   - Database: PostgreSQL
   - Port: 8081

4. **Warehouse** (`warehouse/`)
   - RabbitMQ message consumer (async processing)
   - Spring Boot AMQP integration
   - Port: 8089

5. **Shipping** (`shipping/`)
   - HTTP REST endpoint for shipping operations
   - Spring Boot web app with JDBC
   - Called by Checkout service
   - Port: 8088

### Infrastructure

- **PostgreSQL 17**: Main database for orders, products, users (database: `my_shopping_cart` or `ecommerce` in Docker)
- **Redis 6**: Product cache for Frontend service
- **RabbitMQ 4**: Message queue for order notifications to Warehouse
- **OpenTelemetry Collector**: Receives traces/metrics/logs via gRPC/HTTP and forwards to Grafana Cloud
- **K6 Load Generator**: Load testing tool (Grafana K6) for generating synthetic traffic

### Data Flow

1. Frontend receives order → calls Checkout gRPC → Checkout calls Shipping HTTP → Warehouse consumes RabbitMQ messages
2. Frontend caches products in Redis
3. All services send observability data to OpenTelemetry Collector → Grafana Cloud
4. Load generator continuously hits Frontend with synthetic orders

## Build System

**Maven 4.0.6** with multi-module parent project (`pom.xml`).

### Module Structure
```
parent/
├── frontend/         (Spring Boot web, Node.js frontend build)
├── fraud-detection/  (Spring Boot web)
├── checkout/         (Pure gRPC Java)
├── warehouse/        (Spring Boot AMQP consumer)
├── shipping/         (Spring Boot web)
├── checkout-protobuff/ (Protobuf definitions for gRPC)
└── otel-javaagent-extensions/ (Custom OTel instrumentation - currently disabled in pom.xml)
```

### Build Commands

**From root directory:**
- `./mvnw clean package` - Build all modules (creates JARs in `target/` directories)
- `./mvnw -DskipTests package` - Build without running tests
- `./mvnw clean install` - Build and install to local Maven cache
- `./mvnw test` - Run all tests
- `./mvnw -pl frontend test` - Run tests for specific module

**Build Docker images:**
- Checkout: `mvn com.google.cloud.tools:jib-maven-plugin:dockerBuild` (uses Jib plugin)
- Frontend: `mvn spring-boot:build-image` (Spring Boot Buildpacks)
- Other services: Similar to Frontend

**Build and push multi-platform images:**
- `./build-and-push-multiplatform` (custom script for linux/amd64 and linux/arm64)

### Protobuf Compilation

Frontend and Checkout have `protobuf-maven-plugin` configured to generate Java gRPC code from `.proto` files. Compilation happens automatically during `mvn package`.

### Frontend JavaScript Build

Frontend includes a Node.js sub-project (`src/main/js/`):
- `frontend-maven-plugin` downloads Node v21.7.3 and npm 10.8.3
- Runs `npm install` and `npm run build` during compile phase
- Built artifacts bundled into the Spring Boot JAR

## Running Locally

### Prerequisites
- Java 25+
- Maven (or use `./mvnw` wrapper)
- PostgreSQL (port 5432)
- Redis (port 6379)
- RabbitMQ (port 5672)
- OpenTelemetry Collector or gRPC endpoint (port 4317/4318)
- K6 (for load testing)

### Setup Local Database

```bash
# Create PostgreSQL database (macOS example)
brew install postgresql
brew services start postgresql
psql postgres
  CREATE DATABASE my_shopping_cart;
  CREATE USER my_shopping_cart WITH PASSWORD 'my_shopping_cart';
  GRANT ALL PRIVILEGES ON DATABASE my_shopping_cart TO my_shopping_cart;
```

### Start Services (Each in Separate Terminal)

```bash
# 1. PostgreSQL
./run-postgresql

# 2. Redis
cd redis && ./run-redis.sh

# 3. RabbitMQ
./run-rabbitmq

# 4. OpenTelemetry Collector (requires .env with Grafana Cloud credentials)
./run-otelcol

# 5-9. Start each service with their run scripts:
cd fraud-detection && ./run-fraud-detection
cd checkout && ./run-checkout
cd warehouse && ./run-warehouse
cd shipping && ./run-shipping
cd frontend && ./run-frontend

# 10. Load generator (optional)
cd load-generator && ./run-load-generator
```

### Run Scripts Details

Each service has a `run-*` script that:
1. Sources environment from `setenv.sh` or `setenv.default.sh` (defaults: OTEL agent v2.17.0, Pyroscope v0.18.0)
2. Downloads OpenTelemetry and Pyroscope Java agents if missing (cached in `.otel/` and `.pyroscope/`)
3. Builds the service with Maven
4. Runs the JAR with appropriate OTel/Pyroscope javaagent flags

**Key env vars** (see `setenv.default.sh`):
- `OPEN_TELEMETRY_AGENT_VERSION=2.17.0`
- `DEPLOYMENT_ENVIRONMENT_NAME=staging`
- `PYROSCOPE_AGENT_VERSION=0.18.0`

## Docker Compose Deployment

### Quick Start with Docker Compose

```bash
docker compose build
# Create .env file with Grafana Cloud credentials:
cat > .env << 'ENVEOF'
GRAFANA_CLOUD_INSTANCE_ID=<your-id>
GRAFANA_CLOUD_API_KEY=<your-key>
GRAFANA_CLOUD_OTLP_ENDPOINT=https://otlp-gateway-prod-eu-west-0.grafana.net/otlp
ENVEOF
docker compose up
```

**Compose file:** `compose.yaml` (replaces older `docker-compose.yml`)

**Services defined:**
- frontend (2 replicas, ports 8079-8080)
- fraud-detection (port 8081)
- checkout (ports 50051, 9400)
- shipping (port 8088)
- warehouse (port 8089)
- postgres, redis, rabbitmq
- otel-collector
- k6-load-generator

All services automatically receive OpenTelemetry instrumentation via labels and environment variables. Frontend scales to 2 replicas.

## Kubernetes Deployment

### K3d Quick Start

```bash
./run-k8s.sh
```

This script:
1. Builds Docker images for all 5 services
2. Creates a K3d cluster named `my-shopping-cart`
3. Imports images into the cluster
4. Applies K8s manifests from `kubernetes/app/`
5. Forwards ports: frontend:8080, lgtm (observability stack):3000

### Kubernetes Structure (`kubernetes/`)
- `app/` - Application deployments (Frontend, Checkout, Fraud Detection, Warehouse, Shipping, PostgreSQL, Redis, RabbitMQ)
- `opentelemetry-kube-stack/` - OpenTelemetry Helm charts (see linked upstream repo)
- `prometheus/` - Prometheus configuration
- `jaeger/` - Jaeger distributed tracing
- `dd-operator/` - Datadog operator integration (optional)

## Testing

Currently, only `frontend/` module has tests in `src/test/`. Run with:

```bash
./mvnw -pl frontend test
```

or from frontend directory:

```bash
cd frontend && ../../mvnw test
```

## OpenTelemetry Configuration

### Auto-Instrumentation
Services are instrumented via:
1. OpenTelemetry Java Agent javaagent (automatic SDK injection)
2. Spring Boot OTel starter (for Spring services)
3. Custom instrumentation annotations in code

### Key OTel System Properties (set in run scripts)
```
-Dotel.exporter.otlp.endpoint=http://localhost:4318  # HTTP receiver
-Dotel.semconv-stability.opt-in=http,database        # Stability conventions
-Dotel.instrumentation.logback-appender.experimental-log-attributes=true
-Dotel.java.experimental.span-attributes.copy-from-baggage.include=*
```

### Custom Metrics
**Frontend** implements business KPI metrics using OTel API:
- `OrderValueRecorder` - Custom meter for Sales, Transactions, Average Order Value
- Available in `OrderController` with per-request metrics

### Observability Stack Integration
- **Grafana Cloud** as the primary backend (configured in `.env`)
- Services export via OpenTelemetry Collector to Grafana Cloud OTLP endpoint
- Logs are captured via Logback appender instrumentation
- Metrics available at Prometheus endpoints (e.g., Checkout on port 9400)

## Important Implementation Patterns

### Database Configuration
- **Frontend & Fraud Detection & Shipping:** Use Spring Data JPA with Hibernate DDL auto-update
- **Checkout & Warehouse:** Use plain JDBC (no ORM)
- Database URL typically: `jdbc:postgresql://localhost:5432/my_shopping_cart`
- Username/password: `my_shopping_cart/my_shopping_cart` (local) or `ecommerce/ecommerce` (Docker)

### Service Communication
- **Frontend → Checkout:** gRPC (port 50051, protobuf)
- **Frontend → Fraud Detection:** HTTP REST (port 8081)
- **Checkout → Shipping:** HTTP REST (port 8088)
- **Frontend → Warehouse:** RabbitMQ messages (asynchronous)
- **Frontend → Redis:** Cache integration (port 6379)

### Dependency Versions (Aligned Across Services)
- Spring Boot: 4.0.6
- OpenTelemetry: 1.63.0
- OpenTelemetry Instrumentation: 2.29.0
- gRPC: 1.77.0
- Protobuf: 4.33.1
- Java: 25 (source/target/release)

## Key Files to Know

- `pom.xml` - Parent POM with modules
- `compose.yaml` - Docker Compose orchestration
- `setenv.default.sh` - Default environment variables for local runs
- `run-k8s.sh` - Kubernetes deployment script
- `.claude/settings.local.json` - Claude Code permission allowlist (kubectl, docker buildx, etc.)
- `README.md` - High-level project overview and demos
- `docs/` - Architecture diagrams, OpenTelemetry configuration examples, logs configuration

## Development Notes

### Adding a New Service
1. Create new module directory with `pom.xml` (inherit from parent groupId/version)
2. Add module to root `pom.xml`
3. Create Dockerfile using Spring Boot Buildpacks pattern or custom multi-stage build
4. Add service definition to `compose.yaml`
5. Add K8s manifests to `kubernetes/app/`
6. Create `run-<service>` script sourcing `setenv.default.sh`

### Modifying Protobuf Definitions
1. Edit `.proto` files in `checkout-protobuff/` or appropriate module
2. Run `./mvnw clean package` - protobuf-maven-plugin generates Java code automatically
3. Regenerated classes go to `target/generated-sources/protobuf/`

### Debugging Services Locally
- Set `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318` if running your own collector
- Check logs in `logs/` directory (path set in run scripts)
- Spring Boot actuator endpoints: `http://localhost:<port>/actuator/health`, `/actuator/metrics`
- Checkout Prometheus metrics: `http://localhost:9400/metrics`

### Common Issues
- **Database connection refused:** Ensure PostgreSQL is running and database `my_shopping_cart` exists
- **gRPC connection errors:** Verify Checkout service is listening on port 50051
- **OTel agent download fails:** Check network; agents are cached in `.otel/` and `.pyroscope/` directories
- **Port conflicts:** Services use fixed ports (8080, 8081, 8088, 8089, 50051); adjust in run scripts or compose.yaml

## Container Registry

Services use Jib and Spring Boot build image plugins with GitHub Container Registry (ghcr.io):
- Image naming: `ghcr.io/cyrille-leclerc/<service>:<version>`
- Auth via environment variables: `CONTAINER_REGISTRY_USERNAME`, `CONTAINER_REGISTRY_PASSWORD`
- Multi-platform support: `linux/amd64` and `linux/arm64` (see `build-and-push-multiplatform`)
