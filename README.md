# Smart City Team 3

Contributors: Jenny Burkmier and Nicholas Nguyen

SmartCity Hub is a web app: a Javalin server provides the REST API and serves the web pages, backed by MySQL.

## Requirements
 - Java 18 or newer (Gradle downloads JDK 18 automatically if it's missing)
 - MySQL 8.0 or newer (see [docs/database-setup.md](docs/database-setup.md))
 - A web browser
 - No separate Gradle install needed: use the wrapper (`./gradlew`, Gradle 8.13)

## Project layout
 - `server/src/main/java`: REST API under `/api` (see [docs/api.md](docs/api.md))
 - `server/src/main/resources/public`: web pages (HTML, CSS, JavaScript)
 - `db/`: database schema and seed data

## Getting started
1. Set up MySQL, `db.properties`, and the demo data by following [docs/database-setup.md](docs/database-setup.md).
2. `./gradlew :server:run` starts the server.
3. Open http://localhost:7070 and log in with a demo account.

## Demo accounts
All passwords are `password123`.

| Email | Role |
|---|---|
| citizen@smartcity.test | Citizen |
| admin@smartcity.test | City Administrator |
| publicworks.manager@smartcity.test | Department Manager (Public Works) |
| publicworks.worker@smartcity.test | Worker (Public Works) |
| publicworks.worker2@smartcity.test | Worker (Public Works) |
| utilities.manager@smartcity.test | Department Manager (Utilities) |
| utilities.worker@smartcity.test | Worker (Utilities) |
| parks.manager@smartcity.test | Department Manager (Parks & Recreation) |
| parks.worker@smartcity.test | Worker (Parks & Recreation) |

New accounts created on the Register page are always Citizens.

Department managers use **Assign Tasks** to assign or reassign a worker and start or resolve work. **Department Requests** is for viewing requests; worker task actions are not shown there. The current assignee is highlighted in each task card.

## Commands
  - `./gradlew build` builds the server and runs the tests
  - `./gradlew :server:run` starts the server, then open http://localhost:7070

Changes to the web pages need a server restart to show up.
