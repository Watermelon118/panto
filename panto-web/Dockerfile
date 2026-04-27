# 第一阶段：构建前端静态文件。
# 使用 Node.js 镜像安装依赖并执行 Vite build，生成 dist 目录。
FROM node:22-bookworm-slim AS build

WORKDIR /workspace

# 先复制依赖清单，利用 Docker 缓存。
# package-lock.json 存在时使用 npm ci，确保云端构建和本地锁定版本一致。
COPY package.json package-lock.json ./
RUN npm ci

# 复制前端源码和配置文件，然后构建生产静态资源。
COPY index.html ./
COPY public public
COPY src src
COPY vite.config.ts tsconfig.json tsconfig.app.json tsconfig.node.json ./

# 生产镜像默认通过同源 /api/v1 访问后端，由外层 Nginx 转发到 panto-api。
# 如果某个环境需要单独 API 域名，可以在 docker compose build args 中覆盖。
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# 第二阶段：运行前端静态文件。
# Nginx 只负责提供 dist 静态资源，不包含 Node.js、源码和 node_modules。
FROM nginx:stable-alpine AS runtime

# 删除 Nginx 默认页面，避免默认文件和项目 dist 混在一起。
RUN rm -rf /usr/share/nginx/html/*

# 使用项目内的 Nginx 配置，支持 React Router 刷新回退和静态资源缓存。
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 将构建阶段生成的 dist 复制到 Nginx 静态资源目录。
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80

# 使用 Nginx 前台模式运行，保持容器进程不退出。
CMD ["nginx", "-g", "daemon off;"]
