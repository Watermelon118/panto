# Panto - Food Warehouse Management System

## Project Overview
Panto is a single-warehouse management system for small food B2B businesses.
Reference business scenario: NZ-based food wholesale operation.

## Tech Stack

### Backend (panto-api/)
- Java 21
- Spring Boot 3.5.x
- Spring Security 6 + JWT
- Spring Data JPA + Hibernate
- PostgreSQL 15
- Flyway (DB migrations)
- Maven
- JUnit 5 + Mockito + Testcontainers

### Frontend (panto-web/)
- React 18 + TypeScript 5
- Vite + Tailwind CSS v4
- TanStack Query (server state)
- Zustand (client state)
- React Router v6
- Axios
- date-fns

## Project Structure

panto/
├── panto-api/          Spring Boot backend
├── panto-web/          React frontend
├── docs/               Design documents
├── .github/workflows/  CI/CD configs
└── docker-compose.yml

## Coding Standards

### Backend
- Package: com.panto.wms
- Layered architecture: Controller -> Service -> Repository
- All controllers return Result<T> wrapper
- Use @Transactional on service methods that write
- DTOs for API I/O, Entities for DB - never expose entities in API
- All public methods must have Javadoc
- Exceptions: custom BusinessException with error codes
- Logging: SLF4J with @Slf4j, no System.out

### Frontend
- Functional components + hooks only, no class components
- No any types
- API calls only via TanStack Query hooks in src/api/
- Reusable components in src/components/

### Git
- Branch naming: feature/xxx, fix/xxx, chore/xxx, docs/xxx
- Commit message: Conventional Commits
- feat: new feature
- fix: bug fix
- refactor: refactoring
- test: tests only
- docs: documentation only
- chore: tooling, config

## Security Rules (STRICT)
- Passwords: BCrypt cost=12. NEVER use MD5, SHA1, SHA256 for passwords.
- No secrets in code. Use environment variables.
- SQL: parameterized queries only, never string concatenation.
- JWT: Access Token 2h in memory, Refresh Token 7d in httpOnly cookie.
- Input validation: @Valid on all controller DTOs.

## Key Commands

### Backend
- Run: cd panto-api && ./mvnw spring-boot:run
- Test: cd panto-api && ./mvnw test
- Build: cd panto-api && ./mvnw clean package

### Frontend
- Dev: cd panto-web && npm run dev
- Build: cd panto-web && npm run build
- Lint: cd panto-web && npm run lint

### Full stack
- Start DB: docker-compose up -d
- Stop DB: docker-compose down

## Workflow Rules for AI
1. Before writing any code, explain your plan. Wait for confirmation.
2. Write small commits. One logical change per commit.
3. Never modify these files without asking:
   - .env files
   - application.yml
   - docker-compose.yml
   - .github/workflows/
4. Never skip writing tests for service-layer code.
5. For any DB schema change, create a Flyway migration file.
6. For any new REST endpoint, update docs/api-spec.md.

### Collaboration Preferences
- 进行任何写操作前，先告诉用户将要修改什么，并等待确认。
- 默认按文件逐个修改代码，除非用户明确同意一次改多个文件。
- 每改完一个文件，都要说明该文件新增、删除或修改了哪些内容。
- 默认使用中文注释，并保留现有中文注释为中文，除非用户明确要求改成别的语言。
- 不要主动创建分支、提交 commit 或合并分支，除非用户明确要求。

## Domain Terminology
- SKU: Stock Keeping Unit
- Batch: same inbound shipment group, has expiry date
- GRN: Goods Receipt Note
- Invoice: dual-copy (Customer Copy + Office Copy)
- FIFO: First In First Out batch deduction strategy
- Rollback: soft-delete an order and return stock
- Destroy: write off expired stock with loss recording
- Audit Log: immutable record of all data modifications

## Non-Goals (Do NOT implement)
- Online payment
- Order status workflow (paid/shipped/delivered)
- Customer self-registration
- Multi-warehouse
- Mobile app
- Supplier management
