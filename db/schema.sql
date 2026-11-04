-- SmartCity Hub database schema
-- Re-runnable: drops and recreates every table. ALL DATA IS LOST.
-- Enum values must match docs/api.md section 9.
-- Run as smartcity_app (see docs/database-setup.md), then run seed.sql.

USE smartcity;

DROP TABLE IF EXISTS flags, notifications, announcements, tasks, service_requests,
                     event_registrations, events, users, departments, roles, transit_routes;

-- ---------------------------------------------------------------------------
-- Users and organization
-- ---------------------------------------------------------------------------

CREATE TABLE roles (
  role_id   TINYINT     PRIMARY KEY,
  role_name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE departments (
  department_id INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(255) NOT NULL UNIQUE,      -- stored trimmed + lowercased
  password_hash CHAR(60)     NOT NULL,             -- BCrypt, never plaintext
  role_id       TINYINT      NOT NULL,
  address       VARCHAR(255) NULL,                 -- Citizen
  phone         VARCHAR(20)  NULL,                 -- Citizen
  department_id INT          NULL,                 -- DepartmentManager, Worker
  skill         VARCHAR(100) NULL,                 -- Worker
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at DATETIME     NULL,                 -- engagement report
  FOREIGN KEY (role_id)       REFERENCES roles(role_id),
  FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- ---------------------------------------------------------------------------
-- Events
-- ---------------------------------------------------------------------------

CREATE TABLE events (
  event_id        INT AUTO_INCREMENT PRIMARY KEY,
  title           VARCHAR(150) NOT NULL,
  description     TEXT         NULL,
  category        ENUM('COMMUNITY_SERVICE','SPORTS','PUBLIC_SAFETY',
                       'EDUCATION','ENVIRONMENT','CULTURAL') NOT NULL,
  start_time      DATETIME     NOT NULL,
  district        ENUM('DOWNTOWN','NORTH','SOUTH','EAST','WEST') NOT NULL,
  location_detail VARCHAR(255) NULL,
  price           DECIMAL(8,2) NOT NULL DEFAULT 0.00,   -- 0 = free
  created_by      INT          NOT NULL,                -- CityAdministrator
  is_cancelled    BOOLEAN      NOT NULL DEFAULT FALSE,  -- DELETE /events/{id} (soft delete)
  is_hidden       BOOLEAN      NOT NULL DEFAULT FALSE,  -- removed via flag
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(user_id),
  CHECK (price >= 0),
  INDEX idx_events_category (category),
  INDEX idx_events_start (start_time),
  INDEX idx_events_district (district)
);

CREATE TABLE event_registrations (
  user_id       INT      NOT NULL,
  event_id      INT      NOT NULL,
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, event_id),                      -- ALREADY_REGISTERED
  FOREIGN KEY (user_id)  REFERENCES users(user_id),
  FOREIGN KEY (event_id) REFERENCES events(event_id),
  INDEX idx_registrations_event (event_id)
);

-- ---------------------------------------------------------------------------
-- Service requests
-- ---------------------------------------------------------------------------

CREATE TABLE service_requests (
  request_id       INT AUTO_INCREMENT PRIMARY KEY,
  citizen_id       INT           NOT NULL,
  type             ENUM('STREETLIGHT','POTHOLE','GRAFFITI',
                        'WATER_LEAK','NOISE','OTHER') NOT NULL,
  description      TEXT          NOT NULL,
  location         VARCHAR(255)  NOT NULL,              -- free-text address
  status           ENUM('SUBMITTED','UNDER_REVIEW','ASSIGNED',
                        'IN_PROGRESS','RESOLVED','REJECTED') NOT NULL DEFAULT 'SUBMITTED',
  department_id    INT           NULL,                  -- set by admin on assign
  rejection_reason VARCHAR(500)  NULL,
  resolution_notes VARCHAR(1000) NULL,
  is_hidden        BOOLEAN       NOT NULL DEFAULT FALSE,  -- removed via flag
  submitted_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_at      DATETIME      NULL,
  FOREIGN KEY (citizen_id)    REFERENCES users(user_id),
  FOREIGN KEY (department_id) REFERENCES departments(department_id),
  INDEX idx_requests_citizen (citizen_id),
  INDEX idx_requests_status (status),
  INDEX idx_requests_department (department_id)
);

-- A request has at most one task: the worker currently responsible for it.
-- The request's status is the only workflow status; tasks have none.
CREATE TABLE tasks (
  task_id     INT AUTO_INCREMENT PRIMARY KEY,
  request_id  INT           NOT NULL UNIQUE,
  worker_id   INT           NOT NULL,
  assigned_by INT           NOT NULL,                   -- DepartmentManager
  notes       VARCHAR(1000) NULL,
  assigned_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (request_id)  REFERENCES service_requests(request_id),
  FOREIGN KEY (worker_id)   REFERENCES users(user_id),
  FOREIGN KEY (assigned_by) REFERENCES users(user_id)
);

-- ---------------------------------------------------------------------------
-- Messaging and moderation
-- ---------------------------------------------------------------------------

CREATE TABLE announcements (
  announcement_id INT AUTO_INCREMENT PRIMARY KEY,
  type            ENUM('GENERAL','EMERGENCY') NOT NULL,
  title           VARCHAR(150) NOT NULL,
  message         TEXT         NOT NULL,
  created_by      INT          NOT NULL,
  is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,  -- DELETE /announcements/{id}
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- Rows written by the Observer pattern (NotificationService); read via /me/notifications.
CREATE TABLE notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id         INT          NOT NULL,
  type            ENUM('ANNOUNCEMENT','EMERGENCY_ALERT','REQUEST_STATUS',
                       'TASK_ASSIGNED','EVENT_CANCELLED') NOT NULL,
  message         VARCHAR(500) NOT NULL,
  related_id      INT          NULL,                    -- request/event/announcement id
  is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  INDEX idx_notifications_user (user_id, is_read)
);

-- content_id points at events or service_requests depending on content_type,
-- so it has no foreign key; the server validates that the content exists.
CREATE TABLE flags (
  flag_id      INT AUTO_INCREMENT PRIMARY KEY,
  content_type ENUM('EVENT','REQUEST') NOT NULL,
  content_id   INT          NOT NULL,
  reason       VARCHAR(500) NOT NULL,
  flagged_by   INT          NOT NULL,
  status       ENUM('OPEN','DISMISSED','REMOVED') NOT NULL DEFAULT 'OPEN',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (flagged_by) REFERENCES users(user_id),
  UNIQUE KEY uq_flag_once (content_type, content_id, flagged_by),  -- ALREADY_FLAGGED
  INDEX idx_flags_status (status)
);

-- ---------------------------------------------------------------------------
-- Transit (read-only, seeded)
-- ---------------------------------------------------------------------------

CREATE TABLE transit_routes (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  route          VARCHAR(20)  NOT NULL,
  stop           VARCHAR(100) NOT NULL,
  departure_time TIME         NOT NULL,
  arrival_time   TIME         NOT NULL
);
