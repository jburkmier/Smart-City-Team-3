-- SmartCity Hub demo data
-- Run after schema.sql (schema.sql empties every table first).
--
-- Demo accounts (password for all: password123)
--   citizen@smartcity.test   CITIZEN
--   admin@smartcity.test     CITY_ADMINISTRATOR
--   manager@smartcity.test   DEPARTMENT_MANAGER (Public Works)
--   worker@smartcity.test    WORKER             (Public Works)
--
-- Hashes were generated with jbcrypt 0.4: BCrypt.hashpw("password123", BCrypt.gensalt())
-- and verified with BCrypt.checkpw. To add a user, generate a new hash the same way.

USE smartcity;

INSERT INTO roles (role_id, role_name) VALUES
  (1, 'CITIZEN'),
  (2, 'CITY_ADMINISTRATOR'),
  (3, 'DEPARTMENT_MANAGER'),
  (4, 'WORKER');

INSERT INTO departments (department_id, name) VALUES
  (1, 'Public Works'),
  (2, 'Utilities'),
  (3, 'Parks & Recreation');

INSERT INTO users (user_id, name, email, password_hash, role_id, address, phone, department_id, skill) VALUES
  (1, 'Alex Rivera',  'citizen@smartcity.test', '$2a$10$ArMUdoUZoE1Gy8iIoY8.peyaASSi1fe9Od4jCtdUXT9BmtWzoStXO', 1, '123 Main St', '480-555-0101', NULL, NULL),
  (2, 'Morgan Chen',  'admin@smartcity.test',   '$2a$10$Zyn/tXfjaVwp/8BkVxWh/.4bsa243.ZL37FO5WSL1pSLzol3H3eD6', 2, NULL,          NULL,           NULL, NULL),
  (3, 'Jordan Patel', 'manager@smartcity.test', '$2a$10$8ydu5YKEymL6mvnN46f9KOgroiosINhNhgWEz5fSxifV9iKs8wqYK', 3, NULL,          NULL,           1,    NULL),
  (4, 'Sam Lee',      'worker@smartcity.test',  '$2a$10$opvAWwRxqPcyh2eKisrvpu23XBBF/6d6x3L4uyf8U6p.6o5OvZo.W', 4, NULL,          NULL,           1,    'Road repair');

-- Part 2 adds: ~10 events across all categories/districts, ~8 service requests in every status, a second worker.
-- Part 3 adds: announcements (one EMERGENCY), transit routes, a couple of open flags.
