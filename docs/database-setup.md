# Database Setup

MySQL 8.0 or newer.

## 1. One-time setup (as root)

In MySQL Workbench (root connection) or the `mysql` command line:

```sql
CREATE DATABASE smartcity CHARACTER SET utf8mb4;
CREATE USER 'smartcity_app'@'localhost' IDENTIFIED BY '<choose-a-password>';
GRANT ALL PRIVILEGES ON smartcity.* TO 'smartcity_app'@'localhost';
```

The app always connects as `smartcity_app`, never as root.

## 2. Configure the app

Copy `db.properties.example` (repo root) to `db.properties` and set `db.password` to the password you chose above. `db.properties` is gitignored.

## 3. Create tables and demo data

Run `db/schema.sql`, then `db/seed.sql`, as `smartcity_app`.

**Workbench:** open a `smartcity_app` connection → File → Open SQL Script → `db/schema.sql` → run (lightning bolt). Repeat for `db/seed.sql`.

**Command line** (from the repo root; `mysql.exe` is in `C:\Program Files\MySQL\MySQL Server 8.0\bin` on Windows):

```
mysql -u smartcity_app -p < db/schema.sql
mysql -u smartcity_app -p < db/seed.sql
```

To reset everything to a clean demo state, run both scripts again. `schema.sql` drops all tables first, so **all data is lost**.

## Demo accounts

All passwords are `password123`.

| Email | Role |
|---|---|
| `citizen@smartcity.test` | Citizen |
| `admin@smartcity.test` | City Administrator |
| `manager@smartcity.test` | Department Manager (Public Works) |
| `worker@smartcity.test` | Worker (Public Works) |
| `utilities.manager@smartcity.test` | Department Manager (Utilities) |
| `utilities.worker@smartcity.test` | Worker (Utilities) |
| `parks.manager@smartcity.test` | Department Manager (Parks & Recreation) |
| `parks.worker@smartcity.test` | Worker (Parks & Recreation) |

## Check it worked

```sql
SELECT u.email, r.role_name, LEFT(u.password_hash, 4) AS hash_prefix
FROM smartcity.users u JOIN smartcity.roles r ON r.role_id = u.role_id;
```

Expect 4 rows, each with `hash_prefix` = `$2a$`.
