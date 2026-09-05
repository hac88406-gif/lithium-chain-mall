# ==========================================================================
# 前端 Vue 3 Dockerfile
# 多阶段构建：第一阶段 node 编译前端，第二阶段 nginx 托管静态文件
# ==========================================================================

# ---- Stage 1: Node 编译 ----
FROM node:18-alpine AS builder

WORKDIR /build

# 先复制 package.json 让依赖缓存（lock 文件变了才重装）
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com

# 复制源码并打包（生产模式会做 tree-shaking + 压缩）
COPY . .
RUN npm run build

# ---- Stage 2: Nginx 托管静态文件 ----
FROM nginx:alpine

# 覆盖默认 nginx.conf，加后端反代和 SPA fallback
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从 builder 阶段复制打包好的 dist 静态文件
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
