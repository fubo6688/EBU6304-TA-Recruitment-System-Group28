# Backend Deployment Guide (TA Recruitment System)

This backend is implemented with Java Servlets and file-based persistence.

## Directory Layout

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

## Requirements

1. JDK 8+ (JDK 17+ recommended)
2. Tomcat 9+ (Tomcat 11 supported)
3. Dependency jars in `backend/WEB-INF/lib/`:
   - `json-20240303.jar`
   - `servlet-api.jar` (compile-time only)

## Build Backend Classes

From repository root:

```powershell
cd .\backend
.\build.bat
```

Or compile manually:

```powershell
javac -encoding UTF-8 -cp "WEB-INF\lib\*" -d WEB-INF\classes src\com\ta\model\*.java src\com\ta\util\*.java src\com\ta\servlet\*.java
```

## Recommended Deployment (Automated)

Use the root one-command startup script:

```powershell
cd ..
.\start-dev.bat
```

This compiles backend, syncs runtime files, deploys to Tomcat, and starts service.

## Manual Deployment (Optional)

1. Build backend classes
2. Copy project to Tomcat webapps as `ta-system`
3. Ensure `WEB-INF/web.xml` and compiled classes are present
4. Start Tomcat
5. Open:

```text
http://localhost:8080/ta-system/login.html
```

## API Endpoints

Authentication:
1. `POST /api/login`
2. `GET /api/login?action=logout`

User:
1. `GET /api/user/profile`
2. `POST /api/user/profile`
3. `POST /api/user/password`
4. `GET /api/user/ta-profile`
5. `GET /api/user/resume`

Position:
1. `GET /api/position/list`
2. `POST /api/position/create`
3. `POST /api/position/status`

Application:
1. `GET /api/application/list`
2. `GET /api/application/review-list`
3. `POST /api/application/submit`
4. `POST /api/application/cancel`
5. `POST /api/application/process`
6. `POST /api/application/priority`

Notification:
1. `GET /api/notification/list`
2. `POST /api/notification/read`
3. `POST /api/notification/read-all`
4. `POST /api/notification/create`

Admin:
1. `GET /api/admin/analytics`
2. `GET /api/admin/users`
3. `GET /api/admin/logs`
4. `POST /api/admin/user-status`
5. `GET /api/admin/export`

## Data Files

Runtime data is stored in:
1. `backend/data/users.txt`
2. `backend/data/positions.txt`
3. `backend/data/applications.txt`
4. `backend/data/profiles.txt`
5. `backend/data/logs.txt`
6. `backend/data/*_notifications.txt`

## Known Mode Constraint

The current branch is configured for **Sprint 1 mode**.
Some Sprint 2 frontend actions are intentionally disabled to match Phase 1 deliverables.

## Troubleshooting

1. App fails to start:
   - Verify jar dependencies in `backend/WEB-INF/lib`
2. API 404:
   - Verify deployment path is `/ta-system`
3. CORS/session issues:
   - Use deployed URL (not `file://`)
4. Port conflicts:
   - Use `..\stop-dev.bat` or `..\stop-dev.ps1 -ForceKill`

## Version

Updated: 2026-03-29
