# TaskFlow — Local Task Manager

TaskFlow is a Java 17 / Spring Boot portfolio project. It runs locally on your computer and does not require a paid hosting service or cloud database.

## Features
- Kanban board: To do, In progress, and Done
- Account registration and sign-in/sign-out with BCrypt password hashing
- Email-based password reset links with hashed, single-use tokens that expire after 30 minutes
- Separate task workspaces for registered users, enforced by owner-scoped server operations
- Create, edit, search, filter, reprioritize, and delete tasks
- Custom sections such as **Job**, **College**, and **Home**; type a new section name to create it
- Due dates with overdue / due-today indicators
- Per-task checklists, shown on task cards with completed/total counts and a percentage progress bar
- File attachments up to 10 MB each, stored locally
- Drag-and-drop between board columns with saved status/order
- REST API for task CRUD operations
- H2 file database, retained across app restarts

## Requirements
- JDK 17 or newer
- Maven 3.8 or newer
- Internet access for Maven to download dependencies on the first build

Check:
```bash
java -version
mvn -version
```

## Run locally
Open a terminal in this project folder and run:
```bash
mvn clean test
mvn spring-boot:run
```
Then open **http://localhost:8080**.

### Accounts and privacy
- Open `/register` to create an account, then sign in at `/login`.
- Usernames and email addresses must be unique. Passwords are stored as BCrypt hashes.
- Each task belongs to its creator; task, checklist, REST, and attachment routes verify ownership on the server.
- On an existing local database, the first registered account is assigned any legacy tasks that do not yet have an owner. This is a non-destructive schema upgrade; back up `data/` before upgrading.
- This is a portfolio/demo implementation. Password reset email delivery requires SMTP configuration using environment variables. This is still a local portfolio/demo app; before public production deployment, add robust rate limiting, HTTPS, secure deployment secrets, monitoring, and a production database.

The server binds to `0.0.0.0` in the current WSL-friendly configuration so Windows can reach the app. Do not expose port 8080 to untrusted networks; for local-only use, restrict access with your host firewall or set `server.address=127.0.0.1` if loopback access works in your environment.

### Configure password reset email (optional)

TaskFlow uses SMTP to send a one-time reset link. For Gmail, enable 2-Step Verification and create a Google App Password; do not use your regular Gmail password. Set these environment variables in the terminal before starting TaskFlow:

```bash
export TASKFLOW_SMTP_HOST=smtp.gmail.com
export TASKFLOW_SMTP_PORT=587
export TASKFLOW_MAIL_USERNAME="your-email@gmail.com"
export TASKFLOW_MAIL_PASSWORD="your-16-character-app-password"
export TASKFLOW_MAIL_FROM="your-email@gmail.com"
export TASKFLOW_BASE_URL="http://localhost:8080"
```

Then run `mvn spring-boot:run` in that same terminal. Keep these values private and never commit them. If SMTP is not configured, the forgot-password form still shows a generic response, but no email can be delivered. The reset token is random, stored only as a SHA-256 hash, expires after 30 minutes, and is invalidated after use. Keep TaskFlow local-only; the default base URL is suitable for local development, not public deployment.

## Checklist progress
The card shows:
- completed / total items (for example, `3/5`)
- percent complete (for example, `60%`)
- a progress bar
- checklist item text, with completed items struck through

The progress is calculated from saved checklist items. Open **Edit** to add, complete, or remove items; returning to the board shows the updated progress. When marking a task complete, the app asks for confirmation if checklist items remain incomplete.

## Sections
When creating or editing a task, enter a section name such as `Job`, `College`, or `Home`. A new name becomes a section automatically. Use the flexible search field to find tasks by title, description, assignee, section, priority, or status. Existing tasks without a section are displayed as `General`.

## Local data and backups
- Task data is stored in `./data/taskflow.mv.db`.
- Uploaded files are stored in `./data/uploads/`.
- Keep both when backing up or moving the project.
- Before replacing project files or changing database schema, stop the app and make a copy of the entire `data/` folder.
- Do not commit the `data/` folder or passwords to GitHub; it is excluded by `.gitignore`.

## REST API examples
The API is protected by Spring Security and requires an authenticated session. Use the browser to sign in first; do not place account passwords in shell history or repository documentation. The API shares the same per-account task ownership checks as the web interface.

## Project structure
```text
src/main/java/com/mansi/taskflow/
  model/       Task, checklist items, attachments, user accounts, reset tokens, priority and status
  repository/  Spring Data JPA repositories
  service/     Task operations, registration, password reset
  web/         Page controller, API controller, form and security configuration
src/main/resources/
  application.properties
  templates/   index.html, edit.html, login.html, register.html, password recovery pages
  static/      CSS and JavaScript
src/test/java/
  service/     Task service tests including checklist percentage
  web/         REST API tests
```
