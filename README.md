# EnergIR-server

Energy application server.

**Repository:** <https://github.com/IvoRum/EnergIR-server>

```bash
git clone https://github.com/IvoRum/EnergIR-server.git
cd EnergIR-server/energier-server
```

## Run locally

```bash
cd energier-server
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun     # http://localhost:8080
./gradlew test
```

Credentials are not committed. Put the JDBC URL in a gitignored
`application-local.yml` (see [`deploy/README.md`](energier-server/deploy/README.md#local-development)),
or pass it as an env var:

```bash
SERVER_PORT=8081 ./gradlew bootRun
SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/energir' ./gradlew bootRun
```

## Deploy

| Path | Purpose |
|---|---|
| `energier-server/deploy/` | Server unit, env file (port/database), setup script, optional Nginx. Start at [`deploy/README.md`](energier-server/deploy/README.md). |
| `energier-server/doc/JENKINS.md` | Step-by-step Jenkins pipeline setup. |
| `energier-server/Jenkinsfile` | CI/CD pipeline: build jar → deploy to `/opt/energir` → restart service → smoke test. |
