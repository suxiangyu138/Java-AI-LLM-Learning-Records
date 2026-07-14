# 09 - Docker 实战项目

本章通过三个完整项目，将前面学到的知识融会贯通。

## 9.1 项目一：Python Flask + MySQL + Redis Web 应用

### 项目结构

```
flask-docker/
├── app/
│   ├── __init__.py
│   ├── models.py
│   ├── routes.py
│   └── templates/
│       └── index.html
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── .env
```

### 应用代码

**app/__init__.py**

```python
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
import redis
import os

db = SQLAlchemy()

def create_app():
    app = Flask(__name__)

    # 从环境变量读取配置（Docker 通过 -e 传入）
    app.config['SQLALCHEMY_DATABASE_URI'] = (
        f"mysql+pymysql://{os.environ.get('MYSQL_USER', 'root')}:"
        f"{os.environ.get('MYSQL_PASSWORD', '')}@"
        f"{os.environ.get('DB_HOST', 'localhost')}:3306/"
        f"{os.environ.get('MYSQL_DATABASE', 'flaskapp')}"
    )
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

    db.init_app(app)

    # Redis 连接
    app.redis = redis.Redis(
        host=os.environ.get('REDIS_HOST', 'localhost'),
        port=6379,
        decode_responses=True
    )

    from .routes import bp
    app.register_blueprint(bp)

    return app
```

**app/routes.py**

```python
from flask import Blueprint, render_template, jsonify
from .models import Item

bp = Blueprint('main', __name__)

@bp.route('/')
def index():
    visit_count = bp.app.redis.incr('visits')
    items = Item.query.all()
    return render_template('index.html', items=items, visits=visit_count)

@bp.route('/api/items')
def api_items():
    items = Item.query.all()
    return jsonify([{'id': i.id, 'name': i.name} for i in items])
```

**app/models.py**

```python
from . import db

class Item(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(80), nullable=False)

    def __repr__(self):
        return f'<Item {self.name}>'
```

**requirements.txt**

```
Flask==3.0.0
Flask-SQLAlchemy==3.1.1
PyMySQL==1.1.0
redis==5.0.1
gunicorn==21.2.0
```

### Dockerfile

```dockerfile
FROM python:3.11-slim-bookworm

WORKDIR /app

# 安装系统依赖（MySQL 客户端库等）
RUN apt update && \
    apt install -y --no-install-recommends gcc default-libmysqlclient-dev && \
    rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 创建非 root 用户
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 5000

# 使用 gunicorn 运行（生产环境）
# CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "4", "app:create_app()"]
# 或开发环境：
CMD ["flask", "run", "--host=0.0.0.0"]
```

### docker-compose.yml

```yaml
services:
  web:
    build: .
    ports:
      - "5000:5000"
    environment:
      - FLASK_APP=app
      - FLASK_ENV=development
      - DB_HOST=db
      - MYSQL_USER=flaskuser
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - MYSQL_DATABASE=flaskapp
      - REDIS_HOST=redis
    volumes:
      - ./app:/app/app     # 开发热重载
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - app-net

  db:
    image: mysql:8.0
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_USER=flaskuser
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - MYSQL_DATABASE=flaskapp
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - app-net

  redis:
    image: redis:7-alpine
    networks:
      - app-net

volumes:
  mysql-data:

networks:
  app-net:
```

### 启动

```bash
# .env 文件
echo "MYSQL_ROOT_PASSWORD=RootSecret123" > .env
echo "MYSQL_PASSWORD=UserSecret456" >> .env

# 启动
docker compose up -d --build

# 初始化数据库表
docker compose exec web flask shell -c "from app import db; db.create_all()"

# 访问 http://localhost:5000
```

---

## 9.2 项目二：Node.js 全栈应用 (React + Express + MongoDB)

### 项目结构

```
mern-docker/
├── client/              # React 前端
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── Dockerfile
├── server/              # Express 后端
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── nginx/
│   └── nginx.conf
└── docker-compose.yml
```

### Server Dockerfile（多阶段构建）

```dockerfile
# server/Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .

FROM node:20-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder --chown=app:app /app ./
USER app
EXPOSE 4000
CMD ["node", "src/index.js"]
```

### Client Dockerfile（多阶段构建）

```dockerfile
# client/Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### docker-compose.yml

```yaml
services:
  nginx:
    build:
      context: ./client
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - server
    networks:
      - mern-net

  server:
    build: ./server
    environment:
      - NODE_ENV=production
      - MONGO_URI=mongodb://mongo:27017/myapp
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      mongo:
        condition: service_healthy
    networks:
      - mern-net

  mongo:
    image: mongo:7
    volumes:
      - mongo-data:/data/db
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - mern-net

volumes:
  mongo-data:

networks:
  mern-net:
```

---

## 9.3 项目三：完整的 CI/CD 构建流水线

使用 Docker 构建、测试、推送的 CI/CD 流水线。

### GitHub Actions 配置

```yaml
# .github/workflows/docker-ci.yml
name: Docker CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ====== 测试 ======
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: testpass
          POSTGRES_DB: testdb
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.11"

      - name: Install dependencies
        run: pip install -r requirements.txt

      - name: Run tests
        run: pytest
        env:
          DATABASE_URL: postgresql://postgres:testpass@localhost:5432/testdb
          REDIS_URL: redis://localhost:6379

  # ====== 构建并推送镜像 ======
  build-and-push:
    needs: test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,format=short
            type=ref,event=branch
            type=semver,pattern={{version}}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ====== 部署 ======
  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/myapp
            docker compose pull
            docker compose up -d --remove-orphans
            docker image prune -f
```

---

## 9.4 常见应用容器化配方

### Go 静态编译（超小镜像 ~5MB）

```dockerfile
FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /server .

FROM scratch
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=builder /server /server
EXPOSE 8080
ENTRYPOINT ["/server"]
```

### Java Spring Boot

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### PostgreSQL + pgAdmin 开发环境

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: devpass
      POSTGRES_DB: devdb
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  pgadmin:
    image: dpage/pgadmin4:latest
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@dev.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"

volumes:
  pgdata:
```

### Redis + Redis Commander

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data

  redis-commander:
    image: rediscommander/redis-commander:latest
    environment:
      - REDIS_HOSTS=local:redis:6379
    ports:
      - "8081:8081"

volumes:
  redis-data:
```

---

> **下一步**：[10-Docker进阶与生态](10-Docker进阶与生态.md)
