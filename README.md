# Vehicle Service Reminder System

A web application that helps vehicle owners keep track of servicing, maintenance reminders
(insurance, PUC, registration, oil, tyres, battery, brakes) and running costs for every vehicle
they own. Built with Java 17, Spring Boot 3, Thymeleaf and MySQL 8 as a final-year college project.

---

## 1. Features

| Area | What you can do |
|---|---|
| Accounts | Register, log in (with "remember me"), log out, update name/phone, change password. Passwords are stored as BCrypt hashes. |
| Dashboard | Cards for total vehicles, upcoming, overdue and total maintenance expense (₹); overdue and upcoming reminders; recent services and expenses; 6-month spend chart. |
| Vehicles | Add, view, edit, delete, search by registration / make / model. Registration number is unique per user and stored in capitals. |
| Service records | Add, edit, delete; filter by vehicle, service type and date range; search text; sort by date, cost or mileage. Can auto-create the "next service" reminder. |
| Service history | Per-vehicle page with vehicle details, a timeline of every service, total count and total cost, filter by type and dates. |
| Reminders | Due by date, by mileage, or both. Status (Upcoming / Due soon / Overdue / Completed) is calculated automatically. Mark done, reopen, edit, delete. Filter by vehicle, status and type. |
| Expenses | Fuel, insurance, repair, tyres, battery, parts, cleaning and more. Filter by vehicle, type, dates; search. |
| Reports | Total maintenance / service / other expense, expense by vehicle and by category, monthly expense, services by type, reminder counts. Charts drawn with Chart.js. |
| Security | Every page except login/register/static files needs login. CSRF protection on every form. A user can never open another user's data by changing an ID in the URL (you get a "Record not found" page). |
| UI | Dark sidebar, responsive layout: the sidebar becomes a slide-out menu on phones and tables turn into stacked cards. |

### Reminder status rules (implemented in `util/ReminderStatusUtil.java`)

| Basis | Overdue | Due soon | Upcoming |
|---|---|---|---|
| Date | due date has passed | due within 30 days | more than 30 days away |
| Mileage | current odometer ≥ due mileage | 500 km or less left | more than 500 km left |
| Both | the more urgent of the two results wins | | |

"Completed" is set by the user and is never overwritten. Statuses are refreshed every time
reminders are loaded, so they stay correct as days pass and odometer readings change.

---

## 2. Technologies

- **Backend:** Java 17, Spring Boot 3.3.5, Spring MVC, Spring Data JPA (Hibernate), Spring Security, Jakarta Bean Validation
- **Database:** MySQL 8 (MySQL Connector/J). Tests use an in-memory H2 database.
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5.3, Bootstrap Icons, a little vanilla JavaScript, Chart.js 4 (CDN)
- **Build / IDE:** Maven, IntelliJ IDEA

---

## 3. Architecture

```
Browser
  ↓   HTML pages (Thymeleaf templates + CSS + small JS helpers)
Controller layer    com.vehicleservice.controller   (URLs, forms, validation messages)
  ↓
Service layer       com.vehicleservice.service      (business rules, ownership checks, calculations)
  ↓
Repository layer    com.vehicleservice.repository   (Spring Data JPA interfaces)
  ↓
JPA / Hibernate     com.vehicleservice.model        (entities mapped to tables)
  ↓
MySQL               vehicle_service_db
```

Rules the code follows:

- Controllers never talk to repositories directly; they call services.
- Every lookup by ID also filters by the logged-in user (`findByIdAndUserId`, `findByIdAndVehicleUserId`).
  If the record belongs to someone else, a `ResourceNotFoundException` is thrown and the user sees a 404 page.
- Forms use DTO classes (`dto/*Form`) instead of binding directly to entities, so a user cannot
  inject fields such as `user_id` into a request.
- Business rules (reminder status, totals, report numbers) live in Java, never in JavaScript or HTML.

---

## 4. Folder structure

```
vehicle-service-reminder/
├── pom.xml
├── README.md
├── database/
│   └── database.sql                   ← creates the database, tables, indexes + optional demo data
├── docs/
│   └── PROJECT_REPORT.md              ← college report material (problem statement, ER, use cases...)
└── src/
    ├── main/
    │   ├── java/com/vehicleservice/
    │   │   ├── VehicleServiceApplication.java
    │   │   ├── config/SecurityConfig.java
    │   │   ├── controller/  AuthController, DashboardController, VehicleController,
    │   │   │                ServiceRecordController, ReminderController, ExpenseController,
    │   │   │                ProfileController, GlobalModelAdvice*
    │   │   ├── model/       BaseEntity*, User, Vehicle, ServiceRecord, Reminder, Expense,
    │   │   │                DisplayableEnum*, FuelType*, ServiceType*, ReminderType*,
    │   │   │                ReminderStatus*, ExpenseType*
    │   │   ├── repository/  UserRepository, VehicleRepository, ServiceRecordRepository,
    │   │   │                ReminderRepository, ExpenseRepository
    │   │   ├── service/     UserService, VehicleService, ServiceRecordService, ReminderService,
    │   │   │                ExpenseService, ReportService*
    │   │   ├── dto/         LoginRequest, RegistrationRequest, ProfileUpdateRequest*,
    │   │   │                PasswordChangeRequest*, VehicleForm*, ServiceRecordForm*,
    │   │   │                ReminderForm*, ExpenseForm*
    │   │   ├── exception/   ResourceNotFoundException, GlobalExceptionHandler
    │   │   └── util/        ReminderStatusUtil
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/   login, register, dashboard, vehicles, vehicle-form, vehicle-details*,
    │       │                service-records, service-form, reminders, reminder-form, expenses,
    │       │                expense-form, reports*, profile, error
    │       │   └── fragments/  navbar, sidebar, footer, alerts, head*, components*
    │       └── static/
    │           ├── css/     style.css, dashboard.css, responsive.css
    │           ├── js/      main.js, dashboard.js, validation.js
    │           └── images/  favicon.svg
    └── test/
        ├── java/com/vehicleservice/
        │   ├── TestData.java*
        │   ├── util/ReminderStatusUtilTest.java
        │   ├── service/ UserServiceTest, VehicleServiceTest, ServiceRecordServiceTest,
        │   │            ReminderServiceTest, ExpenseAndReportServiceTest
        │   └── security/SecurityIntegrationTest.java
        └── resources/application-test.properties*
```

`*` = files added on top of the suggested structure, and why:

| File | Reason |
|---|---|
| `model/BaseEntity` | `id` and `created_at` are shared by all 5 entities (inheritance). |
| `model/*Type`, `ReminderStatus`, `DisplayableEnum` | Fixed lists (fuel, service, reminder, expense types, statuses) as Java enums, so invalid values are impossible. |
| `dto/*Form`, `ProfileUpdateRequest`, `PasswordChangeRequest` | Safe form objects with validation annotations. |
| `service/ReportService` | Report and chart calculations kept out of controllers. |
| `controller/GlobalModelAdvice` | Puts the logged-in user into every page (for the top bar). |
| `templates/vehicle-details.html` | Vehicle details + service history page. |
| `templates/reports.html` | Reports & statistics page with charts. |
| `fragments/head.html`, `fragments/components.html` | Shared `<head>` and small reusable pieces (status badge, due date). |
| `TestData.java`, `application-test.properties` | Test helpers; tests run on H2 so MySQL is not needed to build. |

---

## 5. Database setup (MySQL)

1. Install **MySQL 8** (macOS: `brew install mysql` then `brew services start mysql`, or the installer from mysql.com; Windows: MySQL Installer). Note your `root` password.
2. Run the script. Either:
   - **MySQL Workbench:** File → Open SQL Script → `database/database.sql` → click the lightning icon, or
   - **Terminal**, from the project folder:
     ```bash
     mysql -u root -p < database/database.sql
     ```
3. This creates `vehicle_service_db`, the 5 tables with keys and indexes, and the demo account.
   Want an empty database? Run only **PART 1** of the script. The app also works if you skip the
   script completely: the JDBC URL has `createDatabaseIfNotExist=true` and Hibernate
   (`ddl-auto=update`) creates missing tables.

---

## 6. Configuration

Open `src/main/resources/application.properties`. The important lines:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vehicle_service_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

`${DB_PASSWORD:}` means "use the environment variable `DB_PASSWORD`, or empty if it is not set".
Give the app your MySQL password in **one** of these ways:

- **Recommended (IntelliJ):** Run → Edit Configurations… → select `VehicleServiceApplication` →
  *Environment variables* → add `DB_USERNAME=root;DB_PASSWORD=your_password`.
- **Terminal (macOS / Linux):** `export DB_PASSWORD=your_password` before `mvn spring-boot:run`.
- **Quick and simple:** change the line to `spring.datasource.password=${DB_PASSWORD:your_password}`.
  Do not commit a real password to Git.

---

## 7. How to run

### In IntelliJ IDEA

1. File → Open → select the project folder (the one containing `pom.xml`). Choose *Open as Project*. Wait for Maven to download dependencies (progress bar at the bottom).
2. File → Project Structure → Project → SDK: **Java 17** (or newer). IntelliJ can download one for you.
3. Set the database password (section 6).
4. Open `VehicleServiceApplication.java` and click the green ▶ next to `main`.
5. Open **http://localhost:8080** in your browser.

### From a terminal

```bash
mvn clean install          # compiles and runs all tests (uses H2, MySQL not needed)
mvn spring-boot:run        # starts the app on http://localhost:8080
```

If Maven is not installed, use IntelliJ's Maven tool window (right side → Lifecycle → `clean`, `install`)
or install it with `brew install maven`.

### Demo credentials (after running `database.sql`)

| Email | Password |
|---|---|
| demo@vehicleservice.com | demo1234 |

The demo account has 3 vehicles (car, scooter, EV), 9 service records, 8 reminders
(covering every status) and 17 expenses spread over recent months.

### Common problems

| Message | Fix |
|---|---|
| `Access denied for user 'root'@'localhost'` | Wrong/missing MySQL password; see section 6. |
| `Communications link failure` | MySQL is not running. Start it (`brew services start mysql`). |
| `Port 8080 was already in use` | Stop the other app, or set `server.port=8081`. |
| Login fails for demo user | Run `database/database.sql` again (it recreates the demo account). |

---

## 8. Pages

| URL | Page |
|---|---|
| `/login`, `/register` | Log in / create account |
| `/dashboard` | Summary cards, reminders, recent activity, spend chart |
| `/vehicles` | Vehicle cards with search |
| `/vehicles/new`, `/vehicles/{id}/edit` | Vehicle form |
| `/vehicles/{id}` | Vehicle details + service history timeline |
| `/services` | Service records table with filters and sorting |
| `/services/new`, `/services/{id}/edit` | Service record form |
| `/reminders` | Reminders with status tabs and filters |
| `/reminders/new`, `/reminders/{id}/edit` | Reminder form |
| `/expenses` | Expenses table with filters |
| `/expenses/new`, `/expenses/{id}/edit` | Expense form |
| `/reports` | Reports and charts |
| `/profile` | Profile details and password change |

All create / update / delete actions are `POST` requests with a CSRF token.

---

## 9. OOP concepts and where to find them

| Concept | Where |
|---|---|
| **Classes and objects** | Entities (`User`, `Vehicle`, `ServiceRecord`, `Reminder`, `Expense`), services, repositories and controllers are classes; Spring creates their objects (beans) and injects them through constructors. |
| **Encapsulation** | All entity and DTO fields are `private` with getters/setters. `Vehicle.getDisplayName()`, `User.getInitial()`, `Reminder.getDaysUntilDue()` hide small calculations inside the object. `ReminderStatusUtil` has a private constructor so it cannot be instantiated. |
| **Abstraction** | Repositories are interfaces (`VehicleRepository extends JpaRepository`) – we declare *what* we need and Spring Data provides the implementation. Services hide the business rules behind simple methods such as `vehicleService.getVehicle(id, userId)`. `BaseEntity` is an `abstract` class. |
| **Inheritance** | `User`, `Vehicle`, `ServiceRecord`, `Reminder` and `Expense` all `extend BaseEntity` and inherit `id`, `createdAt` and the `@PrePersist` method. `ResourceNotFoundException extends RuntimeException`. Inheritance is used only where it removes real duplication. |
| **Polymorphism** | `UserService implements UserDetailsService`: Spring Security calls `loadUserByUsername` through the interface without knowing our class. `PasswordEncoder` is an interface; `BCryptPasswordEncoder` is plugged in. All enums implement `DisplayableEnum`, so the templates call `getDisplayName()` on any of them. `GlobalExceptionHandler` handles different exception types with one mechanism. |
| **Exception handling** | Custom `ResourceNotFoundException`; `GlobalExceptionHandler` (`@ControllerAdvice`) turns exceptions into a friendly `error.html` page with the right HTTP status; `IllegalArgumentException` for wrong current password is caught in `ProfileController` and shown as a form error. |
| **Collections** | `List` everywhere for query results; `Optional` from repository lookups (`findByIdAndUserId(...).orElseThrow(...)`); `Map`/`EnumMap` in `ReminderService.countByStatus` and `ReportService` (expense by vehicle, by category, by month); `Comparator` for urgency sorting; Streams for totals (`ServiceRecordService.sumCost`). |
| **Interfaces & enums** | `DisplayableEnum`, `FuelType`, `ServiceType`, `ReminderType`, `ReminderStatus` (with fields and methods), `ExpenseType`. Java 17 `switch` expressions in `ReminderService.toReminderType` and `ServiceRecordService.toSort`, text blocks in repository queries. |

---

## 10. Database design

```
users 1 ──< vehicles 1 ──< service_records
                     1 ──< reminders
                     1 ──< expenses
```

| Table | Key columns | Notes |
|---|---|---|
| `users` | `id` PK, `email` UNIQUE | `password` holds a BCrypt hash (60 chars). |
| `vehicles` | `id` PK, `user_id` FK → users | UNIQUE (`user_id`, `registration_number`); indexes on `user_id`, `registration_number`. |
| `service_records` | `id` PK, `vehicle_id` FK → vehicles | `cost DECIMAL(10,2)`; indexes on `vehicle_id`, `service_date`. |
| `reminders` | `id` PK, `vehicle_id` FK → vehicles | `due_date` and/or `due_mileage`; `status` VARCHAR; indexes on `vehicle_id`, `due_date`. |
| `expenses` | `id` PK, `vehicle_id` FK → vehicles | `amount DECIMAL(10,2)`; indexes on `vehicle_id`, `expense_date`. |

- All foreign keys use `ON DELETE CASCADE`: records of a vehicle have no meaning without it, and a deleted account removes its data. `VehicleService.delete` also deletes children explicitly, so it works even if the tables were created by Hibernate.
- Money uses `DECIMAL`, dates use `DATE`, `created_at` uses `DATETIME(6)`, IDs use `BIGINT`, mileage uses `INT`.
- Enum values are stored as readable text (e.g. `OIL_CHANGE`) rather than numbers.
- JPA relationships are **unidirectional `@ManyToOne` with `LAZY` loading** (child → parent). There are no `@OneToMany` lists, which avoids loading huge collections and infinite JSON loops.

---

## 11. Testing

Run all tests with `mvn test` (or right-click `src/test/java` → *Run 'All Tests'* in IntelliJ).
Tests use an in-memory H2 database (`application-test.properties`) and every test is rolled back.

| Test class | Covers |
|---|---|
| `ReminderStatusUtilTest` | Date, mileage and combined status rules; completed reminders stay completed. |
| `UserServiceTest` | Registration, email normalisation, duplicate email, **BCrypt hashing**, login lookup, profile update, password change. |
| `VehicleServiceTest` | **Vehicle CRUD**, search, unique registration per user, cascade delete, **data isolation**. |
| `ServiceRecordServiceTest` | **Service record CRUD**, mileage update, auto reminder, filters, sorting, totals, isolation. |
| `ReminderServiceTest` | Reminder CRUD, automatic status, sorting, counts, complete/reopen, isolation. |
| `ExpenseAndReportServiceTest` | **Expense CRUD and calculations**: filtered totals, total/vehicle-wise/category/monthly expense. |
| `SecurityIntegrationTest` | **Login/security** with MockMvc: protected URLs, registration, login success/failure, logout, CSRF, another user changing IDs in the URL (404), and renders every page to catch template errors. |

---

## 12. Future enhancements

The layered design makes these straightforward to add:

- Email / SMS / WhatsApp reminders (a scheduled job calling `ReminderService.getUpcoming`)
- PDF service reports per vehicle
- Upload of insurance, PUC and RC documents
- Service center directory and ratings
- Shared vehicles (multiple owners / family accounts)
- REST API for a mobile app (services are already independent of the web layer)
- Cloud deployment (Docker + managed MySQL)
