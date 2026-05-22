Developer API Mapping — Frontend Pages to Backend Endpoints

Purpose

This document maps main frontend pages to the backend API endpoints they call, including typical request payloads and expected response fields. Use it as a quick reference when modifying UI or backend handlers.

Notes
- API base (example): http://localhost:8080/ta-system/api
- All endpoints use JSON request/response unless noted otherwise.

Pages and API mapping

1. login.html
- POST /api/login
  - Purpose: authenticate user (login) or register (when action=register)
  - Payload (login): { "account": "<id>", "password": "<pwd>" }
  - Payload (register): { "action": "register", "role": "TA|MO", "account": "<id>", "password": "<pwd>", "email": "<email>@bupt.cn|@qmul.ac.uk" }
  - Response: { "success": true, "role": "TA|MO|Admin", "session": "..." }
- GET /api/login?action=logout
  - Purpose: logout current session

2. register.html
- POST /api/login (action=register)
  - See login register payload above

3. ta-profile.html (TA profile wizard)
- GET /api/user/ta-profile
  - Purpose: fetch existing TA profile
  - Response: profile object (personal info, skills, availability, resume meta)
- POST /api/user/profile
  - Purpose: update/save profile
  - Payload: profile object fields (name, phone, major, skills[], availability[], resumePresent boolean)
- POST /api/user/resume (file upload — multipart/form-data)
  - Purpose: upload PDF resume
  - Response: { success: true, "filename": "<userId>.pdf" }

4. ta-positions.html (browse positions)
- GET /api/position/list?filter=...
  - Purpose: list positions; supports query params for course, status, skills
  - Response: [ { id, title, course, moId, status, deadline, openings, appliedCount } ]
- POST /api/application/submit
  - Purpose: submit application for a position
  - Payload: { "positionId": "<id>", "userId": "<id>" }
  - Response: { success: true, applicationId }

5. ta-applications.html (my applications)
- GET /api/application/my-list
  - Purpose: fetch applications submitted by current TA
  - Response: [ { applicationId, positionId, status, submittedAt, feedback } ]

6. mo-positions.html (MO position management)
- GET /api/position/list?owner=<moId>
  - Purpose: fetch positions owned by MO
- POST /api/position/create
  - Payload: { title, course, description, deadline, openings, skills[] }
  - Response: { success: true, positionId }
- POST /api/position/update
  - Payload: { positionId, ...fields }
- POST /api/position/status
  - Payload: { positionId, status: "open"|"closed"|"reopen" }
- POST /api/position/publish
  - Payload: { positionId }
  - Effect: creates notifications for active TAs

7. mo-review.html (application review)
- GET /api/application/review-list?positionId=<id>
  - Purpose: fetch applications for a position
- POST /api/application/review
  - Payload: { applicationId, action: "accept"|"reject", feedback?: "..." }
  - Response: { success: true }

8. mo-notifications.html / ta-notifications.html
- GET /api/notification/list
  - Purpose: fetch notifications for current user
- POST /api/notification/read
  - Payload: { notificationId }
- POST /api/notification/read-all
  - Purpose: mark all as read

9. admin-dashboard.html
- GET /api/admin/dashboard
  - Purpose: summary counters and stats
  - Response: { totalPositions, openPositions, closedPositions, totalApplications, pendingApplications, approvedApplications }
- GET /api/admin/positions
  - Purpose: fetch all positions for admin view
- GET /api/admin/ta-workload
  - Purpose: per-TA application counts and load

10. admin-account-status.html
- GET /api/user/managed-users
  - Purpose: fetch TA and MO accounts (active/inactive)
- POST /api/user/account-status
  - Payload: { userId, status: "active"|"inactive"|"deactive"|"reactive" }
  - Response: { success: true }

General notes for developers
- Authentication: protected endpoints expect a valid session cookie; API will return 401/403 for unauthorized access.
- Error responses: { success: false, message: "..." }
- Date/time: backend uses YYYY-MM-DD or ISO timestamps for deadlines and createdAt fields.
- Pagination: `/api/position/list` and notification endpoints may accept `page` and `pageSize` parameters in future extensions.

Where to look in the codebase
- Frontend API helpers: `js/api.js` (request helper functions and endpoints)
- Page-specific logic: corresponding `*.html` and inline scripts under project root
- Backend servlets: `backend/src/com/ta/servlet/` — servlet names correspond to API groups (LoginServlet, UserServlet, PositionServlet, ApplicationServlet, AdminServlet, NotificationServlet)
- Data files: `data/` (users.txt, positions.txt, applications.txt, profiles.txt, logs.txt, resumes/)

If you want, I can:
- generate a machine-readable OpenAPI-like spec from these mappings, or
- create a one-page table that lists pages, endpoints, request examples and response examples for copy-paste into tests.

