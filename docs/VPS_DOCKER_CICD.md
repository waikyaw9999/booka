# VPS Docker CI/CD guide (Exacta pattern)

Copy this guide (and `.cursor/rules/vps-docker-cicd.mdc`) into any new repo that should deploy the same way as Exacta.

## Goal

```
GitHub Actions → test → push images to Docker Hub → SSH to VPS → pull & restart
```

App code ships **inside Docker images**. The VPS does **not** need GitHub credentials for each deploy.

---

## 1. Repo layout (minimum)

```
.github/workflows/deploy.yml
docker-compose.prod.yml
.env.example
backend/Dockerfile          # or your API folder
frontend/Dockerfile         # or your UI folder
frontend/nginx.conf         # proxy /api/ → backend (SPA)
```

---

## 2. Workflow template

Save as `.github/workflows/deploy.yml`. Replace image names and test commands for your stack.

```yaml
name: CI/CD

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build-and-test-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      # Adapt: Java/Maven, Node, Go, etc.
      - uses: actions/setup-java@v3
        with:
          java-version: "17"
          distribution: "temurin"
          cache: maven
      - run: mvn -B package --file backend/pom.xml

  build-and-test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: "18.x"
      - working-directory: ./frontend
        run: |
          npm ci
          npm run build

  docker-publish:
    needs: [build-and-test-backend, build-and-test-frontend]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      - uses: docker/build-push-action@v4
        with:
          context: ./backend
          push: true
          tags: YOUR_DOCKERHUB_USER/YOUR_APP-backend:latest
      - uses: docker/build-push-action@v4
        with:
          context: ./frontend
          push: true
          tags: YOUR_DOCKERHUB_USER/YOUR_APP-frontend:latest

  deploy-vps:
    needs: docker-publish
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && vars.ENABLE_VPS_DEPLOY == 'true'
    steps:
      - name: Pull images and restart on the VPS
        uses: appleboy/ssh-action@v1.2.2
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            set -e
            cd "${{ secrets.DEPLOY_PATH }}"
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

**Do not** add `git fetch` / `git reset` on the VPS. Private HTTPS remotes fail in non-interactive SSH (`could not read Username for 'https://github.com'`).

---

## 3. `docker-compose.prod.yml` rules

- `image:` must match workflow tags exactly
- Secrets via `${VAR}` from VPS `.env` — never bake secrets into images
- Prefer healthchecks; frontend `depends_on` backend healthy
- Publish only the public port (e.g. `80:80`); keep DB internal

Example shape:

```yaml
services:
  db:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      retries: 10

  backend:
    image: YOUR_DOCKERHUB_USER/YOUR_APP-backend:latest
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      # map app env from .env
      DATABASE_URL: ...
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      retries: 10
      start_period: 45s

  frontend:
    image: YOUR_DOCKERHUB_USER/YOUR_APP-frontend:latest
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "${FRONTEND_PORT:-80}:80"

volumes:
  postgres_data:
```

---

## 4. Frontend proxy (SPA)

Nginx (or equivalent) should:

- Serve static build
- Proxy `/api/` to the Compose service name (e.g. `http://backend:8080`)
- Fall back to `index.html` for client routing

---

## 5. GitHub Secrets and Variables

Repo → **Settings → Secrets and variables → Actions**

| Type | Name | Value |
|------|------|--------|
| Secret | `DOCKER_USERNAME` | Docker Hub username |
| Secret | `DOCKER_PASSWORD` | Docker Hub access token |
| Secret | `SSH_HOST` | VPS public IP / hostname |
| Secret | `SSH_USER` | SSH login user |
| Secret | `SSH_PRIVATE_KEY` | Full private deploy key |
| Secret | `DEPLOY_PATH` | e.g. `/opt/your-app` |
| Variable | `ENABLE_VPS_DEPLOY` | `true` when ready |

Leave `ENABLE_VPS_DEPLOY` unset until images publish successfully and the VPS has Compose + `.env`.

---

## 6. One-time VPS setup

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
# log out and back in

sudo mkdir -p /opt/your-app
# copy docker-compose.prod.yml and .env to /opt/your-app (scp)
cd /opt/your-app
nano .env
docker compose -f docker-compose.prod.yml up -d
```

### Deploy SSH key (dedicated)

```bash
ssh-keygen -t ed25519 -C "github-deploy" -f github-deploy -N ""
```

- Append `github-deploy.pub` to VPS `~/.ssh/authorized_keys`
- Put private key contents into GitHub secret `SSH_PRIVATE_KEY`
- Do not reuse your laptop key

Firewall / security group: allow **22**, **80**, **443**. Do **not** open the database port.

---

## 7. Rollout order

1. Add Dockerfiles + workflow + `docker-compose.prod.yml` + `.env.example`
2. Push to `main` with Docker Hub secrets only → confirm `docker-publish`
3. Prepare VPS (Compose + `.env` + first `up -d`)
4. Add SSH secrets + `DEPLOY_PATH`
5. Set `ENABLE_VPS_DEPLOY=true`
6. Push again → confirm `deploy-vps` green
7. Hit the public URL

---

## 8. Day-2 operations

| Change | What to do |
|--------|------------|
| App code | Push `main` → new images → auto pull/restart |
| Compose / env shape | Edit files on VPS under `DEPLOY_PATH`, then `up -d` |
| Secrets | Edit VPS `.env`, then `docker compose -f docker-compose.prod.yml up -d --force-recreate SERVICE` |
| Disable deploy | Unset or set `ENABLE_VPS_DEPLOY` to something other than `true` |

---

## 9. Common failures

| Symptom | Cause | Fix |
|---------|--------|-----|
| `could not read Username for 'https://github.com'` | CI ran `git` on VPS over HTTPS | Remove git from deploy script |
| `Permission denied (publickey)` on deploy | Wrong/missing `SSH_PRIVATE_KEY` or user | Fix key / `SSH_USER` / `authorized_keys` |
| Compose pulls old behavior | Image tag mismatch or cache | Align tags; `docker compose pull` |
| Backend unhealthy forever | Bad env / DB / health path | `docker logs` on VPS; fix `.env` |
| Frontend 502 on `/api` | nginx upstream name/port wrong | Match Compose service name + port |

---

## 10. Copy into a new project

1. Copy `docs/VPS_DOCKER_CICD.md` and `.cursor/rules/vps-docker-cicd.mdc`
2. Adapt workflow tests and image names
3. Write Compose for that app’s services
4. Follow §7 rollout order

In Cursor, `@vps-docker-cicd` (or open this doc) when asking the agent to wire deploy for another app.
