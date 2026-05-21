# TA Recruitment System User Manual

## 1. Introduction

The TA Recruitment System is a lightweight web-based application for managing Teaching Assistant recruitment at BUPT International School. It replaces scattered forms, Excel files, and manual communication with a clearer online workflow.

The system supports three main user roles:

1. Teaching Assistant (TA)
2. Module Organiser (MO)
3. Administrator (Admin)

Each role has a separate set of functions. TA users can create profiles, upload CVs, browse positions, apply for jobs, and check application results. MO users can post jobs, manage positions, and review applicants. Admin users can monitor the whole system and manage account status.

## 2. System Access

### 2.1 Login

1. Open the system in a browser.
2. Enter the account ID and password.
3. The system detects the role of the account and shows the matching login type.
4. Click the login button to enter the system.

If the account or password is wrong, the system shows an error message. If the same account fails login several times, the system locks the account for a short time.

### 2.2 Logout

1. Click the logout option in the page header or navigation area.
2. The system clears the current session.
3. The user is returned to the login page.

### 2.3 Account Status

Only active accounts can log in and use protected functions. If an account is inactive, the user should contact an administrator to reactivate it.

## 3. TA User Guide

TA users are students who want to apply for Teaching Assistant positions.

### 3.1 Create or Update Applicant Profile

1. Log in as a TA.
2. Open the `My Profile` page from the sidebar.
3. Complete the basic information form, including major, year, GPA, email, TA experience, and internship experience.
4. Click `Next`.
5. Select skills from the skill list.
6. Select available time slots.
7. Click `Next`.
8. Upload a PDF CV or resume.
9. Preview the uploaded resume if needed.
10. Confirm the information and submit the profile.

The system saves the profile information. When the TA opens the profile page again, existing data is loaded automatically.

### 3.2 Upload CV

1. Open the `My Profile` page.
2. Go to the resume upload step.
3. Select a PDF file from the local computer.
4. Confirm the upload.
5. Use the preview option to check whether the resume can be opened correctly.

The uploaded CV is used by Module Organisers during application review.

### 3.3 Browse Available Jobs

1. Open the `Browse Positions` page.
2. View the list of available TA positions.
3. Check the title, course or department, salary, job description, required skills, openings, application count, accepted count, and status.
4. Use filters to search by course name, major, skill, or status.
5. Open a position detail window to read full information.

Closed positions cannot be applied for.

### 3.4 Apply for a Job

1. Open the `Browse Positions` page.
2. Select an open position.
3. Click `View & Apply`.
4. Read the position details carefully.
5. Click `Apply Now`.
6. Wait for the success message.

The system checks whether the TA profile is complete before allowing an application. The system also prevents duplicate applications for the same position.

### 3.5 Check Application Status

1. Open the `My Applications` page.
2. View all submitted applications.
3. Check the status of each application.

Common application statuses include:

1. `pending`: the application is waiting for review.
2. `approved`: the application has been accepted.
3. `rejected`: the application has been rejected.
4. `canceled`: the application has been canceled.

The TA can also filter applications by status. If the MO provides feedback, the feedback is shown with the application record.

### 3.6 Manage Favourite Positions

1. Open the `Browse Positions` page.
2. Click the save or favourite button on a position.
3. Open the `My Favourites` page.
4. Review saved positions.
5. Open a saved position to view details or apply if it is still available.

This function helps TAs keep track of positions they may want to apply for later.

### 3.7 View Notifications

1. Open the `Notifications` page.
2. View application updates, position updates, and deadline reminders.
3. Use type filters such as `Application`, `Position`, and `Deadline`.
4. Search notifications if needed.
5. Mark one notification as read or mark all notifications as read.

Notifications help TAs follow application progress without relying on email or manual messages.

## 4. Module Organiser User Guide

Module Organisers are staff members who create TA positions and review applicants.

### 4.1 Manage Positions

1. Log in as an MO.
2. Open the `Position Management` page.
3. View existing positions owned by the current MO.
4. Use status filters to view all, open, or closed positions.

Each position shows important information such as job title, course name, openings, applied count, accepted count, status, and deadline.

### 4.2 Create a New Position

1. Open the `Position Management` page.
2. Click the create or new position button.
3. Enter the job title.
4. Enter the course name or department.
5. Enter the salary or payment information.
6. Enter the job description.
7. Enter required skills.
8. Enter the number of openings.
9. Select a deadline.
10. Save or publish the position.

The system checks the deadline. The deadline cannot be earlier than today.

### 4.3 Edit a Position

1. Open the `Position Management` page.
2. Select a position.
3. Click the edit option.
4. Update the position information.
5. Save the changes.

The MO can update details such as description, required skills, openings, salary, and deadline.

### 4.4 Close or Reopen a Position

1. Open the `Position Management` page.
2. Select a position.
3. Click the close option to stop new applications.
4. Click the reopen option if the position should accept applications again.

If the old deadline has already passed, the MO must provide a new valid deadline before reopening the position.

### 4.5 Publish a Position

1. Open the `Position Management` page.
2. Select a position.
3. Click the publish option.
4. Confirm the action.

After publishing, active TA users receive a notification about the available position.

### 4.6 Review Applications

1. Open the `Application Review` page.
2. Select one of the MO-owned positions.
3. View the applicant list.
4. Open an applicant profile.
5. View the applicant resume.
6. Compare applicant skills, available time, and experience with the position requirements.
7. Choose `Accept` or `Reject`.
8. Add feedback if needed.
9. Submit the review decision.

After the decision is submitted, the TA application status is updated. The TA can see the result in `My Applications` and can receive a notification.

### 4.7 View MO Notifications

1. Open the `Notifications` page.
2. View new application messages, position messages, and deadline reminders.
3. Filter or search notifications.
4. Mark notifications as read when finished.

## 5. Administrator User Guide

Administrators monitor the whole system and manage account status.

### 5.1 View Admin Dashboard

1. Log in as an Admin.
2. Open the `Admin Dashboard` page.
3. View summary cards for system statistics.

The dashboard can show information such as:

1. Total positions
2. Open positions
3. Closed positions
4. Total applications
5. Pending applications
6. Approved applications
7. Rejected applications

### 5.2 Check Position Overview

1. Open the `Admin Dashboard` page.
2. View the position table.
3. Filter positions by status.
4. Search or filter by course name.

The position table helps the administrator understand current recruitment progress across modules.

### 5.3 Check TA Workload

1. Open the `Admin Dashboard` page.
2. Scroll to the TA workload area.
3. Review each TA's total applications, pending applications, approved applications, and rejected applications.

This function helps the school understand whether some TAs may have too many applications or accepted positions. It supports workload balancing and management decisions.

### 5.4 Manage Account Status

1. Open the `Account Status` page.
2. View accounts in separate Admin, TA, and MO areas.
3. Find the target user account.
4. Click `De-active` to disable an active account.
5. Click `Re-active` to enable an inactive account.

Inactive accounts cannot log in or use protected system functions. This helps improve security and account control.

## 6. Error Handling Guide

The system includes several checks to prevent invalid operations.

### 6.1 Login Errors

If the account or password is incorrect, the system shows an error message. After repeated failed login attempts, the account is locked for a short time.

### 6.2 Incomplete TA Profile

A TA cannot apply for a position before completing required profile information, including basic information, skills, available time, and resume.

### 6.3 Duplicate Applications

A TA cannot submit multiple active applications for the same position.

### 6.4 Closed Position Restriction

Closed positions cannot receive new applications.

### 6.5 Deadline Validation

When an MO creates, publishes, or reopens a position, the deadline must be valid. A deadline earlier than today is rejected.

### 6.6 Account Status Restriction

Inactive accounts cannot log in or access protected functions.


## 7. Notes on AI-Support Features

The system may support decision-making by comparing TA skills with job requirements and by showing workload information. These functions help users understand matching and workload conditions more clearly.

However, the system does not automatically make final recruitment decisions. The final selection is still made by the Module Organiser, and workload decisions are still managed by human administrators. This keeps the process transparent and fair.
