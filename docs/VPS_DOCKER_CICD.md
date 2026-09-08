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
Caddyfile                   # HTTPS reverse proxy → frontend:80
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
- With Caddy HTTPS: publish **only** Caddy `80`/`443`; keep frontend and DB internal (no host port on frontend)
- Without Caddy (HTTP-only labs): you may publish frontend `80:80` instead — not recommended for a public domain

Example shape (Booka with Caddy):

```yaml
services:
  # postgres + backend as usual (no host ports)

  frontend:
    image: ${DOCKERHUB_USERNAME}/booka-frontend:latest
    # no ports: — internal only

  caddy:
    image: caddy:2-alpine
    ports:
      - "80:80"
      - "443:443"
      - "443:443/udp"
    environment:
      DOMAIN: ${DOMAIN:-booka.juneko.online}
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
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
# copy docker-compose.prod.yml, Caddyfile, and .env to /opt/your-app (scp)
cd /opt/your-app
nano .env
# For Booka HTTPS: set DOMAIN=booka.juneko.online (optional default) — see §7
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

## 7. HTTPS with Caddy (Booka: `booka.juneko.online`)

Caddy terminates TLS (Let's Encrypt automatic) and reverse-proxies to the internal `frontend:80` service. The SPA nginx still proxies `/api/` → `backend:8080` on the Compose network.

### Prerequisites

1. **DNS A record** — point `booka.juneko.online` at your VPS public IP. Wait until it resolves (`dig` / `nslookup`) before bringing Caddy up, or cert issuance will fail.
2. **Firewall** — allow inbound **80** and **443** (and **22** for SSH). Let's Encrypt HTTP-01 needs port 80 reachable from the internet.
3. **DOMAIN** — in VPS `.env` set `DOMAIN=booka.juneko.online` (optional; Compose/Caddyfile default to that host). Keep `DOCKERHUB_USERNAME=...` as before.
4. **Stripe URLs** (when enabling Checkout later) — use `https://booka.juneko.online/...` for `STRIPE_SUCCESS_URL` / `STRIPE_CANCEL_URL`.

### One-time: copy files and start

From your laptop (replace user/host/`DEPLOY_PATH`):

```bash
# Copy updated Compose + Caddyfile (+ .env if you changed it)
scp docker-compose.prod.yml Caddyfile user@YOUR_VPS_IP:/opt/booka/
# if .env is new or updated:
# scp .env user@YOUR_VPS_IP:/opt/booka/.env

ssh user@YOUR_VPS_IP
cd /opt/booka   # or your DEPLOY_PATH

# Ensure DOMAIN is set (or rely on default booka.juneko.online)
grep -E '^(DOCKERHUB_USERNAME|DOMAIN)=' .env || true
# DOCKERHUB_USERNAME=your-dockerhub-user
# DOMAIN=booka.juneko.online

# Stop any old stack that published host port 80 on frontend (avoids conflict)
docker compose -f docker-compose.prod.yml down

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

### Verify

```bash
docker compose -f docker-compose.prod.yml ps
curl -I https://booka.juneko.online
# Browser: https://booka.juneko.online — padlock / valid cert
```

CI deploys still only `pull` + `up -d`. After changing Compose or `Caddyfile` in git, **scp those files to the VPS** once (or whenever they change); image-only deploys do not update them.

### Troubleshooting

| Symptom | Cause | Fix |
|---------|--------|-----|
| Chrome “Not secure” / HTTP-only | Hitting `http://` or cert never issued | Use `https://booka.juneko.online`; check Caddy logs for ACME errors |
| Cert obtain fail / ACME error | DNS not pointing here, port 80 blocked, or rate limit | Fix A record; open 80/443; `docker logs` on the `caddy` service; retry after DNS propagates |
| `address already in use` on 80/443 | Another process or old frontend published `80:80` | `docker compose ... down`; stop host nginx/apache; ensure frontend has **no** `ports:` in prod Compose |
| Site 502 / blank | Frontend/backend not healthy | `docker compose ps` + `docker logs` on `frontend` / `backend` / `caddy` |
| Still serving HTTP on bare IP | Expected — use the domain name so Caddy can match the site block and serve TLS | |

Useful logs:

```bash
docker compose -f docker-compose.prod.yml logs -f caddy
```

---

## 8. Rollout order

1. Add Dockerfiles + workflow + `docker-compose.prod.yml` + `Caddyfile` + `.env.example`
2. Push to `main` with Docker Hub secrets only → confirm `docker-publish`
3. Prepare VPS (Compose + Caddyfile + `.env` + DNS + firewall + first `up -d`)
4. Add SSH secrets + `DEPLOY_PATH`
5. Set `ENABLE_VPS_DEPLOY=true`
6. Push again → confirm `deploy-vps` green
7. Hit **https://booka.juneko.online**

---

## 9. Day-2 operations

| Change | What to do |
|--------|------------|
| App code | Push `main` → new images → auto pull/restart |
| Compose / Caddyfile / env shape | `scp` updated files to VPS under `DEPLOY_PATH`, then `up -d` |
| Secrets | Edit VPS `.env`, then `docker compose -f docker-compose.prod.yml up -d --force-recreate SERVICE` |
| Disable deploy | Unset or set `ENABLE_VPS_DEPLOY` to something other than `true` |

---

## 10. Common failures

| Symptom | Cause | Fix |
|---------|--------|-----|
| `could not read Username for 'https://github.com'` | CI ran `git` on VPS over HTTPS | Remove git from deploy script |
| `Permission denied (publickey)` on deploy | Wrong/missing `SSH_PRIVATE_KEY` or user | Fix key / `SSH_USER` / `authorized_keys` |
| Compose pulls old behavior | Image tag mismatch or cache | Align tags; `docker compose pull` |
| Backend unhealthy forever | Bad env / DB / health path | `docker logs` on VPS; fix `.env` |
| Frontend 502 on `/api` | nginx upstream name/port wrong | Match Compose service name + port |
| HTTPS / cert issues | See §7 troubleshooting | DNS, firewall 80/443, no port conflict with Caddy |

---

## 11. Copy into a new project

1. Copy `docs/VPS_DOCKER_CICD.md` and `.cursor/rules/vps-docker-cicd.mdc`
2. Adapt workflow tests and image names
3. Write Compose for that app’s services (include Caddy + `Caddyfile` for public HTTPS)
4. Follow §8 rollout order

In Cursor, `@vps-docker-cicd` (or open this doc) when asking the agent to wire deploy for another app.
