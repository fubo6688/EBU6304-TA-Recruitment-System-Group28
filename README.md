# TA Recruitment System (Group 28)

A lightweight web-based TA recruitment system using static HTML/CSS/JavaScript on the frontend and Java Servlet APIs on the backend.

## Group Name-list

1. Norman-Ou: 190898878 (Support TA)
2. fubo6688: 231220965 (Lead)
3. Andyfree-98: 231223014 (Member)
4. YYYNickYYY: 231222936 (Member)
5. huweize123: 231221766 (Member)
6. benlink1234: 231222626 (Member)
7. Drak3Nnnn: 231220943 (Member)

## Current Delivery Mode

This repository is currently configured for **Sprint 1 (Core MVP)**.

Build and deployment mode is now **single-path only**:
1. Only the `backend` script flow is supported (`start-dev.*`, `stop-dev.*`, `restart-dev.*`, `backend/build.bat`)
2. Maven build path has been removed from this branch

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
|- register.html
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

Note:
1. This branch keeps only the backend script build/deploy workflow.
2. Do not use Maven commands in this branch.

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

The system reads user credentials from `users.txt` only.
`users.csv` has been removed to avoid accidental misuse.

Validated from `data/users.txt` and `backend/data/users.txt`.

| Username | Password | Role |
|---|---|---|
| ta002 | Qmta2026A | TA |
| mo001 | Qmta2026A | MO |
| admin001 | Qmta2026A | Admin |
| 20210001 | Qmta2026A | TA |
| M001 | Qmta2026A | MO |
| ADM001 | Qmta2026A | Admin |
| admin_user | Qmta2026A | Admin |
| admin_02 | Qmta2026A | Admin |
| mo_smith | Qmta2026A | MO |
| mo_jones | Qmta2026A | MO |
| mo_wang | Qmta2026A | MO |
| ta_alice | Qmta2026A | TA |
| ta_bob | Qmta2026A | TA |
| ta_charlie | Qmta2026A | TA |
| ta_david | Qmta2026A | TA |
| ta_emma | Qmta2026A | TA |

Notes:
1. Legacy weak passwords were migrated in batch to `Qmta2026A`.
2. If an account still cannot log in, restart service with `./restart-dev.bat` so runtime data is reloaded.

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

Detailed Sprint 1 API endpoints:

Authentication:
1. `POST /api/login`
2. `GET /api/login?action=logout`
3. `POST /api/login` with `action=register` (TA/MO self-registration, pending admin approval)

User:
1. `GET /api/user/profile`
2. `POST /api/user/profile`
3. `POST /api/user/password`
4. `GET /api/user/ta-profile`
5. `GET /api/user/resume`
6. `GET /api/user/pending-registrations` (Admin only)
7. `POST /api/user/approve-registration` (Admin only, decision=`approve|reject`)

## Registration and Password Policy

1. TA and MO users can register on the login page.
2. New registrations are created with `pending` status.
3. Pending users cannot log in until Admin approval.
4. Admin can approve/reject from API:
   - `GET /api/user/pending-registrations`
   - `POST /api/user/approve-registration`
5. Password complexity (registration and password change):
   - At least 8 characters
   - Must contain uppercase + lowercase + digit
   - Letters and digits only (no symbols)

Position:
1. `GET /api/position/list`
2. `POST /api/position/create`

Application:
1. `GET /api/application/review-list`
2. `POST /api/application/submit`

## Backend Build and Deployment Details

Backend directory layout:

```text
backend/
|- src/com/ta/
|  |- model/
|  |- servlet/
|  |- util/
|- WEB-INF/
|  |- web.xml
|  |- classes/
|  |- lib/
|- data/
|- build.bat
|- deploy.bat
```

Build backend classes only (optional):

```powershell
cd .\backend
.\build.bat
```

Manual compile command (optional):

```powershell
javac -encoding UTF-8 -cp "WEB-INF\lib\*" -d WEB-INF\classes src\com\ta\model\*.java src\com\ta\util\*.java src\com\ta\servlet\*.java
```

Runtime data files:
1. `data/users.txt`
2. `data/positions.txt`
3. `data/applications.txt`
4. `data/profiles.txt`
5. `data/logs.txt`
6. `data/resumes/*.pdf` (TA resume files, fixed name: `userId.pdf`)

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
   - Verify dataset files exist in `data`
   - Restart via `restart-dev.bat` so `TA_DATA_DIR` is applied correctly
4. Build errors:
   - Ensure `backend/WEB-INF/lib/json-20240303.jar` exists
5. CORS/session issues:
   - Use deployed URL under Tomcat, not direct local file open (`file://`)

## License

See `LICENSE`.
