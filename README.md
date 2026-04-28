# Panto WMS

Panto is a single-warehouse food warehouse management system for small B2B wholesale businesses. The reference scenario is a New Zealand food wholesaler that needs practical workflows for products, customers, inventory, inbound stock, orders, stock destruction, reports, and audit logs.

Live site: [https://panto-wms.com](https://panto-wms.com)   Account: admin    Password: bcptdtptp0


## Features

- Product and SKU management
- Customer management
- User and role based access control
- Goods receipt and batch tracking
- Stock summary, batch list, and stock transaction views
- FIFO-oriented stock deduction foundations
- Order, invoice, and export workflows
- Expired stock destruction records
- Dashboard and reporting views
- Immutable-style audit log foundation
- Production Docker deployment with HTTPS gateway

## Tech Stack

Backend:

- Java 21
- Spring Boot
- Spring Security with JWT
- Spring Data JPA and Hibernate
- PostgreSQL 15
- Flyway
- Maven
- JUnit 5, Mockito, and Testcontainers

Frontend:

- React
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- Zustand
- React Router
- Axios

Infrastructure:

- Docker Compose
- Nginx gateway
- Let's Encrypt certificates through Certbot
- AWS EC2 deployment target
- Cloudflare DNS

## Repository Layout

```text
panto/
├── panto-api/          Spring Boot backend
├── panto-web/          React frontend
├── deploy/             Production Docker and Nginx deployment files
├── docs/               Requirements, design, and API specification
├── .github/workflows/  CI/CD configuration
└── docker-compose.yml  Local PostgreSQL and pgAdmin services
```

## Prerequisites

- Java 21
- Maven wrapper support through `panto-api/mvnw`
- Node.js and npm
- Docker Desktop or Docker Engine

## Local Development

Start PostgreSQL and pgAdmin:

```bash
docker compose up -d
```

Run the backend:

```bash
cd panto-api
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd panto-api
.\mvnw.cmd spring-boot:run
```

Run the frontend in another terminal:

```bash
cd panto-web
npm install
npm run dev
```

The default local endpoints are:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/v1`
- PostgreSQL: `localhost:5432`
- pgAdmin: `http://localhost:5050`

## Local Checks

Backend:

```bash
cd panto-api
./mvnw test
./mvnw clean package
```

Frontend:

```bash
cd panto-web
npm run lint
npm run build
```

## Production Deployment

The production deployment files live in `deploy/`.

Current production shape:

- `gateway`: public Nginx entrypoint on 80 and 443
- `web`: React static frontend container
- `api`: Spring Boot backend container
- `postgres`: internal PostgreSQL container

Useful deployment docs:

- [Production deploy guide](deploy/README.md)
- [AWS EC2 checklist](deploy/aws-ec2-checklist.md)
- [HTTPS Nginx config](deploy/nginx/https.conf)

Production secrets must be provided through server-side environment files or environment variables. Do not commit `.env` files, database passwords, JWT secrets, private keys, or live admin credentials.

## Documentation

- [API specification](docs/api-spec.md)
- [System design](docs/design.md)
- [Requirements](docs/requirements.md)

## Domain Notes

- SKU: Stock Keeping Unit
- Batch: inbound shipment group with expiry date
- GRN: Goods Receipt Note
- FIFO: First In First Out stock deduction strategy
- Destroy: write off expired stock with loss recording
- Audit Log: immutable record of data modifications
