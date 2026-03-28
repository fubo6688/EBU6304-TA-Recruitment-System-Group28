# BUPT TA Recruitment System

A web-based Teaching Assistant (TA) recruitment management system built with Jakarta Servlet and JSP. The system supports three user roles: **Module Owner (MO)**, **Teaching Assistant (TA)**, and **Administrator (ADMIN)**, each with distinct functionalities.

---

## Quick Start

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **JDK** | 21+ | Java runtime & compiler |
| **Maven** | 3.8+ | Build & dependency management |

### Run (One Command)

```bash
cd MyRecruitmentSystem
mvn clean package cargo:run
```

Open browser: **http://localhost:8080/MyRecruitmentSystem/login.jsp**

Press `Ctrl+C` in terminal to stop the server.

> The project uses an **embedded Tomcat 10.x** via the Cargo Maven plugin — no need to install Tomcat separately.

### Test Accounts

| Username | Password | Role | Name |
|----------|----------|------|------|
| `admin_user` | `admin_user` | ADMIN | System Manager |
| `admin_02` | `123` | ADMIN | Office Coordinator |
| `mo_smith` | `123` | MO | Dr. Smith (Java Module) |
| `mo_jones` | `123` | MO | Prof. Jones (Agile Module) |
| `mo_wang` | `123` | MO | Dr. Wang (Network Module) |
| `ta_alice` | `123` | TA | Alice Zhang |
| `ta_bob` | `123` | TA | Bob Li |
| `ta_charlie` | `123` | TA | Charlie Brown |
| `ta_david` | `123` | TA | David Wang |
| `ta_emma` | `123` | TA | Emma Wilson |

---

## Features

### Role-Based Functionalities

| Role | Feature | Description |
|------|---------|-------------|
| **MO** | Post Recruitment | Create TA/Invigilator job listings with required skills |
| **MO** | Review Applications | Accept or reject pending applications |
| **TA** | Browse Jobs | View all available positions |
| **TA** | Apply for Jobs | Submit applications with name and email |
| **TA** | Manage Profile | Upload CV and specify skills |
| **ADMIN** | Workload Dashboard | Monitor TA assignment counts, flag overloaded TAs (>2 jobs) |

### System Features

- **Authentication** — CSV-based login with session management
- **Authorization** — URL-level access control via `AuthFilter` (admin-only, MO-only endpoints)
- **File Upload** — CV upload support (PDF, DOC, DOCX, max 10MB)
- **Data Persistence** — All data stored in CSV files (no database required)

---

## Project Structure

```
MyRecruitmentSystem/
├── pom.xml                          # Maven config
├── users.csv                        # User credentials
├── README.md
└── src/main/
    ├── java/com/bupt/
    │   ├── controller/              # Servlets & Filter
    │   │   ├── AdminDashboardServlet.java
    │   │   ├── ApplyServlet.java
    │   │   ├── AuthFilter.java
    │   │   ├── JobListServlet.java
    │   │   ├── JobServlet.java
    │   │   ├── LoginServlet.java
    │   │   ├── ProcessApplicationServlet.java
    │   │   └── ProfileServlet.java
    │   ├── model/                   # Data models
    │   │   ├── Application.java
    │   │   └── Job.java
    │   └── utils/                   # Utility classes
    │       └── FileStorageUtil.java
    └── webapp/
        ├── WEB-INF/web.xml
        ├── login.jsp
        ├── index.jsp
        ├── postJob.jsp
        ├── jobList.jsp
        ├── applyJob.jsp
        ├── moDashboard.jsp
        ├── admin.jsp
        ├── profile.jsp
        ├── success.jsp
        └── logout.jsp
```

---

## API / URL Mappings

### Servlets

| URL Pattern | Class | Method | Description |
|-------------|-------|--------|-------------|
| `/login` | `LoginServlet` | POST | Authenticate user, create session |
| `/postJob` | `JobServlet` | POST | Create new job listing |
| `/jobs` | `JobListServlet` | GET | Retrieve and display all jobs |
| `/apply` | `ApplyServlet` | POST | Submit a job application |
| `/processApplication` | `ProcessApplicationServlet` | POST | Accept/reject application |
| `/createProfile` | `ProfileServlet` | POST | Save TA profile with CV upload |
| `/adminDashboard` | `AdminDashboardServlet` | GET | Load TA workload data |

### Filter

| URL Pattern | Class | Description |
|-------------|-------|-------------|
| `/*` | `AuthFilter` | Intercepts all requests; enforces login & role-based access |

### Pages

| Page | Access | Description |
|------|--------|-------------|
| `login.jsp` | Public | Login form |
| `index.jsp` | All authenticated | Role-based dashboard menu |
| `postJob.jsp` | MO only | Job creation form |
| `jobList.jsp` | TA | Available positions table |
| `applyJob.jsp` | TA | Job application form |
| `moDashboard.jsp` | MO | Application review table |
| `admin.jsp` | ADMIN | TA workload dashboard |
| `profile.jsp` | TA | Profile & CV upload form |
| `success.jsp` | All authenticated | Operation success confirmation |
| `logout.jsp` | All authenticated | Invalidates session, redirects to login |

---

## Data Models

### Application

| Field | Type | Description |
|-------|------|-------------|
| `applicationId` | String | Auto-generated UUID (8 chars) |
| `jobId` | String | Reference to the job |
| `applicantName` | String | Name of the applicant |
| `applicantEmail` | String | Email of the applicant |
| `status` | String | `PENDING` / `ACCEPTED` / `REJECTED` (default: `PENDING`) |

**Methods:**

```java
// Constructor
public Application(String jobId, String applicantName, String applicantEmail)

// Serialization
public String toCSV()  // → "applicationId,jobId,applicantName,applicantEmail,status"

// Getters
public String getApplicationId()
public String getJobId()
public String getApplicantName()
public String getApplicantEmail()
public String getStatus()

// Setters
public void setApplicationId(String applicationId)
public void setStatus(String status)
```

### Job

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | String | Auto-generated UUID (8 chars) |
| `moduleName` | String | Course/module name |
| `role` | String | `Teaching Assistant` or `Invigilator` |
| `requiredSkills` | String | Comma-separated skill list |

**Methods:**

```java
// Constructor
public Job(String moduleName, String role, String requiredSkills)

// Serialization
public String toCSV()  // → "jobId,moduleName,role,requiredSkills"

// Getters
public String getJobId()
public String getModuleName()
public String getRole()
public String getRequiredSkills()

// Setter
public void setJobId(String jobId)
```

---

## Utility Class — FileStorageUtil

Central data access layer for CSV file operations.

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `APP_FILE` | `applications.csv` | Stores job applications |
| `JOB_FILE` | `jobs.csv` | Stores job listings |
| `PROFILE_FILE` | `profiles.csv` | Stores TA profiles |

### Methods

```java
// Application operations
public static synchronized void saveApplication(Application app)
public static List<Application> getAllApplications()
public static synchronized void updateApplicationStatus(String appId, String newStatus)

// Job operations
public static synchronized void saveJob(Job job)
public static List<Job> getAllJobs()

// Profile operations
public static synchronized void saveProfile(String taId, String name, String skills, String cvPath)

// Admin operations
public static Map<String, Integer> getTAWorkload()  // Returns TA → accepted job count
```

---

## CSV File Formats

### users.csv (Pre-configured)

```
username,password,role,displayName
```

### applications.csv (Runtime-generated)

```
applicationId,jobId,applicantName,applicantEmail,status
```

### jobs.csv (Runtime-generated)

```
jobId,moduleName,role,requiredSkills
```

### profiles.csv (Runtime-generated)

```
taId,name,skills,cvFilePath
```

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Web Framework | Jakarta Servlet 5.0 / JSP |
| Server | Apache Tomcat 10.x (embedded via Cargo) |
| Build Tool | Apache Maven |
| Data Storage | CSV files (no database) |
| Packaging | WAR |

---

## Alternative Deployment (Manual Tomcat)

If you prefer using a standalone Tomcat installation:

```bash
mvn clean package
```

Copy `target/MyRecruitmentSystem.war` to Tomcat's `webapps/` directory, then start Tomcat.
