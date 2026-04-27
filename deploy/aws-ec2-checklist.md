# AWS EC2 部署清单

本文档用于把 Panto 部署到 AWS EC2。当前目标是单机部署：一台 EC2 上运行 Docker Compose，包含 `gateway`、`web`、`api`、`postgres` 四个容器。

## 1. AWS 资源准备

### 1.1 区域

建议选择：

```text
ap-southeast-2
```

这是 AWS Sydney 区域，离 New Zealand 较近，网络延迟通常比美国或欧洲低。

### 1.2 EC2 系统

建议使用 Ubuntu LTS：

```text
Ubuntu Server 24.04 LTS
```

也可以用 Ubuntu Server 22.04 LTS。不要使用 CentOS 作为新部署基础系统。

### 1.3 实例规格

初始建议：

```text
t3.small
```

如果只是演示或低频使用，可以先用更小规格试运行。但 PostgreSQL、Spring Boot 和前端/网关一起跑在同一台机器上，内存太小会比较紧张。

### 1.4 磁盘

建议最小：

```text
20GB gp3
```

如果后续会保留较多订单、审计日志和导出文件，可以提高到 30GB 或更多。

## 2. 安全组

入站规则建议：

```text
TCP 22    只允许你的固定公网 IP，或不开放 SSH 改用 SSM
TCP 80    0.0.0.0/0 和 ::/0
TCP 443   0.0.0.0/0 和 ::/0
```

不要开放：

```text
TCP 5432  PostgreSQL 不允许公网访问
TCP 8080  Spring Boot 不允许公网访问
```

后端和数据库只在 Docker 内部网络通信。

## 3. Elastic IP

给 EC2 绑定 Elastic IP。

原因：

- EC2 停止再启动后，普通公网 IP 可能变化。
- 域名 DNS 需要指向稳定 IP。
- HTTPS 证书申请和后续访问都依赖稳定入口。

## 4. 域名 DNS

如果使用 Route 53 或其他 DNS 服务商，添加 A 记录：

```text
panto.example.com  A  EC2_Elastic_IP
```

DNS 生效前可以先用公网 IP 验证 HTTP：

```text
http://EC2_Elastic_IP
```

HTTPS 阶段建议使用域名：

```text
https://panto.example.com
```

## 5. 连接服务器

使用 SSH：

```bash
ssh -i your-key.pem ubuntu@EC2_Elastic_IP
```

如果使用 AWS Systems Manager Session Manager，则不需要开放 SSH 到公网，但需要额外配置 EC2 IAM role 和 SSM Agent。

## 6. 安装 Docker

在 Ubuntu 上执行：

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

允许当前用户使用 Docker：

```bash
sudo usermod -aG docker ubuntu
```

退出服务器后重新登录，再验证：

```bash
docker version
docker compose version
```

## 7. 获取代码

在服务器上选择部署目录：

```bash
mkdir -p ~/apps
cd ~/apps
```

拉取代码：

```bash
git clone <你的仓库地址> panto
cd panto
```

如果仓库是私有仓库，建议使用 GitHub deploy key 或 GitHub CLI 登录，不要把个人 token 写进脚本。

## 8. 创建生产环境变量

复制模板：

```bash
cd ~/apps/panto/deploy
cp .env.example .env
```

编辑：

```bash
nano .env
```

必须替换：

```text
POSTGRES_PASSWORD
PANTO_JWT_ACCESS_TOKEN_SECRET
PANTO_JWT_REFRESH_TOKEN_SECRET
```

生成随机密钥可以使用：

```bash
openssl rand -base64 48
```

注意：

- `deploy/.env` 不提交 Git。
- 数据库密码和 JWT 密钥上线后不要随意改。
- 如果改 JWT 密钥，已有登录 token 会全部失效。

## 9. 启动生产 Compose

```bash
cd ~/apps/panto/deploy
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

查看状态：

```bash
docker compose --env-file .env -f docker-compose.prod.yml ps
```

预期四个服务都是 healthy：

```text
gateway
web
api
postgres
```

## 10. HTTP 验证

在服务器上验证：

```bash
curl -i http://127.0.0.1/nginx-health
curl -i http://127.0.0.1/
curl -i http://127.0.0.1/products
```

在本机浏览器验证：

```text
http://EC2_Elastic_IP
```

如果 DNS 已生效：

```text
http://panto.example.com
```

## 11. 日志检查

查看所有日志：

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f
```

查看后端日志：

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f api
```

查看 Nginx 网关日志：

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f gateway
```

## 12. 重启和更新

普通重启：

```bash
docker compose --env-file .env -f docker-compose.prod.yml restart
```

更新代码后重新构建并启动：

```bash
cd ~/apps/panto
git pull
cd deploy
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

## 13. 停止服务

停止服务但保留数据库数据：

```bash
docker compose --env-file .env -f docker-compose.prod.yml down
```

不要在生产环境随意执行：

```bash
docker compose --env-file .env -f docker-compose.prod.yml down -v
```

`down -v` 会删除 PostgreSQL 数据卷。

## 14. 备份

最低要求：上线前先确认可以备份数据库。

手动导出：

```bash
docker compose --env-file .env -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > panto-backup.sql
```

如果上面的环境变量没有在当前 shell 中定义，可以直接写 `.env` 里的实际值：

```bash
docker compose --env-file .env -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U panto panto > panto-backup.sql
```

建议后续配置：

```text
EBS snapshot
定时 pg_dump
备份文件上传到 S3
```

## 15. HTTPS 前置检查

申请 HTTPS 前确认：

```text
域名 A 记录已经指向 EC2 Elastic IP
安全组开放 80 和 443
http://你的域名 可以访问前端
http://你的域名/nginx-health 返回 ok
```

之后再进行 Certbot / Let's Encrypt 配置。

仓库里已经提供 HTTPS 网关配置模板：

```text
deploy/nginx/https.conf.example
```

使用时需要把模板里的 `panto.example.com` 替换成真实域名，并确保证书路径和 Certbot 生成的路径一致。

## 16. 常见问题

### 16.1 浏览器打不开 EC2

检查：

```text
安全组是否开放 80
EC2 是否绑定 Elastic IP
gateway 容器是否 healthy
服务器防火墙是否阻止 80
```

### 16.2 API 请求失败

检查：

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f gateway
docker compose --env-file .env -f docker-compose.prod.yml logs -f api
```

确认前端生产 API 地址是：

```text
/api/v1
```

### 16.3 登录后 cookie 不生效

生产配置中：

```text
PANTO_JWT_REFRESH_COOKIE_SECURE=true
```

这要求浏览器通过 HTTPS 访问。HTTP 阶段只适合验证页面、API 代理和健康检查，不适合最终验收完整登录流程。

### 16.4 数据库连接失败

检查：

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
SPRING_DATASOURCE_URL
```

并查看：

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f postgres
docker compose --env-file .env -f docker-compose.prod.yml logs -f api
```
