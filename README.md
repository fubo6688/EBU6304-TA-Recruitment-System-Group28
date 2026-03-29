# TA Recruitment System (Group 28)

## Group Name-list

1. Norman-Ou: 190898878 (Support TA)
2. fubo6688: 231220965 (Lead)
3. Andyfree-98: 231223014 (Member)
4. YYYNickYYY: 231222936 (Member)
5. huweize123: 231221766 (Member)
6. Yihua Zeng: 231222626 (Member)
7. Liyuan Tian: 231220943 (Member)

A lightweight web-based TA recruitment system using static HTML/CSS/JavaScript on the frontend and Java Servlet APIs on the backend.

## Current Delivery Mode

This repository is currently configured for **Sprint 1 (Core MVP)**.

Enabled Sprint 1 workflow:
1. User authentication (login/logout)
2. TA profile creation and resume upload
3. MO job posting
4. TA browse open jobs and submit applications
5. MO view applicants for their own jobs

Temporarily disabled in Sprint 1 mode:
1. MO approve/reject actions
2. TA application status tracking and priority updates
3. MO job status editing/closing/reopening
4. Admin analytics, user management, and logs pages
5. Notification center pages

## Project Structure

```text
.
|- login.html
|- profile.html
|- ta-profile.html
|- ta-positions.html
|- ta-applications.html
|- mo-positions.html
|- mo-review.html
|- admin-analytics.html
|- admin-users.html
|- admin-logs.html
|- notification.html
|- css/
|- js/
|  |- script.js
|  |- api.js
|- backend/
|  |- src/com/ta/
|  |- WEB-INF/
|  |- data/
|- WEB-INF/
|- start-dev.ps1 / start-dev.bat
|- stop-dev.ps1 / stop-dev.bat
|- restart-dev.ps1 / restart-dev.bat
```

## Prerequisites

1. Windows PowerShell 5.1+
2. JDK 8+ (JDK 17+ recommended)
3. Apache Tomcat 9+ (Tomcat 11 supported in this repository)
4. Environment variable `CATALINA_HOME` recommended (auto-discovery is also supported)

## Quick Start

Run from repository root:

```powershell
.\start-dev.bat
```

What this does:
1. Compiles backend Java sources
2. Syncs runtime `WEB-INF`
3. Deploys project to Tomcat `webapps/ta-system`
4. Starts Tomcat if not already running
5. Opens the login page in browser

Default URL:

```text
http://localhost:8080/ta-system/login.html
```

## Stop / Restart

Stop service:

```powershell
.\stop-dev.bat
```

Force stop (if port is occupied):

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-dev.ps1 -ForceKill
```

Restart service:

```powershell
.\restart-dev.bat
```

Restart with options (no browser + force cleanup):

```powershell
powershell -ExecutionPolicy Bypass -File .\restart-dev.ps1 -NoBrowser -ForceKill
```

## Test Accounts

Validated from `data/users.txt` and `backend/data/users.txt`.

| Username | Password | Role |
|---|---|---|
| ta002 | 123456 | TA |
| mo001 | 123456 | MO |
| admin001 | admin123 | Admin |
| 20210001 | 123456 | TA |
| M001 | 123456 | MO |
| ADM001 | admin123 | Admin |

Notes:
1. `ta001` may be marked as `inactive` in some datasets.
2. For stable testing, prefer the accounts listed above.

## Backend API Base

By default the frontend resolves API base from current URL:

```text
http://localhost:8080/ta-system/api
```

Main endpoint groups:
1. `/api/login`
2. `/api/user/*`
3. `/api/position/*`
4. `/api/application/*`
5. `/api/notification/*`
6. `/api/admin/*`

## Sprint 1 Demonstration Path

Use this order during demo/viva:
1. Login as TA -> open `ta-profile.html`
2. Complete TA profile and upload resume
3. Login as MO -> open `mo-positions.html`
4. Post a new position
5. Login as TA -> browse positions and apply
6. Login as MO -> open `mo-review.html` and verify applicants are visible

## Troubleshooting

1. Port 8080 occupied:
   - Run `stop-dev.ps1 -ForceKill` then `start-dev.bat`
2. 404 under `/ta-system`:
   - Re-run `start-dev.bat`
   - Check Tomcat `webapps/ta-system` exists
3. Login/API failures:
   - Verify Tomcat is running
   - Verify dataset files exist in `backend/data`
4. Build errors:
   - Ensure `backend/WEB-INF/lib/json-20240303.jar` exists

## License

See `LICENSE`.
