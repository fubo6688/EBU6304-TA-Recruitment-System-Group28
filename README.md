# TA Recruitment System (Group 28)

Final submission for the EBU6304 Software Engineering group project: a Tomcat-based TA recruitment system with TA, MO, and Admin workflows. It uses static HTML/CSS/JavaScript, Java Servlets, and plain-text file persistence under data/ instead of a database.

## Group Name List

| No. | Member | Student ID | Role |
| --- | --- | --- | --- |
| 1 | Norman-Ou | 190898878 | Support TA |
| 2 | fubo6688 | 231220965 | Lead |
| 3 | Andyfree-98 | 231223014 | Member |
| 4 | YYYNickYYY | 231222936 | Member |
| 5 | huweize123 | 231221766 | Member |
| 6 | benlink1234 | 231222626 | Member |
| 7 | Drak3Nnnn | 231220943 | Member |

## Final Delivery Status

This repository is configured for **Final Delivery**.

Current delivery characteristics:

- Main application context: `/ta-system`
- Legacy compatibility context: `/MyRecruitmentSystem`
- Build style: script-based Java compilation and Tomcat deployment
- Persistence: workspace-level `data/` directory
- Database dependency: none
- Maven path: removed from this branch
- Recommended runtime: JDK 17+ and Tomcat 10.1+ or Tomcat 11

The final version includes authentication, TA profile completion, resume upload
and parsing, TA application submission, MO position management, MO review
workflow, Admin monitoring, notification centres, account activation controls
and deadline reminders.

## Contents

1. System overview
2. Technology stack
3. Repository structure
4. Environment requirements
5. Quick start
6. Manual deployment
7. Role workflows
8. Feature details
9. API summary
10. Data storage
11. Demo accounts
12. Testing
13. Optional AI resume analysis
14. Documentation and handout mapping
15. Troubleshooting
16. Limitations
17. License

## System Overview

The system models a university TA recruitment workflow.

```text
TA user
  -> completes profile and uploads PDF resume
  -> browses published TA positions
  -> submits applications
  -> receives review results and deadline reminders

MO user
  -> creates TA positions for modules/courses
  -> publishes positions to active TAs
  -> reviews applications for owned positions
  -> sends accept/reject decisions with feedback

Admin user
  -> monitors all positions and applications
  -> reviews TA workload
  -> activates or deactivates TA/MO accounts
```

High-level architecture:

```text
Browser pages
  HTML + CSS + JavaScript
  js/api.js for API calls
        |
        | HTTP / JSON / multipart upload
        v
Tomcat web application
  Servlet filter
  LoginServlet
  UserServlet
  PositionServlet
  ApplicationServlet
  AdminServlet
  NotificationServlet
  DeadlineReminderSchedulerListener
        |
        v
DataManager
  Plain-text files under data/
  PDF resume files under data/resumes/
  Parsed resume JSON under data/resume_parsed/
  AI matching cache under data/resume_ai_matches/
```

## Technology Stack

| Layer | Technology |
| --- | --- |
| Frontend | HTML5, CSS3, JavaScript |
| Backend | Java Servlet API using `jakarta.servlet` |
| Server | Apache Tomcat 10.1+ or Tomcat 11 |
| Runtime | JDK 17+ |
| Persistence | Plain text files and uploaded PDF files |
| JSON handling | `org.json` |
| PDF parsing | Apache PDFBox |
| Tests | Plain Java project test classes with assertion helpers |

## Repository Structure

Important files and folders:

```text
.
|- README.md                         final project documentation
|- login.html                        login page
|- register.html                     TA/MO registration page
|- forgot-password.html              password reset page
|- index.html                        role-aware home page
|- ta-profile.html                   TA profile wizard
|- ta-positions.html                 TA position browser and apply page
|- ta-applications.html              TA application status page
|- ta-notifications.html             TA notification centre
|- mo-positions.html                 MO position management page
|- mo-review.html                    MO application review page
|- mo-notifications.html             MO notification centre
|- admin-dashboard.html              Admin monitoring dashboard
|- admin-account-status.html         Admin account activation page
|- css/
|  `- style.css                      shared styling
|- js/
|  |- api.js                         frontend API wrapper
|  `- script.js                      shared frontend behaviour
|- data/                             runtime text-file data and uploads
|- backend/
|  |- src/com/ta/model/              domain model classes
|  |- src/com/ta/util/               persistence, parsing and AI utilities
|  |- src/com/ta/servlet/            Servlet controllers
|  |- test/com/ta/                   project tests
|  |- WEB-INF/                       backend deployment descriptors and libs
|  `- build.bat                      backend compile script
|- WEB-INF/                          runtime WEB-INF copied for deployment
|- docs/                             JavaDocs and supporting documents
|- start-dev.bat / start-dev.ps1     scripted local deployment
|- stop-dev.bat / stop-dev.ps1       stop local Tomcat
|- restart-dev.bat / restart-dev.ps1 restart local Tomcat deployment
|- sync-webinf.bat                   copy backend WEB-INF to root WEB-INF
`- run-project-tests.sh              macOS/Linux backend test runner
```

Backend package layout:

```text
backend/src/com/ta/
|- model/
|  `- User.java
|- util/
|  |- DataManager.java
|  |- ResumeParserClient.java
|  `- AiResumeAnalysisClient.java
`- servlet/
   |- LoginServlet.java
   |- UserServlet.java
   |- PositionServlet.java
   |- ApplicationServlet.java
   |- AdminServlet.java
   |- NotificationServlet.java
   `- DeadlineReminderSchedulerListener.java
```

## Environment Requirements

Required:

- JDK 17 or newer
- Apache Tomcat 10.1 or Tomcat 11
- Modern browser such as Chrome, Edge or Firefox

Recommended on Windows:

- Windows PowerShell 5.1+
- `CATALINA_HOME` environment variable pointing to Tomcat

Important compatibility note:

- The backend imports `jakarta.servlet.*` and uses Servlet 6 style APIs.
- Tomcat 9 uses `javax.servlet.*`, so Tomcat 9 is not compatible with this
  branch.

## Quick Start

### Windows scripted startup

From the repository root:

```powershell
.\start-dev.bat
```

The startup flow:

1. Checks Java and Tomcat.
2. Compiles backend Java sources.
3. Syncs backend `WEB-INF` into root `WEB-INF`.
4. Deploys the project to Tomcat contexts `/ta-system` and
   `/MyRecruitmentSystem`.
5. Starts Tomcat if it is not already running.
6. Opens the login page.

Default URL:

```text
http://localhost:8080/ta-system/login.html
```

Legacy compatibility URL:

```text
http://localhost:8080/MyRecruitmentSystem/login.jsp
```

### Stop and restart

```powershell
.\stop-dev.bat
.\restart-dev.bat
```

If port 8080 is stuck, use:

```powershell
.\stop-dev.ps1 -ForceKill
```

### macOS/Linux verification

The full Tomcat automation scripts are Windows-focused. On macOS/Linux, use
the project test runner to compile and verify the backend:

```bash
bash run-project-tests.sh
```

## Manual Deployment

Manual deployment is useful when the scripted deployment cannot locate Tomcat.

1. Compile backend classes:

```powershell
cd backend
.\build.bat
cd ..
```

2. Sync root runtime `WEB-INF`:

```powershell
.\sync-webinf.bat
```

3. Copy the project files to Tomcat:

```text
%CATALINA_HOME%\webapps\ta-system
```

4. Start Tomcat:

```text
%CATALINA_HOME%\bin\startup.bat
```

5. Open:

```text
http://localhost:8080/ta-system/login.html
```

Do not open pages through `file://`, because the application depends on
Tomcat sessions, cookies and `/api/*` endpoints.

## Role Workflows

### TA Workflow

1. Log in as a TA.
2. Open `ta-profile.html`.
3. Complete personal details, major, GPA, skills, available time and TA
   experience.
4. Upload a PDF resume.
5. Open `ta-positions.html`.
6. Search and filter positions by course, major, skill or status.
7. View a position detail panel.
8. Submit an application to an open position.
9. Open `ta-applications.html` to review submitted applications.
10. Open `ta-notifications.html` to check published positions, review results
    and deadline reminders.

TA-side rules:

- A TA must complete profile information before applying.
- A resume must be uploaded before application submission.
- Closed positions cannot be applied for.
- Duplicate applications for the same position are blocked.
- Current application and accepted-position limits are enforced by backend
  logic.

### MO Workflow

1. Log in as an MO.
2. Open `mo-positions.html`.
3. Create a new TA position with course, department, openings, requirements,
   description and deadline.
4. Publish the position to notify active TA users.
5. Edit position details when needed.
6. Close or reopen positions.
7. Open `mo-review.html`.
8. Review applications for MO-owned positions.
9. Accept or reject each application and optionally provide feedback.
10. Open `mo-notifications.html` to review application and system messages.

MO-side rules:

- MO users can manage only positions belonging to themselves or their mapped
  QM ID.
- A position with a passed deadline must receive a new deadline before reopen
  or publish.
- Review decisions update the TA application list and position counters.

### Admin Workflow

1. Log in as Admin.
2. Open `admin-dashboard.html`.
3. Review summary cards for positions and applications.
4. Filter all positions by status or course name.
5. Review TA workload, including total, pending, approved and rejected
   applications.
6. Open `admin-account-status.html`.
7. Deactivate or reactivate TA/MO accounts.

Admin-side rules:

- Admin-only endpoints reject TA/MO users.
- Inactive accounts cannot log in or use protected APIs.
- Admin actions are logged in `data/logs.txt`.

## Feature Details

### Authentication, Registration and Security

- Account/password login with backend session creation.
- Role auto-detection on the login page.
- Logout through backend API and frontend state cleanup.
- TA/MO self-registration.
- Registration email whitelist: `@bupt.cn` or `@qmul.ac.uk`.
- Duplicate account, duplicate email and duplicate role/QM ID checks.
- Password policy for registration and password change:
  - at least 8 characters
  - uppercase letter required
  - lowercase letter required
  - digit required
  - letters and digits only
- Server-side failed login lockout:
  - 3 failed attempts
  - 60-second lock window
  - enforced on backend, not only on the browser
- Unified protected-page session and role guards.

### TA Features

- Multi-step profile wizard.
- Step recovery based on existing profile completeness.
- Profile editing and account password change.
- Avatar upload.
- PDF resume upload and preview.
- Local resume parsing through PDFBox.
- Position browsing with filtering and matching display.
- Application submission with backend eligibility checks.
- Personal application status list.
- Notification centre with read/unread operations.

### MO Features

- Create TA positions.
- Edit position title, department, salary, description, requirements, openings
  and deadline.
- Publish positions and notify active TAs.
- Close and reopen positions.
- Review applications for owned positions.
- Accept/reject with feedback.
- View cached AI matching information when available.
- Notification centre with filters and mark-read actions.

### Admin Features

- Dashboard summary:
  - total positions
  - open positions
  - closed positions
  - total applications
  - pending applications
  - approved applications
  - rejected applications
- All-position table with course, MO, status, openings, applied count and
  accepted count.
- TA workload monitoring.
- TA/MO account activation and deactivation.
- Export-oriented admin endpoints for recruitment data.

### Notifications, Reminders and Audit Logs

- Application submitted notifications.
- Application reviewed notifications.
- Position published notifications.
- Deadline reminder notifications.
- Read and read-all operations.
- Audit log records for login, logout, profile update, password change,
  position operations, review operations, account-status updates and export
  operations.

## API Summary

The frontend resolves the base API from the current web context. For the
standard deployment:

```text
http://localhost:8080/ta-system/api
```

Primary API groups:

| API group | Purpose |
| --- | --- |
| `/api/login` | login, logout, registration, role hint, password reset |
| `/api/user/*` | profile, password, avatar, resume, resume parsing and AI result reads |
| `/api/position/*` | position list, detail, create, update, publish and status changes |
| `/api/application/*` | submit application, TA application list, MO/Admin review list and review decisions |
| `/api/notification/*` | notification list, mark one read, mark all read |
| `/api/admin/*` | dashboard, positions, TA workload and export-related reads |

Representative endpoints:

| Endpoint | Method | Main roles | Description |
| --- | --- | --- | --- |
| `/api/login` | POST | TA/MO/Admin | Login or registration depending on submitted action |
| `/api/login?action=logout` | GET | TA/MO/Admin | Destroy current session |
| `/api/login?action=role-hint` | GET | Public | Return role for an existing account |
| `/api/user/profile` | GET/POST | TA/MO/Admin | Read or update current user profile |
| `/api/user/resume` | GET | TA/MO/Admin | Preview a TA resume |
| `/api/user/parse-resume` | POST | TA/MO/Admin | Parse an uploaded PDF resume |
| `/api/position/list` | GET | TA/MO/Admin | List positions |
| `/api/position/create` | POST | MO/Admin | Create a position |
| `/api/position/update` | POST | MO/Admin | Update a position |
| `/api/position/status` | POST | MO/Admin | Close or reopen a position |
| `/api/position/publish` | POST | MO/Admin | Publish a position and notify TAs |
| `/api/application/submit` | POST | TA | Submit an application |
| `/api/application/my-list` | GET | TA/Admin | View TA applications |
| `/api/application/review-list` | GET | MO/Admin | View applications for review |
| `/api/application/review` | POST | MO/Admin | Accept or reject an application |
| `/api/admin/dashboard` | GET | Admin | Recruitment dashboard data |
| `/api/admin/ta-workload` | GET | Admin | TA workload summary |

Authorization is enforced on the backend. Unauthorized and forbidden requests
return JSON failure messages with appropriate HTTP status values where
applicable.

## Data Storage

Runtime data is stored under the repository-level `data/` directory.

| File or folder | Purpose |
| --- | --- |
| `data/users.txt` | user accounts, roles, QM IDs and active/inactive status |
| `data/profiles.txt` | TA/MO/Admin profile details |
| `data/positions.txt` | TA position records |
| `data/applications.txt` | application records and review status |
| `data/logs.txt` | audit log records |
| `data/*_notifications.txt` | per-user notification records |
| `data/resumes/` | uploaded PDF resumes |
| `data/avatars/` | uploaded avatar images |
| `data/resume_parsed/` | locally parsed resume JSON |
| `data/resume_ai/` | user-level AI resume analysis cache |
| `data/resume_ai_matches/` | position-specific AI matching cache |

Data directory resolution order:

1. JVM system property `ta.data.dir`
2. Environment variable `TA_DATA_DIR`
3. Web application path inference
4. Local workspace `data/`
5. Fallback relative `data/`

The scripted startup pins runtime data to the workspace-level `data/`
directory so data is not lost during redeployment.

## Demo Accounts

The following accounts are seeded in `data/users.txt` and are useful for
demonstration.

| Role | Account | Password | Status | Notes |
| --- | --- | --- | --- | --- |
| Admin | `admin001` | `Admin001SafeA1` | active | Main admin demo account |
| Admin | `admin_user` | `AdminUserSafeA1` | active | Additional admin account |
| MO | `mo001` | `Mo001SecureA1` | active | Main MO demo account, mapped to `M001` |
| MO | `mo_smith` | `MoSmithSafeA1` | active | Java / AI module MO |
| MO | `mo_jones` | `MoJonesSafeA1` | active | Agile / architecture module MO |
| TA | `ta002` | `Ta002SecureA1` | active | Main TA demo account |
| TA | `ta_bob` | `TaBobSafeA1` | active | Additional active TA |
| TA | `ta_charlie` | `TaCharlieSafeA1` | active | Additional active TA |
| TA | `ta_alice` | `TaAliceSafeA1` | inactive | Inactive-account demo |

If login fails, check that the account is active and that Tomcat is serving the
current workspace data directory.

## Testing

Project tests are stored under `backend/test/`.

### macOS/Linux

Run:

```bash
bash run-project-tests.sh
```

The script compiles backend sources, compiles test classes and runs:

```text
com.ta.util.DataManagerProjectTest
com.ta.servlet.CORSFilterProjectTest
com.ta.servlet.LoginServletProjectTest
com.ta.servlet.UserServletProjectTest
com.ta.servlet.PositionServletProjectTest
com.ta.servlet.ApplicationServletProjectTest
com.ta.servlet.AdminServletProjectTest
com.ta.servlet.DeadlineReminderSchedulerListenerProjectTest
```

Expected final output:

```text
All project tests passed.
```

### Windows PowerShell

Compile through the normal build flow, then run:

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

The tests use temporary `ta.data.dir` directories, so they do not require
editing the main `data/users.txt` file.

## Optional AI Resume Analysis

Resume parsing itself is local and uses PDFBox. AI resume matching is optional
and requires environment variables for an external chat-completions-compatible
service.

Required variables for AI matching:

```text
TA_AI_BASE_URL
TA_AI_API_KEY
TA_AI_MODEL
```

Optional variables:

```text
TA_AI_TIMEOUT_MS
TA_AI_MAX_OUTPUT_TOKENS
TA_AI_AUTH_HEADER
TA_AI_AUTH_SCHEME
TA_AI_FALLBACK_MODELS
```

If these variables are not configured, the core recruitment workflow still
works. The system simply reports that AI analysis is unavailable.

## Documentation and Handout Mapping

Additional documentation:

| File | Purpose |
| --- | --- |
| `docs/handout-alignment.md` | Maps common handout requirements to implemented files |
| `docs/user-manual.md` | Role-by-role demo guide |
| `backend/INTEGRATION.md` | Older integration and API notes |
| `docs/index.html` | Generated JavaDocs entry point |

Coursework requirement coverage:

| Requirement area | Where it is covered |
| --- | --- |
| Working Java backend | `backend/src/com/ta/servlet/` and `backend/src/com/ta/util/` |
| File persistence | `DataManager.java` and `data/*.txt` |
| TA functionality | `ta-profile.html`, `ta-positions.html`, `ta-applications.html` |
| MO functionality | `mo-positions.html`, `mo-review.html` |
| Admin functionality | `admin-dashboard.html`, `admin-account-status.html` |
| Authentication and authorization | `LoginServlet.java`, role guards in servlets and `js/script.js` |
| Testing | `backend/test/` and `run-project-tests.sh` |
| JavaDocs / documentation | `docs/`, `README.md`, `docs/user-manual.md` |
| Error handling | JSON failure responses in servlet endpoints |
| Demo data | `data/users.txt`, `data/positions.txt`, `data/applications.txt` |

## Troubleshooting

| Problem | Suggested fix |
| --- | --- |
| `javac` not found | Install JDK 17+ and add `JAVA_HOME/bin` to `PATH` |
| Tomcat not found | Set `CATALINA_HOME` to the Tomcat directory |
| 404 under `/ta-system` | Re-run `start-dev.bat` and confirm Tomcat deployed `webapps/ta-system` |
| Port 8080 occupied | Run `stop-dev.ps1 -ForceKill`, then restart |
| Login fails with correct-looking account | Check account status in `data/users.txt`; inactive users are blocked |
| API calls fail from local file | Open through `http://localhost:8080/ta-system/`, not `file://` |
| Build cannot find JSON classes | Check `backend/WEB-INF/lib/json-20240303.jar` exists |
| Build cannot find Servlet classes | Check `backend/WEB-INF/lib/jakarta.servlet-api-6.0.0.jar` exists |
| Resume preview fails | Confirm the PDF exists under `data/resumes/` |
| AI matching unavailable | Configure `TA_AI_*` variables or use the app without AI matching |

## Deadline Reminder Scheduler

The deadline reminder job is implemented by
`DeadlineReminderSchedulerListener`. It starts with the web application and
periodically checks upcoming position deadlines.

Configuration system properties:

```text
ta.deadline.reminder.interval.minutes
ta.deadline.reminder.initial.delay.seconds
```

Default behaviour:

- runs periodically in the background
- notifies MOs about positions they own
- notifies TAs about positions they have applied for
- avoids duplicate reminder notifications through reminder keys

## Security Notes

Implemented controls:

- session-based login
- role-based backend checks
- account active/inactive enforcement
- password policy checks
- failed-login lockout
- HTTP-only session cookie setting in `web.xml`

Known classroom-scope simplification:

- Passwords are stored in plain text in `data/users.txt`. This is acceptable
  for the local coursework demo but should be replaced by salted password
  hashing before any real deployment.


## License

See `LICENSE` for licensing details.
