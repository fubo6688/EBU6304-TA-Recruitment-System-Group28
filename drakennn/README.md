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

This repository is currently configured for **Sprint 2 (Enhanced Workflow)**.

Build and deployment mode in this branch:
1. Recommended: `backend` script flow (`start-dev.*`, `stop-dev.*`, `restart-dev.*`, `backend/build.bat`)
2. Supported as fallback: manual Tomcat drag-and-drop deployment (legacy workflow)
3. Maven build path has been removed from this branch

Enabled Sprint 2 workflow:
1. User authentication (login/logout)
2. TA profile wizard with step recovery based on profile completeness
3. TA browse open jobs and submit applications
4. TA view own application status list (`ta-applications.html`)
5. MO create/edit positions, close/reopen positions, and publish notices
6. MO review applications and process Accept/Reject decisions
7. Admin dashboard for overall positions and TA workload monitoring
8. Admin registration approval page in Admin sidebar

Partially implemented / placeholder:
1. MO notification page UI exists (`mo-notifications.html`), business logic pending
2. Legacy `admin-analytics/admin-users/admin-logs` pages are replaced by current admin pages

## Detailed Implemented Features (Current)

This section summarizes all currently implemented and testable features in this branch.

### 1. Authentication, Registration, and Security

1. Account/password login with backend session creation.
2. Role auto-detection on login page:
   - Enter account first, then login button label changes to TA Login / MO Login / Admin Login.
3. Logout support through API and frontend state cleanup.
4. TA/MO self-registration:
   - New TA/MO accounts are created with `pending` status.
   - Pending users cannot log in before admin approval.
5. Admin registration approval flow:
   - Admin can approve/reject pending TA/MO accounts.
6. Password policy enforcement for registration and password change:
   - Minimum 8 characters.
   - Must contain uppercase + lowercase + digit.
   - Letters and digits only.
7. Server-side failed login lockout:
   - Same account fails 3 times -> account is locked for 60 seconds.
   - Lock is enforced by backend (not client-only), so page refresh cannot bypass it.
8. Unified session/role guard on protected pages:
   - TA, MO, Admin pages perform session and role validation before loading content.
   - Direct URL access without valid session/role redirects to login page.
9. Legacy login compatibility:
   - `MyRecruitmentSystem/login.jsp` redirects to `login.html` in same context.

### 2. TA Features

1. TA profile wizard with step recovery:
   - Personal details, skills/available time, resume upload, confirmation.
   - Existing profile data is auto-loaded when returning.
2. Profile and account management:
   - Update personal info.
   - Change password with validation.
   - Upload/update avatar image.
3. Resume management:
   - Upload PDF resume.
   - Preview resume through backend endpoint.
4. Browse positions (`ta-positions.html`):
   - View all available positions with details.
   - Multi-condition filtering (course/major/skill/status).
   - Closed positions are not appliable.
   - Duplicate application for same position is prevented.
5. Submit applications:
   - TA can apply to open positions.
   - Application records are persisted and shown in TA list.
6. My Applications (`ta-applications.html`):
   - View own applications with status/priority/feedback.
   - Filter by status.

### 3. MO Features

1. Position management (`mo-positions.html`):
   - Create position.
   - Edit position details.
   - Open/Close/Reopen position.
   - Publish position.
2. Position publish behavior:
   - Publishing triggers TA notifications for active TA accounts.
3. Application review (`mo-review.html`):
   - View applications for MO-owned positions.
   - Accept/Reject with optional feedback.
   - Review updates TA-side status and counters.
4. Ownership boundaries:
   - MO can manage/review only positions/applications belonging to self (or own QM ID mapping).
5. MO notification page:
   - Page entry and access control are available.
   - Full notification center business workflow remains pending.

### 4. Admin Features

1. Admin dashboard (`admin-dashboard.html`):
   - Summary cards: total/open/closed positions, total/pending/approved applications.
   - All positions table with fields:
     - ID, Title, Course Name, MO, Status, Openings, Applied, Accepted.
   - Dashboard filters:
     - Filter by position status.
     - Filter by course name.
2. TA workload monitoring:
   - Per-TA total apps, pending, approved, rejected, and current load.
3. Registration approvals (`admin-approvals.html`):
   - List all pending TA/MO registrations.
   - Approve/Reject actions with immediate refresh.

### 5. Data, Notification, and Audit

1. Text-file data persistence (under workspace `data/`):
   - users, positions, applications, profiles, logs, notification files.
2. Position counters are maintained by backend logic:
   - `appliedCount` and `acceptedCount` are updated during submit/review/cancel flows.
3. Notification persistence:
   - Application submitted/review result notifications.
   - Published position notifications.
4. Operation audit logging:
   - Login/logout, profile updates, password changes, position operations, review operations, approval operations.

### 6. API and Permission Enforcement

1. API groups:
   - `/api/login`, `/api/user/*`, `/api/position/*`, `/api/application/*`, `/api/admin/*`.
2. Backend permission checks are role-aware:
   - Admin-only endpoints for admin operations.
   - MO/Admin checks for position/review management.
   - TA checks for apply flows.
3. Unauthorized/forbidden requests return proper failure responses (`401/403` style behavior with JSON message).

### 7. Runtime and Deployment Behavior

1. Scripted startup compiles backend and deploys to Tomcat webapps.
2. Current startup script deploys to both contexts:
   - `/ta-system` (recommended daily entry)
   - `/MyRecruitmentSystem` (legacy compatibility entry)
3. Runtime data directory is pinned to workspace `data/` to avoid data reset during redeploy.

## Project Structure

```text
.
|- login.html
|- register.html
|- index.html
|- profile.html
|- ta-profile.html
|- ta-positions.html
|- ta-applications.html
|- mo-positions.html
|- mo-notifications.html
|- mo-review.html
|- admin-dashboard.html
|- admin-approvals.html
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
3. Deploys project to Tomcat `webapps/ta-system` and `webapps/MyRecruitmentSystem`
4. Starts Tomcat if not already running
5. Opens the login page in browser

Note:
1. This branch keeps only the backend script build/deploy workflow.
2. Do not use Maven commands in this branch.

Default URL:

```text
http://localhost:8080/ta-system/login.html
```

Compatibility URL (legacy bookmark, now redirected to new login page):

```text
http://localhost:8080/MyRecruitmentSystem/login.jsp
```

Recommendation:
1. Prefer `ta-system` URL for daily testing.
2. If you open `MyRecruitmentSystem/login.jsp`, it will redirect to `login.html` in the same context.

## Legacy Manual Startup (Drag to Tomcat)

If you want the original manual startup workflow, use the following steps.

### Step 1: Build backend classes

From repository root:

```powershell
cd .\backend
.\build.bat
cd ..
```

### Step 2: Sync root WEB-INF

From repository root:

```powershell
.\sync-webinf.bat
```

This copies compiled classes and `web.xml` into root `WEB-INF`, which is needed for direct Tomcat webapp deployment.

### Step 3: Drag/copy project to Tomcat webapps

1. Open Tomcat `webapps` folder.
2. Create or replace one app folder:
   - `ta-system` (recommended)
   - or `MyRecruitmentSystem` (legacy URL compatibility)
3. Copy project web files into that folder:
   - `*.html`, `*.jsp`, `css/`, `js/`, `WEB-INF/`

### Step 4: Start Tomcat manually

1. Run Tomcat startup script:
   - `CATALINA_HOME\bin\startup.bat`
2. Open login page in browser:
   - `http://localhost:8080/ta-system/login.html`
   - or `http://localhost:8080/MyRecruitmentSystem/login.html`

### Optional: Keep runtime data in workspace

To avoid data writing to unexpected locations, set environment variable before starting Tomcat:

```powershell
setx TA_DATA_DIR "D:\MyRecruitmentSystem\data"
```

Then restart Tomcat.

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
5. `/api/admin/*`

Detailed Sprint 2 API endpoints:

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

Position:
1. `GET /api/position/list`
2. `POST /api/position/create`
3. `POST /api/position/update`
4. `POST /api/position/status` (open/closed, includes reopen)
5. `POST /api/position/publish` (publish notice + TA notifications)

Application:
1. `GET /api/application/review-list`
2. `GET /api/application/my-list`
3. `POST /api/application/submit`
4. `POST /api/application/review` (MO/Admin accept or reject)

Admin:
1. `GET /api/admin/dashboard`
2. `GET /api/admin/positions`
3. `GET /api/admin/ta-workload`

## Registration and Password Policy

1. TA and MO users can register on the login page.
2. New registrations are created with `pending` status.
3. Pending users cannot log in until Admin approval.
4. Admin can approve/reject from API:
   - `GET /api/user/pending-registrations`
   - `POST /api/user/approve-registration`
5. Registration no longer requires TA-only fields in register page (skills/available time).
6. Password complexity (registration and password change):
   - At least 8 characters
   - Must contain uppercase + lowercase + digit
   - Letters and digits only (no symbols)

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

## Sprint 2 Demonstration Path

Use this order during demo/viva:
1. Login as TA -> open `ta-profile.html`
2. Complete TA profile and upload resume
3. Login as MO -> open `mo-positions.html`
4. Post and publish a new position
5. Login as TA -> browse positions and apply
6. Login as TA -> open `ta-applications.html` and check status is pending
7. Login as MO -> open `mo-review.html` and Accept/Reject applicants
8. Login as Admin -> open `admin-dashboard.html` and `admin-approvals.html`

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
