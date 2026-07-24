# SonarQube Installation — Version Information

Fill this in after you complete Step 6 (Verify the installation) of the setup guide.

| Field | Value |
|---|---|
| SonarQube version | *(from Administration → System)* |
| SonarQube edition | *(e.g. Community)* |
| Docker version | *(output of `docker --version`)* |
| Docker Compose version | *(output of `docker compose version`)* |
| PostgreSQL image version | `postgres:16` |
| Operating system | *(e.g. Windows 11 / macOS Sonoma / Ubuntu 24.04)* |
| Installation date | *(today's date)* |
| SonarQube image digest | *(output of the `docker image inspect` command below)* |

## How to get the image digest

**Linux / macOS:**
```bash
docker image inspect sonarqube:community --format='{{index .RepoDigests 0}}'
```

**Windows PowerShell:**
```powershell
docker image inspect sonarqube:community --format='{{index .RepoDigests 0}}'
```

Paste the exact output above. This digest pins the exact image you used, even if the `community` tag later points to a newer version — important for reproducibility across the team.

## Acceptance checklist

Copy this into your submission and tick off as you go:

- [ ] `http://localhost:9000` opens successfully
- [ ] Administrator can log in
- [ ] Default admin password has been changed
- [ ] Both Docker containers (`sonarqube`, `sonarqube_database`) are running
- [ ] SonarQube reports an **Operational** status
- [ ] SonarQube version recorded above
- [ ] SonarQube image digest recorded above
- [ ] SonarQube restarts (`docker compose restart`) without losing data
- [ ] Another team member could reproduce this using only the compose.yaml + these notes
- [ ] No passwords or tokens committed to Git