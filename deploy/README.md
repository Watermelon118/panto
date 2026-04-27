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

## 8. HTTPS 配置

`docker-compose.prod.yml` 的 gateway 服务已经按 HTTPS 部署：监听 80 + 443，挂载 `nginx/https.conf`、`/etc/letsencrypt` 和 `/var/www/certbot`。第一次上线需要在 EC2 上先申请证书，并创建 Certbot webroot 目录。

### 8.1 前置条件

- 域名 A 记录解析到 EC2 Elastic IP（apex 和 www 都要）。
- EC2 安全组放行 80 和 443。
- HTTP 站点先跑通（用 IP 或域名访问 `http://...` 能看到登录页）。

### 8.2 申请证书（standalone，约 30 秒停机）

`nginx/https.conf` 引用了 `/etc/letsencrypt/live/panto-wms.com/` 下的证书；如果文件不存在 nginx 会启动失败，因此第一次必须先拿到证书再切换配置。可以用以下两种方式之一：

**方式 A — 临时停 gateway 用 standalone**（最简单）：

```bash
cd ~/apps/panto/deploy

# 短暂停 gateway 释放 80 端口
sudo docker compose --env-file .env -f docker-compose.prod.yml stop gateway

# Ubuntu 安装 certbot
sudo apt update && sudo apt install -y certbot

# 申请 SAN 证书（替换为你的真实邮箱和域名）
sudo certbot certonly --standalone \
  -d panto-wms.com -d www.panto-wms.com \
  --email your-email@example.com \
  --agree-tos --no-eff-email

# 创建续签 webroot 目录（必须，否则 gateway 挂载会失败）
sudo mkdir -p /var/www/certbot

# 启动 gateway，这次就是 HTTPS
sudo docker compose --env-file .env -f docker-compose.prod.yml up -d gateway
```

gateway 启动后，建议把证书续期方式保存为 webroot，避免后续自动续期时再停 gateway 抢占 80 端口：

```bash
sudo certbot certonly --webroot \
  -w /var/www/certbot \
  --cert-name panto-wms.com \
  -d panto-wms.com -d www.panto-wms.com \
  --force-renewal \
  --email your-email@example.com \
  --agree-tos --no-eff-email
```

**方式 B — 临时切回 HTTP 配置申请证书**（避免停机）：先把 compose 的 gateway volume 改回 `nginx/default.conf`，启动后用 `certbot --webroot -w /var/www/certbot ...` 申请，再切回 `nginx/https.conf`。

### 8.3 验证 HTTPS

```bash
# TLS 握手 + 证书信息
curl -vI https://panto-wms.com/nginx-health

# HTTP → HTTPS 跳转
curl -I http://panto-wms.com/

# www → apex 跳转
curl -I https://www.panto-wms.com/
```

浏览器登录后在 DevTools → Application → Cookies 检查 `refresh_token`：`HttpOnly` + `Secure` + `SameSite=Strict` + `Path=/api/v1/auth`。

### 8.4 自动续签

Let's Encrypt 证书 90 天到期。Ubuntu 装完 certbot 自带 `certbot.timer`，但默认续签后不会重载我们 docker 里的 nginx，需要补一个 deploy hook：

```bash
sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-panto-gateway.sh > /dev/null <<'EOF'
#!/bin/sh
set -eu
if /usr/bin/docker ps --format '{{.Names}}' | /usr/bin/grep -qx panto-gateway; then
  /usr/bin/docker exec panto-gateway nginx -s reload
fi
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-panto-gateway.sh

# 演练续签流程，确认无误
sudo certbot renew --cert-name panto-wms.com --dry-run --no-random-sleep-on-renew
```

容器名 `panto-gateway` 已在 compose 里通过 `container_name` 固定。

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
