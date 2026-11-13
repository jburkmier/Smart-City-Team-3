-- SmartCity Hub demo data
-- Run after schema.sql (schema.sql empties every table first).
--
-- Demo accounts (password for all: password123)
--   citizen@smartcity.test   CITIZEN
--   admin@smartcity.test     CITY_ADMINISTRATOR
--   manager@smartcity.test   DEPARTMENT_MANAGER (Public Works)
--   worker@smartcity.test    WORKER             (Public Works)
--   utilities.manager@smartcity.test  DEPARTMENT_MANAGER (Utilities)
--   utilities.worker@smartcity.test   WORKER             (Utilities)
--   parks.manager@smartcity.test      DEPARTMENT_MANAGER (Parks & Recreation)
--   parks.worker@smartcity.test       WORKER             (Parks & Recreation)
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
  (4, 'Sam Lee',      'worker@smartcity.test',  '$2a$10$opvAWwRxqPcyh2eKisrvpu23XBBF/6d6x3L4uyf8U6p.6o5OvZo.W', 4, NULL,          NULL,           1,    'Road repair'),
  (5, 'Reed Richards', 'utilities.manager@smartcity.test', '$2a$10$O9qsVKG2rXLlclsAJ.fOtO3Ex08FflubHILHrT4YntVjRSoVJpNiK', 3, NULL, NULL, 2, NULL),
  (6, 'Johnny Storm',  'utilities.worker@smartcity.test',  '$2a$10$T4WqA.2c01TW37.ULNajdOQrxMb00HLYqSJLsKOqzoMtoX9dt1U/W', 4, NULL, NULL, 2, 'Water and electrical'),
  (7, 'Sue Storm',     'parks.manager@smartcity.test',     '$2a$10$rk706dPVY9RCat4YnN2m5uAkH.0EBt1ifu77rD3UUvLjCXJhoCPEa', 3, NULL, NULL, 3, NULL),
  (8, 'Ben Grimm',     'parks.worker@smartcity.test',      '$2a$10$GA0D/H39uGkCXItiy35SYOHfaU597iS1Z.XOfdUHii7bLE99wQOpe', 4, NULL, NULL, 3, 'Landscaping');

-- Events span all six categories and five districts, with both free and paid options.
-- Relative dates keep upcoming events current; past registrations demonstrate the
-- difference between all registrations and the "Upcoming only" view.
INSERT INTO events
  (event_id, title, description, category, start_time, district, location_detail, price, created_by)
VALUES
  (1, 'River Park Cleanup', 'Help collect litter and restore the riverbank trails.',
   'COMMUNITY_SERVICE', DATE_ADD(NOW(), INTERVAL 7 DAY), 'NORTH', 'River Park main entrance', 0.00, 2),
  (2, 'Downtown 5K Fun Run', 'A community run through downtown streets. All fitness levels welcome.',
   'SPORTS', DATE_ADD(NOW(), INTERVAL 10 DAY), 'DOWNTOWN', 'Civic Plaza', 15.00, 2),
  (3, 'Neighborhood Safety Workshop', 'Learn practical emergency preparedness and home safety tips.',
   'PUBLIC_SAFETY', DATE_ADD(NOW(), INTERVAL 5 DAY), 'EAST', 'East Community Center', 0.00, 2),
  (4, 'Community Gardening Class', 'Learn seasonal planting and water-wise gardening basics.',
   'EDUCATION', DATE_ADD(NOW(), INTERVAL 14 DAY), 'SOUTH', 'Southside Library', 5.00, 2),
  (5, 'Urban Tree Planting Day', 'Plant shade trees and help improve the neighborhood canopy.',
   'ENVIRONMENT', DATE_ADD(NOW(), INTERVAL 3 DAY), 'WEST', 'Westside Greenway', 0.00, 2),
  (6, 'Cultural Food and Music Festival', 'Celebrate local cultures with food, music, and family activities.',
   'CULTURAL', DATE_ADD(NOW(), INTERVAL 21 DAY), 'DOWNTOWN', 'Central Park stage', 12.00, 2),
  (7, 'Youth Soccer Tournament', 'A friendly weekend tournament for local youth teams.',
   'SPORTS', DATE_ADD(NOW(), INTERVAL 17 DAY), 'NORTH', 'Northside Sports Complex', 8.00, 2),
  (8, 'Emergency Preparedness Fair', 'Meet first responders and explore preparedness resources.',
   'PUBLIC_SAFETY', DATE_ADD(NOW(), INTERVAL 12 DAY), 'SOUTH', 'Fire Station 4', 0.00, 2),
  (9, 'Water Conservation Seminar', 'Simple ways to reduce household water use in a desert climate.',
   'EDUCATION', DATE_SUB(NOW(), INTERVAL 7 DAY), 'WEST', 'Westside Library', 0.00, 2),
  (10, 'Community Mural Workshop', 'Help design a neighborhood mural with local artists.',
   'CULTURAL', DATE_SUB(NOW(), INTERVAL 2 DAY), 'EAST', 'East Arts Center', 10.00, 2);

-- Citizen Alex Rivera is registered for upcoming events and a past event.
INSERT INTO event_registrations (user_id, event_id) VALUES
  (1, 1),
  (1, 4),
  (1, 9);

-- Part 2 adds: ~8 service requests in every status, a second worker.
-- Part 3 adds: announcements (one EMERGENCY), transit routes, a couple of open flags.
