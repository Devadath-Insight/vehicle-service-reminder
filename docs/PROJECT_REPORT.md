# Vehicle Service Reminder System — Project Report

*Final-year / semester project documentation. Sections can be copied into the college report format.*

---

## 1. Problem Statement

Vehicle owners must remember many recurring maintenance and legal deadlines: periodic servicing,
oil changes, brake and tyre checks, battery replacement, insurance renewal, Pollution Under Control
(PUC) certificate renewal and registration renewal. These deadlines depend on **time** (a date) and
on **usage** (the odometer reading). Most owners rely on memory, paper service booklets, stickers
on the windscreen or scattered phone notes. As a result, services are missed, insurance and PUC
lapse (leading to fines and legal risk), resale value drops because there is no service history,
and owners have no idea how much a vehicle actually costs to run.

There is a need for a simple, secure, centralised system in which an owner can record every
vehicle, every service and every expense, and be told clearly what is **upcoming**, what is
**due soon** and what is **overdue**.

## 2. Objectives

1. Provide secure user registration and login with encrypted (BCrypt) passwords.
2. Allow an owner to manage multiple vehicles and their details.
3. Maintain a complete, searchable service history for each vehicle.
4. Create reminders based on date, mileage or both, with automatic status calculation.
5. Track all maintenance-related expenses by vehicle and category.
6. Present a dashboard and reports with totals and charts.
7. Guarantee that each user can see and modify only their own data.
8. Deliver a responsive interface usable on desktop, tablet and mobile.
9. Demonstrate object-oriented design and a clean layered architecture in Java.

## 3. Existing System

| Method | Drawbacks |
|---|---|
| Paper service booklet | Easily lost, no reminders, no totals, one booklet per vehicle. |
| Windscreen sticker ("next service at 45,000 km") | Covers only one item; says nothing about insurance, PUC, tyres. |
| Workshop SMS reminders | Only for that workshop's services; nothing for insurance, PUC or self-maintenance. |
| Phone calendar / notes | Date-only; cannot handle mileage-based reminders; no service history or expense analysis. |
| Spreadsheets | Manual formulas, no alerts, not mobile friendly, easy to break. |

Common limitations: information is scattered, there are no mileage-aware reminders,
no combined view of all vehicles, and no cost analysis.

## 4. Proposed System

A web application — the **Vehicle Service Reminder System** — where every owner has a private
account holding:

- a garage of vehicles with registration number, make, model, year, fuel type, odometer, colour and purchase date;
- service records with type, date, odometer, cost, service center, notes and the next due date/mileage;
- reminders for services and documents, whose status is recalculated automatically every time they are viewed;
- expenses for fuel, insurance, repairs, parts, cleaning, etc.;
- a dashboard and a reports page with totals and charts.

Benefits over the existing system: one place for everything, date **and** mileage based alerts,
automatic prioritisation (overdue first), instant totals, secure multi-user access and use from any device with a browser.

## 5. System Requirements

### 5.1 Hardware (development / server)

| Item | Minimum |
|---|---|
| Processor | Dual-core 2 GHz (Intel i3 / Apple M1 or equivalent) |
| RAM | 4 GB (8 GB recommended with IntelliJ IDEA) |
| Disk | 2 GB free |
| Client | Any device with a modern web browser |

### 5.2 Software

| Item | Version |
|---|---|
| Operating system | Windows 10/11, macOS, or Linux |
| JDK | Java 17 or newer |
| Build tool | Apache Maven 3.8+ (bundled with IntelliJ) |
| Database | MySQL 8 |
| IDE | IntelliJ IDEA (Community or Ultimate) |
| Browser | Chrome, Edge, Firefox or Safari (current versions) |

## 6. Functional Requirements

| ID | Requirement |
|---|---|
| FR1 | The system shall let a new user register with full name, email, phone and password (with confirmation). |
| FR2 | The system shall reject duplicate emails, invalid emails, invalid phone numbers and short passwords. |
| FR3 | The system shall authenticate users by email and password and offer "remember me". |
| FR4 | The system shall let a user add, view, edit, delete and search vehicles. |
| FR5 | The system shall keep registration numbers unique per user. |
| FR6 | The system shall let a user add, edit, delete, search, filter (vehicle, type, date) and sort service records. |
| FR7 | The system shall show a service history per vehicle with total number and total cost of services. |
| FR8 | The system shall let a user create reminders by date, mileage or both for services, insurance, PUC, registration and others. |
| FR9 | The system shall calculate reminder status automatically: Overdue, Due soon (≤ 30 days / ≤ 500 km), Upcoming; Completed when marked by the user. |
| FR10 | The system shall optionally create the next-service reminder when a service record is saved. |
| FR11 | The system shall let a user add, edit, delete, search and filter expenses. |
| FR12 | The system shall show dashboard statistics: total vehicles, upcoming, overdue, total maintenance expense, recent items. |
| FR13 | The system shall show reports: expense by vehicle, by category, by month, services by type. |
| FR14 | The system shall let a user update name and phone and change the password (current password required). |
| FR15 | The system shall ask for confirmation before any delete. |
| FR16 | The system shall let the user log out. |

## 7. Non-Functional Requirements

| Type | Requirement |
|---|---|
| Security | BCrypt password hashing; all pages except login/register protected; CSRF tokens on every form; session fixation protection; per-user data isolation checked on every request. |
| Usability | Consistent layout, clear labels, inline validation messages, empty-state guidance, confirmation dialogs. |
| Responsiveness | Works from 320 px phones to large desktops; off-canvas navigation on small screens. |
| Reliability | Validation on both client and server; friendly error page, no stack traces shown. |
| Performance | Indexed foreign keys and date columns; lazy loading; filtered queries run in the database. |
| Maintainability | Layered architecture, one responsibility per class, automated tests. |
| Portability | Runs anywhere Java 17 and MySQL 8 are available. |

## 8. Technology Used

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 17 | Core programming language |
| Framework | Spring Boot 3 | Auto-configuration, embedded Tomcat server |
| Web | Spring MVC | Controllers, request mapping, form binding |
| Security | Spring Security | Authentication, authorisation, CSRF, BCrypt |
| Persistence | Spring Data JPA + Hibernate | Object–relational mapping, repositories |
| Validation | Jakarta Bean Validation | `@NotBlank`, `@Email`, `@Positive`, ... |
| Views | Thymeleaf | Server-side HTML templates with reusable fragments |
| UI | HTML5, CSS3, Bootstrap 5, Bootstrap Icons | Layout and components |
| Scripts | JavaScript, Chart.js | Sidebar, confirmations, form helpers, charts |
| Database | MySQL 8 | Persistent storage |
| Build | Maven | Dependency management and build |
| Testing | JUnit 5, Spring Boot Test, MockMvc, H2 | Automated tests |

## 9. System Architecture

The system follows a **three-tier, layered MVC architecture**:

```
┌────────────────────────── Presentation tier ──────────────────────────┐
│ Browser  ⇄  Thymeleaf templates (HTML/CSS/JS)                         │
└───────────────────────────────────────────────────────────────────────┘
                 │ HTTP (GET pages, POST forms + CSRF token)
┌────────────────────────── Application tier (Spring Boot) ─────────────┐
│ Security filter chain  →  Controllers  →  Services  →  Repositories    │
│ (login, sessions, CSRF)   (web layer)     (business)    (data access)   │
│                             GlobalExceptionHandler, ReminderStatusUtil  │
└───────────────────────────────────────────────────────────────────────┘
                 │ JDBC via Hibernate
┌────────────────────────── Data tier ──────────────────────────────────┐
│ MySQL 8: users, vehicles, service_records, reminders, expenses         │
└───────────────────────────────────────────────────────────────────────┘
```

Request flow example — *open a vehicle*: the browser requests `GET /vehicles/5` → Spring Security
confirms the session → `VehicleController.details` asks `UserService` for the logged-in user →
`VehicleService.getVehicle(5, userId)` runs `findByIdAndUserId`; if the vehicle belongs to someone
else nothing is found and `ResourceNotFoundException` is thrown → `GlobalExceptionHandler` shows
the 404 page. Otherwise the controller adds the vehicle, its history and reminders to the model and
Thymeleaf renders `vehicle-details.html`.

## 10. Database Design

### users
| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK, auto increment |
| full_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| password | VARCHAR(100) | NOT NULL (BCrypt hash) |
| phone | VARCHAR(20) | |
| created_at | DATETIME(6) | NOT NULL |

### vehicles
| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users.id, ON DELETE CASCADE, indexed |
| registration_number | VARCHAR(20) | NOT NULL, indexed, UNIQUE with user_id |
| make, model | VARCHAR(50) | NOT NULL |
| year | INT | NOT NULL |
| fuel_type | VARCHAR(20) | NOT NULL |
| current_mileage | INT | NOT NULL |
| color | VARCHAR(30) | |
| purchase_date | DATE | |
| created_at | DATETIME(6) | NOT NULL |

### service_records
| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK |
| vehicle_id | BIGINT | FK → vehicles.id, ON DELETE CASCADE, indexed |
| service_type | VARCHAR(30) | NOT NULL |
| service_date | DATE | NOT NULL, indexed |
| mileage | INT | NOT NULL |
| cost | DECIMAL(10,2) | NOT NULL |
| service_center | VARCHAR(150) | |
| description | VARCHAR(1000) | |
| next_service_date | DATE | |
| next_service_mileage | INT | |
| created_at | DATETIME(6) | NOT NULL |

### reminders
| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK |
| vehicle_id | BIGINT | FK → vehicles.id, ON DELETE CASCADE, indexed |
| reminder_type | VARCHAR(30) | NOT NULL |
| title | VARCHAR(150) | NOT NULL |
| description | VARCHAR(1000) | |
| due_date | DATE | indexed |
| due_mileage | INT | |
| status | VARCHAR(20) | NOT NULL |
| created_at | DATETIME(6) | NOT NULL |

### expenses
| Column | Type | Constraint |
|---|---|---|
| id | BIGINT | PK |
| vehicle_id | BIGINT | FK → vehicles.id, ON DELETE CASCADE, indexed |
| expense_type | VARCHAR(20) | NOT NULL |
| amount | DECIMAL(10,2) | NOT NULL |
| expense_date | DATE | NOT NULL, indexed |
| description | VARCHAR(500) | |
| created_at | DATETIME(6) | NOT NULL |

The schema is in **third normal form**: every non-key column depends only on its table's primary
key; user details are stored once in `users`, vehicle details once in `vehicles`, and child tables
refer to them by foreign key.

## 11. ER Diagram Description

Entities and attributes are listed in section 10. Relationships:

- **USER owns VEHICLE** — one-to-many (1 : N). A user may own zero or more vehicles; each vehicle belongs to exactly one user (total participation of VEHICLE).
- **VEHICLE has SERVICE_RECORD** — one-to-many (1 : N). Each service record belongs to exactly one vehicle.
- **VEHICLE has REMINDER** — one-to-many (1 : N). Each reminder belongs to exactly one vehicle.
- **VEHICLE has EXPENSE** — one-to-many (1 : N). Each expense belongs to exactly one vehicle.

Service records, reminders and expenses are existence-dependent on their vehicle (deleted with it).
Reminders reach their owner through the vehicle (REMINDER → VEHICLE → USER), which is also how the
application checks ownership.

```
[USER] 1 ──── owns ──── N [VEHICLE] 1 ──── has ──── N [SERVICE_RECORD]
                               │ 1
                               ├──────── has ──── N [REMINDER]
                               │ 1
                               └──────── has ──── N [EXPENSE]
```

To draw it: rectangles for the 5 entities, ovals for attributes (underline the `id` keys),
diamonds for "owns"/"has", and write 1 and N on the connecting lines.

## 12. Use Case Description

**Actors:** Guest (not logged in), Vehicle Owner (registered user), System (automatic status calculation).

| Use case | Actor | Main flow | Alternate flows |
|---|---|---|---|
| UC1 Register | Guest | Opens /register, fills the form, submits; system validates, hashes the password, saves and redirects to login. | Invalid data or existing email → form is shown again with messages. |
| UC2 Log in | Guest | Enters email and password; system verifies the BCrypt hash and opens the dashboard. | Wrong credentials → "Invalid email or password". |
| UC3 Manage vehicles | Owner | Adds / edits / deletes / searches vehicles. | Duplicate registration → error; delete asks for confirmation. |
| UC4 Record service | Owner | Chooses vehicle, type, date, odometer, cost, next due; saves. System raises the vehicle odometer and optionally creates a reminder. | Next date before service date → error. |
| UC5 View service history | Owner | Opens a vehicle; sees timeline, total services and cost; filters by type/date. | No services → guidance to add one. |
| UC6 Manage reminders | Owner | Creates a reminder by date/mileage; marks it done or reopens it; edits or deletes. | Neither date nor mileage entered → error. |
| UC7 Calculate status | System | On every reminder listing, recalculates status from today's date and current odometer. | Completed reminders are left unchanged. |
| UC8 Track expenses | Owner | Adds, edits, deletes, filters expenses. | — |
| UC9 View dashboard / reports | Owner | Sees statistic cards, lists and charts. | New user → empty-state guidance. |
| UC10 Manage profile | Owner | Updates name/phone; changes password with current password. | Wrong current password → error. |
| UC11 Log out | Owner | Clicks Log out; session ends; redirected to login. | — |

## 13. Modules

1. **Authentication module** — `AuthController`, `UserService` (implements `UserDetailsService`), `SecurityConfig`, `login.html`, `register.html`.
2. **Dashboard module** — `DashboardController.dashboard`, `dashboard.html`, `dashboard.js`.
3. **Vehicle module** — `VehicleController`, `VehicleService`, `VehicleRepository`, `vehicles.html`, `vehicle-form.html`, `vehicle-details.html`.
4. **Service record & history module** — `ServiceRecordController`, `ServiceRecordService`, `ServiceRecordRepository`, `service-records.html`, `service-form.html`.
5. **Reminder module** — `ReminderController`, `ReminderService`, `ReminderRepository`, `ReminderStatusUtil`, `reminders.html`, `reminder-form.html`.
6. **Expense module** — `ExpenseController`, `ExpenseService`, `ExpenseRepository`, `expenses.html`, `expense-form.html`.
7. **Reports module** — `DashboardController.reports`, `ReportService`, `reports.html`.
8. **Profile module** — `ProfileController`, `profile.html`.
9. **Error handling module** — `ResourceNotFoundException`, `GlobalExceptionHandler`, `error.html`.

## 14. OOP Concepts Applied

| Concept | Implementation |
|---|---|
| Class & object | Entities, DTOs, services, repositories, controllers; objects are created and wired by Spring. |
| Encapsulation | Private fields with getters/setters; derived values such as `Reminder.getDaysUntilDue()` computed inside the object. |
| Abstraction | Repository interfaces (Spring Data generates the code); services expose simple business operations; `abstract class BaseEntity`. |
| Inheritance | All entities extend `BaseEntity` (`id`, `createdAt`); `ResourceNotFoundException` extends `RuntimeException`. |
| Polymorphism | `UserService` used through the `UserDetailsService` interface; `BCryptPasswordEncoder` used through `PasswordEncoder`; all enums implement `DisplayableEnum`. |
| Exception handling | Custom exception + `@ControllerAdvice` global handler returning a friendly page per error type. |
| Collections | `List`, `Optional`, `Map`/`EnumMap`, `Comparator`, Streams API. |

## 15. Advantages

- All vehicles, services, reminders and expenses in one secure place.
- Reminders understand both dates and kilometres; the most urgent items are shown first.
- Automatic status means no manual updating.
- Complete service history improves resale value and helps workshops diagnose problems.
- Clear cost visibility per vehicle, per category and per month.
- Works on any device with a browser; no installation for the end user.
- Secure: hashed passwords, CSRF protection and strict per-user data isolation.
- Clean layered design makes the code easy to extend and test.

## 16. Limitations

- Reminders are shown only inside the application; no email/SMS notifications yet.
- Odometer readings are entered manually (no vehicle telematics / OBD integration).
- Documents (insurance policy, PUC, RC) cannot be uploaded yet.
- A vehicle belongs to a single account; family sharing is not supported.
- Charts use a CDN, so an internet connection is needed for them (the numbers still appear in tables).
- Single currency (₹) and English-only interface.

## 17. Future Scope

- Scheduled email, SMS and WhatsApp reminders.
- Downloadable PDF service history for resale or insurance claims.
- Upload and expiry tracking for insurance, PUC and RC documents.
- Service center directory, booking and ratings.
- Shared vehicles for families and fleets, with roles.
- REST API and a companion mobile application.
- Fuel efficiency (km/l, km/kWh) analytics.
- Cloud deployment with Docker and a managed MySQL database.

## 18. Conclusion

The Vehicle Service Reminder System replaces scattered paper records and memory with a single,
secure, responsive web application. It records vehicles, services, reminders and expenses;
calculates reminder urgency automatically from both dates and odometer readings; and summarises
running costs through a dashboard and reports. Built with Java 17, Spring Boot, Spring Security,
Spring Data JPA, Thymeleaf and MySQL using a layered architecture, the project demonstrates core
object-oriented principles, secure web development practices, relational database design and
automated testing, and provides a solid foundation for the future enhancements listed above.
