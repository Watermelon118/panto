# Panto 生产部署说明

本文档说明如何使用 `deploy/docker-compose.prod.yml` 在本地模拟生产环境，并为后续部署到 AWS EC2 做准备。

## 1. 部署结构

生产 Compose 包含四个服务：

```text
浏览器
  |
  v
gateway  外层 Nginx，公网入口，监听 80
  |-- /api/  -> api:8080
  |-- /      -> web:80

web       前端 Nginx，提供 React dist 静态文件
api       Spring Boot 后端
postgres  PostgreSQL 15，数据保存在 Docker volume
```

公网只需要访问 `gateway`。`api` 和 `postgres` 不直接暴露到公网。

## 2. 环境变量

先复制环境变量模板：

```powershell
cd D:\practices\panto\deploy
Copy-Item .env.example .env
```

然后编辑 `deploy/.env`，至少替换下面三个值：

```text
POSTGRES_PASSWORD=替换成强随机数据库密码
PANTO_JWT_ACCESS_TOKEN_SECRET=替换成强随机 Access Token 密钥
PANTO_JWT_REFRESH_TOKEN_SECRET=替换成强随机 Refresh Token 密钥
```

注意：

- `deploy/.env` 会被 `.gitignore` 忽略，不要提交真实密码或密钥。
- `PANTO_JWT_REFRESH_COOKIE_SECURE=true` 用于生产 HTTPS 环境。
- 如果只是本地 HTTP 模拟，浏览器可能不会保存 `Secure` cookie；这是正常现象。完整登录联调应在 HTTPS 环境验证。

## 3. 本地生产模拟启动

在 `deploy` 目录执行：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

启动后查看服务状态：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml ps
```

四个服务都应该是 `healthy`：

```text
gateway
web
api
postgres
```

## 4. 验证

检查网关健康状态：

```powershell
curl.exe -i http://127.0.0.1/nginx-health
```

预期返回：

```text
HTTP/1.1 200 OK
ok
```

检查前端首页：

```powershell
curl.exe -i http://127.0.0.1/
```

检查 React Router 刷新回退：

```powershell
curl.exe -i http://127.0.0.1/products
```

预期返回 `200`，内容包含 `<!doctype html>`。

检查 API 代理：

```powershell
curl.exe -i -X POST http://127.0.0.1/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d "{}"
```

预期返回后端的参数校验错误，说明 `/api/` 已经正确转发到 Spring Boot。

## 5. 查看日志

查看所有服务日志：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml logs -f
```

只查看后端日志：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml logs -f api
```

后端请求日志会包含：

```text
traceId
method
path
status
durationMs
user
clientIp
userAgent
```

当用户反馈问题时，可以通过响应头 `X-Trace-Id` 反查后端日志。

## 6. 停止和清理

停止服务但保留数据库数据：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml down
```

停止服务并删除数据库 volume：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml down -v
```

注意：`down -v` 会删除 PostgreSQL 数据，只能用于本地测试或明确要清空数据时。

## 7. AWS EC2 部署前准备

部署到 AWS 前需要准备：

```text
EC2 实例：建议先用 Ubuntu 22.04 / 24.04 LTS
安全组：开放 80 和 443；SSH 只允许自己的 IP，或使用 SSM Session Manager
Elastic IP：固定公网 IP
域名 DNS：A 记录指向 Elastic IP
Docker：安装 Docker Engine 和 Docker Compose plugin
deploy/.env：在服务器上手动创建，填真实密码和密钥
```

第一阶段可以先通过：

```text
http://EC2公网IP
```

验证服务。配置域名和证书后，最终访问：

```text
https://你的域名
```

## 8. HTTPS 后续工作

当前 `docker-compose.prod.yml` 只监听 HTTP 80。正式上线还需要补 HTTPS：

```text
1. 域名解析到 EC2 Elastic IP
2. 使用 Certbot 申请 Let's Encrypt 证书
3. 参考 deploy/nginx/https.conf.example 配置 gateway Nginx 的 443 server
4. HTTP 80 自动跳转 HTTPS
5. 保持后端 Refresh Token cookie 的 Secure=true
```

没有 HTTPS 时，浏览器不会在普通 HTTP 页面中稳定使用 `Secure` cookie。因此正式登录流程应在 HTTPS 配置完成后验收。

## 9. 常用命令

构建并启动：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

查看状态：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml ps
```

查看日志：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml logs -f
```

停止服务：

```powershell
docker compose --env-file .env -f docker-compose.prod.yml down
```
