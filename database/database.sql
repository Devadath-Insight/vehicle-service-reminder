-- =====================================================================
--  Vehicle Service Reminder System - MySQL 8 database script
--
--  How to run (pick one):
--    * MySQL Workbench: File > Open SQL Script > this file > Execute (lightning icon)
--    * Terminal:        mysql -u root -p < database/database.sql
--
--  The script is safe to run more than once:
--    * tables are created only if they do not exist
--    * the demo user (and, through ON DELETE CASCADE, all demo records)
--      is deleted and inserted again
--
--  Demo login:  demo@vehicleservice.com  /  demo1234
--  (the password is stored as a BCrypt hash - never as plain text)
--
--  Want an empty database without demo data? Run only PART 1.
-- =====================================================================

-- =====================================================================
--  PART 1: database, tables, keys and indexes
--  Column names, types and lengths match the JPA entities in
--  src/main/java/com/vehicleservice/model exactly.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vehicle_service_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vehicle_service_db;

-- ---------------------------------------------------------------------
--  users  (model/User.java)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt hash',
    phone       VARCHAR(20)  NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  vehicles  (model/Vehicle.java)   one user -> many vehicles
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicles (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    user_id              BIGINT      NOT NULL,
    registration_number  VARCHAR(20) NOT NULL,
    make                 VARCHAR(50) NOT NULL,
    model                VARCHAR(50) NOT NULL,
    `year`               INT         NOT NULL,
    fuel_type            VARCHAR(20) NOT NULL COMMENT 'PETROL, DIESEL, ELECTRIC, HYBRID, CNG, OTHER',
    current_mileage      INT         NOT NULL DEFAULT 0 COMMENT 'kilometres',
    color                VARCHAR(30) NULL,
    purchase_date        DATE        NULL,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_vehicles PRIMARY KEY (id),
    -- the same registration number cannot be added twice by the same user
    CONSTRAINT uk_vehicles_user_registration UNIQUE (user_id, registration_number),
    -- deleting a user account deletes that user's vehicles
    CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_vehicles_user_id (user_id),
    INDEX idx_vehicles_registration_number (registration_number)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  service_records  (model/ServiceRecord.java)   one vehicle -> many services
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_records (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    vehicle_id            BIGINT        NOT NULL,
    service_type          VARCHAR(30)   NOT NULL COMMENT 'GENERAL_SERVICE, OIL_CHANGE, BRAKE_SERVICE, ...',
    service_date          DATE          NOT NULL,
    mileage               INT           NOT NULL COMMENT 'odometer at service, km',
    cost                  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    service_center        VARCHAR(150)  NULL,
    description           VARCHAR(1000) NULL,
    next_service_date     DATE          NULL,
    next_service_mileage  INT           NULL,
    created_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_service_records PRIMARY KEY (id),
    -- service history has no meaning without its vehicle
    CONSTRAINT fk_service_records_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    INDEX idx_service_records_vehicle_id (vehicle_id),
    INDEX idx_service_records_service_date (service_date)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  reminders  (model/Reminder.java)   one vehicle -> many reminders
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reminders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    vehicle_id     BIGINT        NOT NULL,
    reminder_type  VARCHAR(30)   NOT NULL COMMENT 'GENERAL_SERVICE, INSURANCE, POLLUTION_CERTIFICATE, ...',
    title          VARCHAR(150)  NOT NULL,
    description    VARCHAR(1000) NULL,
    due_date       DATE          NULL,
    due_mileage    INT           NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'UPCOMING' COMMENT 'UPCOMING, DUE_SOON, OVERDUE, COMPLETED',
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_reminders PRIMARY KEY (id),
    CONSTRAINT fk_reminders_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    INDEX idx_reminders_vehicle_id (vehicle_id),
    INDEX idx_reminders_due_date (due_date)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  expenses  (model/Expense.java)   one vehicle -> many expenses
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expenses (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    vehicle_id    BIGINT        NOT NULL,
    expense_type  VARCHAR(20)   NOT NULL COMMENT 'SERVICE, FUEL, INSURANCE, REPAIR, TYRES, BATTERY, PARTS, CLEANING, OTHER',
    amount        DECIMAL(10,2) NOT NULL,
    expense_date  DATE          NOT NULL,
    description   VARCHAR(500)  NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (id) ON DELETE CASCADE,
    INDEX idx_expenses_vehicle_id (vehicle_id),
    INDEX idx_expenses_expense_date (expense_date)
) ENGINE = InnoDB;


-- =====================================================================
--  PART 2 (OPTIONAL): demo data
--  Dates are relative to today (CURDATE()) so the dashboard always shows
--  a mix of upcoming, due-soon and overdue reminders.
--  The app recalculates reminder statuses automatically when pages load.
-- =====================================================================

-- Remove a previous demo account; ON DELETE CASCADE removes its vehicles and records.
DELETE FROM users WHERE email = 'demo@vehicleservice.com';

-- Password: demo1234  (BCrypt, strength 10)
INSERT INTO users (full_name, email, password, phone, created_at)
VALUES ('Demo User', 'demo@vehicleservice.com',
        '$2a$10$H7fi42VOqnunrrWfjN2Fgeui3yF2V6EH73Yt/tMx9OLrOIARvB2Ba',
        '9876543210', NOW(6));
SET @demo_user = LAST_INSERT_ID();

-- ---------- Vehicles ----------
INSERT INTO vehicles (user_id, registration_number, make, model, `year`, fuel_type, current_mileage, color, purchase_date, created_at)
VALUES (@demo_user, 'KL 01 AB 1234', 'Maruti Suzuki', 'Swift', 2019, 'PETROL', 42350, 'Pearl Arctic White', '2019-08-14', NOW(6));
SET @swift = LAST_INSERT_ID();

INSERT INTO vehicles (user_id, registration_number, make, model, `year`, fuel_type, current_mileage, color, purchase_date, created_at)
VALUES (@demo_user, 'KL 07 CD 4521', 'Honda', 'Activa 6G', 2021, 'PETROL', 18200, 'Matte Axis Grey', '2021-03-02', NOW(6));
SET @activa = LAST_INSERT_ID();

INSERT INTO vehicles (user_id, registration_number, make, model, `year`, fuel_type, current_mileage, color, purchase_date, created_at)
VALUES (@demo_user, 'KL 22 EV 0808', 'Tata', 'Nexon EV', 2023, 'ELECTRIC', 12500, 'Intensi-Teal', '2023-11-20', NOW(6));
SET @nexon = LAST_INSERT_ID();

-- ---------- Service records ----------
INSERT INTO service_records (vehicle_id, service_type, service_date, mileage, cost, service_center, description, next_service_date, next_service_mileage, created_at) VALUES
(@swift,  'GENERAL_SERVICE',     DATE_SUB(CURDATE(), INTERVAL 14 MONTH), 30120, 5850.00, 'Popular Maruti, Thiruvananthapuram', 'Periodic maintenance: engine oil, oil filter, air filter, spark plugs checked.', DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 40120, NOW(6)),
(@swift,  'OIL_CHANGE',          DATE_SUB(CURDATE(), INTERVAL 8 MONTH),  35400, 2400.00, 'Quick Lube, Kazhakoottam',           'Engine oil 5W-30 and oil filter replaced.', NULL, NULL, NOW(6)),
(@swift,  'BRAKE_SERVICE',       DATE_SUB(CURDATE(), INTERVAL 5 MONTH),  38900, 3200.00, 'Popular Maruti, Thiruvananthapuram', 'Front brake pads replaced, discs cleaned, brake fluid topped up.', NULL, NULL, NOW(6)),
(@swift,  'GENERAL_SERVICE',     DATE_SUB(CURDATE(), INTERVAL 2 MONTH),  41800, 6450.00, 'Popular Maruti, Thiruvananthapuram', '40,000 km service. Coolant replaced, wipers changed.', DATE_ADD(CURDATE(), INTERVAL 4 MONTH), 51800, NOW(6)),
(@swift,  'WHEEL_ALIGNMENT',     DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  42100,  850.00, 'Wheel Care, Pattom',                 'Alignment and balancing for all four wheels.', NULL, NULL, NOW(6)),
(@activa, 'GENERAL_SERVICE',     DATE_SUB(CURDATE(), INTERVAL 10 MONTH), 14000, 1150.00, 'Honda Service Centre, Kochi',        'Regular service, chain adjusted, brakes tightened.', NULL, NULL, NOW(6)),
(@activa, 'BATTERY_REPLACEMENT', DATE_SUB(CURDATE(), INTERVAL 6 MONTH),  16000, 1850.00, 'Amaron Point, Kochi',                'New 12V maintenance-free battery, 24 months warranty.', NULL, NULL, NOW(6)),
(@activa, 'OIL_CHANGE',          DATE_SUB(CURDATE(), INTERVAL 3 MONTH),  17100,  450.00, 'Honda Service Centre, Kochi',        'Engine oil changed.', DATE_ADD(CURDATE(), INTERVAL 3 MONTH), 18500, NOW(6)),
(@nexon,  'GENERAL_SERVICE',     DATE_SUB(CURDATE(), INTERVAL 4 MONTH),  10000, 1200.00, 'Tata Motors EV Service, Kochi',      '10,000 km check-up: software update, cabin filter, tyre rotation.', NULL, NULL, NOW(6));

-- ---------- Reminders (one of each status) ----------
INSERT INTO reminders (vehicle_id, reminder_type, title, description, due_date, due_mileage, status, created_at) VALUES
(@swift,  'GENERAL_SERVICE',       'Next general service',       'Created from the 40,000 km service.',             DATE_ADD(CURDATE(), INTERVAL 4 MONTH),  51800, 'UPCOMING',  NOW(6)),
(@swift,  'INSURANCE',             'Insurance renewal',          'Comprehensive policy. Compare quotes before renewing.', DATE_ADD(CURDATE(), INTERVAL 18 DAY), NULL,  'DUE_SOON',  NOW(6)),
(@swift,  'POLLUTION_CERTIFICATE', 'PUC certificate renewal',    'Any authorised emission testing centre.',         DATE_SUB(CURDATE(), INTERVAL 5 DAY),    NULL,  'OVERDUE',   NOW(6)),
(@activa, 'OIL_CHANGE',            'Engine oil change',          'Due every 1,400 km or 6 months.',                 DATE_ADD(CURDATE(), INTERVAL 3 MONTH),  18500, 'DUE_SOON',  NOW(6)),
(@activa, 'INSURANCE',             'Two-wheeler insurance',      NULL,                                              DATE_ADD(CURDATE(), INTERVAL 7 MONTH),  NULL,  'UPCOMING',  NOW(6)),
(@activa, 'POLLUTION_CERTIFICATE', 'PUC certificate renewal',    'Renewed at the petrol pump testing centre.',      DATE_SUB(CURDATE(), INTERVAL 20 DAY),   NULL,  'COMPLETED', NOW(6)),
(@nexon,  'TYRE_REPLACEMENT',      'Tyre rotation / inspection', 'Rotate every 12,000 km.',                         NULL,                                   12000, 'OVERDUE',   NOW(6)),
(@nexon,  'REGISTRATION',          'Registration renewal',       NULL,                                              DATE_ADD(CURDATE(), INTERVAL 14 MONTH), NULL,  'UPCOMING',  NOW(6));

-- ---------- Expenses (spread over recent months for the charts) ----------
INSERT INTO expenses (vehicle_id, expense_type, amount, expense_date, description, created_at) VALUES
(@swift,  'FUEL',      3200.00, DATE_SUB(CURDATE(), INTERVAL 5 MONTH), 'Full tank', NOW(6)),
(@swift,  'FUEL',      2950.00, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), 'Full tank', NOW(6)),
(@swift,  'FUEL',      3100.00, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'Full tank', NOW(6)),
(@swift,  'FUEL',      3050.00, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'Full tank', NOW(6)),
(@swift,  'FUEL',      3300.00, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'Full tank before trip', NOW(6)),
(@swift,  'FUEL',      1500.00, DATE_SUB(CURDATE(), INTERVAL 6 DAY),   'Half tank', NOW(6)),
(@swift,  'INSURANCE', 14200.00, DATE_SUB(CURDATE(), INTERVAL 11 MONTH), 'Comprehensive policy, 1 year', NOW(6)),
(@swift,  'CLEANING',   650.00, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'Foam wash and interior vacuum', NOW(6)),
(@swift,  'PARTS',      1200.00, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'Floor mats and phone holder', NOW(6)),
(@activa, 'FUEL',        600.00, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), NULL, NOW(6)),
(@activa, 'FUEL',        650.00, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), NULL, NOW(6)),
(@activa, 'FUEL',        620.00, DATE_SUB(CURDATE(), INTERVAL 12 DAY),  NULL, NOW(6)),
(@activa, 'REPAIR',      900.00, DATE_SUB(CURDATE(), INTERVAL 5 MONTH), 'Side mirror replaced', NOW(6)),
(@activa, 'INSURANCE',  1850.00, DATE_SUB(CURDATE(), INTERVAL 5 MONTH), 'Third-party + own damage', NOW(6)),
(@nexon,  'FUEL',        950.00, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'Home charging (electricity share)', NOW(6)),
(@nexon,  'FUEL',        450.00, DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'Fast charger on highway', NOW(6)),
(@nexon,  'CLEANING',    400.00, DATE_SUB(CURDATE(), INTERVAL 20 DAY),  'Exterior wash', NOW(6));

-- Quick check (optional):
-- SELECT COUNT(*) AS vehicles  FROM vehicles  WHERE user_id = @demo_user;
-- SELECT status, COUNT(*)      FROM reminders GROUP BY status;
