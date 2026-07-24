# SonarQube Setup — Research Club Task 1

## Overview
This repo documents installing and verifying a local **SonarQube** instance using
Docker Compose, as part of Summer Research 26 onboarding task.

SonarQube is a static code analysis platform used to detect bugs, vulnerabilities,
code smells, and test coverage gaps. This task covers standing up the tool itself
(SonarQube + PostgreSQL) via Docker.

## Prerequisites
- 64-bit machine, 4GB+ RAM available to Docker, 2+ CPU cores, ~10GB free disk space
- Docker Desktop (or Docker Engine 20.10+) installed and running

## How to run it
1. Clone this repo / copy `compose.yaml` into a folder.
2. From that folder, run:
   ```bash
   docker compose up -d
   ```
3. Check both containers are up:
   ```bash
   docker compose ps
   ```
4. Watch startup logs until SonarQube reports it's operational:
   ```bash
   docker compose logs -f sonarqube
   ```
5. Open [http://localhost:9000](http://localhost:9000), log in with `admin` / `admin`,
   and set a new password when prompted.
6. Verify status under **Administration → System** — should say **Operational**.

## Verification / persistence check
```bash
docker compose restart
```
Reopen `http://localhost:9000` and confirm login + data still work after restart.

## Version info
See [`setup/version-information.md`](./setup/version-information.md) for the exact
SonarQube version, image digest, Docker version, and environment recorded for
this install.

## Useful commands
| Action | Command |
|---|---|
| Stop (keep data) | `docker compose stop` |
| Start again | `docker compose start` |
| Restart | `docker compose restart` |
| View logs | `docker compose logs sonarqube` |
| Live logs | `docker compose logs -f sonarqube` |
| Stop + remove containers (keep data) | `docker compose down` |
| Stop + wipe all data | `docker compose down -v` ⚠️ destructive |

## Notes / learnings
*(Write 3–5 sentences here once you've done it — e.g. what SonarQube is for,
anything that tripped you up, how long first startup took. This is the part
that shows genuine understanding, not just copy-pasted commands — good for
resume/interview talking points.)*

## Security note
No credentials or tokens are committed to this repository. The admin password
set during first login is stored locally only.