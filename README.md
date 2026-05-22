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
8. Admin account status page (separate TA/MO areas) for de-active/re-active operations
9. TA and MO notification center pages
10. Independent deadline reminder scheduler (backend timed task)

Partially implemented / placeholder:
1. Legacy `admin-analytics/admin-users/admin-logs` pages are replaced by current admin pages

## Detailed Implemented Features (Current)

This section summarizes all currently implemented and testable features in this branch.

### 1. Authentication, Registration, and Security

1. Account/password login with backend session creation.
2. Role auto-detection on login page:
   - Enter account first, then login button label changes to TA Login / MO Login / Admin Login.
3. Logout support through API and frontend state cleanup.
4. TA/MO self-registration:
   - Registration email must end with `@bupt.cn` or `@qmul.ac.uk`.
   - New TA/MO accounts are activated immediately after successful registration.
5. Account status semantics:
   - `active`: account is enabled and can log in/use protected APIs.
   - `inactive`: account is disabled and login/protected APIs are blocked.
6. Registration uniqueness guard:
   - Backend rejects registration when another account already has the same `role + qmId`.
7. Password policy enforcement for registration and password change:
   - Minimum 8 characters.
   - Must contain uppercase + lowercase + digit.
   - Letters and digits only.
8. Server-side failed login lockout:
   - Same account fails 3 times -> account is locked for 60 seconds.
   - Lock is enforced by backend (not client-only), so page refresh cannot bypass it.
9. Unified session/role guard on protected pages:
   - TA, MO, Admin pages perform session and role validation before loading content.
   - Direct URL access without valid session/role redirects to login page.
10. Legacy login compatibility:
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
   - TA must complete profile (basic info, skills/availability, resume) before applying.
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
   - Posting deadline cannot be earlier than today.
   - If a position deadline has passed, MO must set a new deadline before reopen/publish.
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
   - Full notification center is available (filter/search/mark-read/mark-all-read).

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
3. TA/MO account status management (`admin-account-status.html`):
   - Admin can de-active or re-active active/inactive TA and MO accounts.
   - TA and MO accounts are displayed in two separate sections.
   - Inactive accounts are blocked from protected API access and new login sessions.

### 5. Data, Notification, and Audit

1. Text-file data persistence (under workspace `data/`):
   - users, positions, applications, profiles, logs, notification files.
2. Position counters are maintained by backend logic:
   - `appliedCount` and `acceptedCount` are updated during submit/review/cancel flows.
3. Notification persistence:
   - Application submitted/review result notifications.
   - Published position notifications.
   - Deadline reminder notifications by independent timed scheduler.
4. Operation audit logging:
   - Login/logout, profile updates, password changes, position operations, review operations, account-status operations.

### 6. API and Permission Enforcement

1. API groups:
   - `/api/login`, `/api/user/*`, `/api/position/*`, `/api/application/*`, `/api/admin/*`, `/api/notification/*`.
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
# TA Recruitment System (Group 28)

Comprehensive TA recruitment web application (Sprint 2). Frontend uses static HTML/CSS/JavaScript; backend is implemented with Java Servlet APIs and simple file-based persistence for demo and evaluation purposes.

## Overview

This project provides a lightweight system for publishing TA positions, submitting and reviewing applications, and administering TA/MO accounts. It is designed for classroom demonstrations and local testing with scripted build/deploy helpers for Tomcat.

Key capabilities:
- Role-aware authentication (TA, MO, Admin) with session enforcement
- TA profile wizard, resume upload, and application flows
- MO position lifecycle: create, publish, open/close, review applications
- Admin dashboard and account status management (activate/deactivate)
- File-backed persistence under `data/` and notification logging

## Contents

1. Project layout
2. Features
3. Prerequisites
4. Quick start (scripted)
5. Manual deployment (legacy)
6. API summary
7. Data files
8. Test accounts
9. Running tests
10. Troubleshooting
11. License

## Project layout

Important files and folders:

```
README.md
*.html, *.jsp
css/
js/ (script.js, api.js)
data/ (runtime data files)
backend/ (backend source, build scripts and WEB-INF)
WEB-INF/ (runtime WEB-INF for deployment)
start-dev.bat, stop-dev.bat, restart-dev.bat, sync-webinf.bat
```

Backend short layout:

```
backend/
  ├─ src/com/ta/         (Java servlet sources)
  ├─ WEB-INF/
  ├─ data/
  ├─ build.bat
  └─ deploy.bat
```

## Features

- Authentication, registration and role detection
- Password policy and server-side lockout on repeated failed logins
- TA profile wizard with step recovery and resume handling
- Position management for MOs (create/edit/publish/open/close)
- Application submission by TA, review by MO/Admin with feedback
- Notification persistence and deadline reminder scheduler
- Admin dashboard and TA/MO account status management

## Prerequisites

- Windows PowerShell 5.1+
- JDK 8+ (JDK 17+ recommended)
- Apache Tomcat 9+ (Tomcat 11 supported)
- Optional: `CATALINA_HOME` environment variable

## Quick start (scripted)

From the repository root:

```powershell
.\start-dev.bat
```

This will:
- Compile backend Java sources
- Sync runtime `WEB-INF`
- Deploy to Tomcat contexts `ta-system` and `MyRecruitmentSystem`
- Start Tomcat if not already running and open the login page

Default URL:

http://localhost:8080/ta-system/login.html

Compatibility (legacy):

http://localhost:8080/MyRecruitmentSystem/login.jsp

Notes: use the scripted workflow in this branch (Maven build path removed).

## Manual deployment (legacy)

1. Build backend classes:

```powershell
cd backend
.\build.bat
cd ..
```

2. Sync WEB-INF:

```powershell
.\sync-webinf.bat
```

3. Copy project files into a Tomcat app folder `webapps/ta-system` (or `MyRecruitmentSystem`).

4. Start Tomcat:

```
%CATALINA_HOME%\bin\startup.bat
```

## API summary

Frontend resolves API base from current URL (example): `http://localhost:8080/ta-system/api`.

Primary endpoints:
- `/api/login` (POST login/register, GET logout)
- `/api/user/*` (profile, password, resume, admin user list)
- `/api/position/*` (list/create/update/status/publish)
- `/api/application/*` (submit/my-list/review)
- `/api/notification/*` (list/read/read-all)
- `/api/admin/*` (dashboard, positions, ta-workload)

See `js/api.js` and frontend pages for details on request payloads and expected responses.

## Data files

Data persisted under `data/` (text files):

- `users.txt`, `positions.txt`, `applications.txt`, `profiles.txt`, `logs.txt`
- `data/resumes/` — uploaded PDF resumes (named `<userId>.pdf`)

To pin runtime data to a particular location, set `TA_DATA_DIR` environment variable before starting Tomcat.

## Test accounts

Latest project test accounts:

- `ta002` / `Qmta2026A` (TA) - profile, password change, application submission, login success/lockout checks
- `ta_bob` / `Qmta2026A` (TA) - login lockout, publish notifications, admin workload coverage
- `mo001` / `Qmta2026A` (MO) - position create/update/status/publish, application review, TA profile viewing
- `admin001` / `Qmta2026A` (Admin) - pending registrations, dashboard, positions, TA workload

The tests create their own temporary `ta.data.dir` values, so you do not need to edit `data/users.txt` before running them. If login problems occur in the app itself, try `restart-dev.bat` to reload runtime data.

## Running tests

Project tests are under `backend/test/`. After compiling the backend and test sources, run all current project test classes:

```powershell
$tests = @(
   'com.ta.util.DataManagerProjectTest',
   'com.ta.servlet.CORSFilterProjectTest',
   'com.ta.servlet.LoginServletProjectTest',
   'com.ta.servlet.UserServletProjectTest',
   'com.ta.servlet.PositionServletProjectTest',
   'com.ta.servlet.ApplicationServletProjectTest',
   'com.ta.servlet.AdminServletProjectTest',
   'com.ta.servlet.DeadlineReminderSchedulerListenerProjectTest'
)

foreach ($t in $tests) {
  java -cp 'backend\test-bin;backend\WEB-INF\classes;backend\WEB-INF\lib\*' $t
}
```

Suggested flow:

1. Compile the backend classes and copy the test class files into `backend/test-bin`.
2. Run `DataManagerProjectTest` and `CORSFilterProjectTest` first to verify the base utility and filter behavior.
3. Run the servlet tests in the order above to cover login, profile, position, application, and admin flows.
4. Finish with `DeadlineReminderSchedulerListenerProjectTest` to verify scheduler startup and shutdown behavior.

## Troubleshooting

- Port 8080 occupied: run `stop-dev.ps1 -ForceKill` then `start-dev.bat`.
- 404 under `/ta-system`: re-run `start-dev.bat` and verify `webapps/ta-system` exists.
- Login/API failures: ensure Tomcat is running and `data/` files exist; use `restart-dev.bat`.
- Build errors: verify required jars in `backend/WEB-INF/lib` (e.g. `json-*.jar`).
- CORS/session issues: access pages via Tomcat URL, do not open HTML directly with `file://`.

## Deadline reminder scheduler

Implemented as `DeadlineReminderSchedulerListener`. Defaults to 60-minute scans; tuning via system properties:

- `ta.deadline.reminder.interval.minutes`
- `ta.deadline.reminder.initial.delay.seconds`

## Security notes

- Password policy: min 8 chars, uppercase+lowercase+digit, letters and digits only.
- Failed login lockout: 3 failed attempts -> 60s lock enforced server-side.

## Contributing

- Use provided scripts for development and demos. Keep runtime `data/` inside the workspace to preserve test data between redeploys.

If you'd like, I can add an additional developer doc mapping frontend pages to API calls.

## License

See the `LICENSE` file for licensing details.
