# SonarQube Setup — Research Club Task 1

## Overview
This project sets up a local SonarQube instance using Docker Compose (SonarQube + PostgreSQL 16), verifies that it reports an Operational status, and confirms persistence across a restart.

## Environment Used
Installation was originally attempted on a Windows 11 Pro machine using Docker Desktop, but Docker Desktop could not start due to a persistent Hyper-V virtual socket error tied to Windows component store/servicing corruption (confirmed via `DISM /Online /Cleanup-Image /ScanHealth` and `/RestoreHealth`). This issue was unrelated to disk space, Core Isolation, or leftover VMware components — all of which were checked and ruled out.

Due to time constraints, the installation was completed instead in a **GitHub Codespace** (cloud-based Linux dev container), where Docker Engine is pre-installed and the Hyper-V/WSL2 issue does not apply.

## Reproduction Steps

### Prerequisites
- Docker Engine 20.10+ (pre-installed in Codespaces) or Docker Desktop
- At least 4 GB RAM and 2 CPU cores available
- ~10 GB free disk space

Verify Docker:
```bash
docker --version
docker compose version
```

### Setup
1. Clone this repository (or open it in a Codespace).
2. Navigate to the installation directory:
```bash
   cd task-1/sonarqube-installation
```
3. `compose.yaml` (SonarQube + PostgreSQL 16, port 9000) is already provided in this directory.
4. Start the containers:
```bash
   docker compose up -d
```
5. Check container status:
```bash
   docker compose ps
```
   Both `sonarqube` and `sonarqube_database` should show a running status.
6. Monitor startup logs until SonarQube reports operational:
```bash
   docker compose logs -f sonarqube
```
   Wait for the line `SonarQube is operational`, then press `Ctrl+C` (this only stops log viewing, not the containers).

### Access
Open port `9000` (in Codespaces, via the **PORTS** tab → Open in Browser; locally, via `http://localhost:9000`).

Log in with the default credentials:
- Username: `admin`
- Password: `admin`

You will be required to set a new password on first login. **The new password is stored securely and is not committed to this repository.**

### Verification
- Navigate to **Administration → System** (or `/admin/system`).
- Confirmed server status: **Operational**.
- Version and image digest recorded in [`setup/version-information.md`](./setup/version-information.md).

### Persistence Test
```bash
docker compose restart
```
After restart, confirmed SonarQube remains accessible, login still works, and status remains Operational — verifying data persistence.

## Useful Commands
| Action | Command |
|---|---|
| Stop (keep data) | `docker compose stop` |
| Start again | `docker compose start` |
| Restart | `docker compose restart` |
| View logs | `docker compose logs sonarqube` |
| Live logs | `docker compose logs -f sonarqube` |
| Shut down (keep data) | `docker compose down` |
| Shut down + delete all data | `docker compose down -v` ⚠️ destructive |

## Notes
- No passwords or access tokens are committed to this repository.
- Image digest is recorded in `setup/version-information.md` to ensure reproducibility even if the `community` tag changes later.