# Panto - Food Warehouse Management System

# some details
To prevent garbled characters when reading the file, please use UTF-8 encoding format when reading.

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
- 每改完一个文件后先停止，等待用户查看并确认，再继续下一个文件。
- 每次新增或修改文件时，都要说明这个文件的职责、这次改动的目的，以及关键代码的大致作用，必要时结合代码做简要讲解。
- 默认使用中文注释，并保留现有中文注释为中文，除非用户明确要求改成别的语言。
- 不要主动创建分支、提交 commit 或合并分支，除非用户明确要求。
- 默认由用户在 VS Code 中执行分支创建、提交和合并操作，AI 只负责给出步骤与代码修改。

### Milestone 2 Git Workflow
- 采用简化版企业 Git Flow：`main` 为稳定分支，`develop` 为 milestone2 集成分支。
- 所有 milestone 功能都从 `develop` 拉出 `feature/*` 分支开发。
- 功能分支自测通过后合并回 `develop`。
- `develop` 验证通过后，再合并回 `main`。



### Execution Preferences
- 默认不要一次性铺开很多文件，先从最小可落地的文件开始。
- 对于较大的功能，先完成后端骨架，再补测试和文档，最后再进入前端接入。
- milestone2 当前先按顺序推进：产品管理 -> 客户管理 -> 用户管理 -> 前端页面整合。
- 客户详情等接口在当前里程碑只返回已实现的基础数据，不提前承诺后续里程碑的数据结构。

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
