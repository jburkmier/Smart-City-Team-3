# Smart City Team 3

Contributors: Jenny Burkmier and Nicholas Nguyen

## Requirements
 - Java 18 or newer (Gradle downloads JDK 18 automatically if it's missing)
 - MySQL 8.0 or newer (see [docs/database-setup.md](docs/database-setup.md))
 - No separate Gradle install needed: use the wrapper (`./gradlew`, Gradle 8.13)

## Modules
 - `common`: shared DTOs and enums
 - `server`: Javalin REST server on port 7070
 - `client`: JavaFX desktop app

## Commands
  - `./gradlew build` builds all modules and runs the tests
  - `./gradlew :server:run` starts the server (start this first)
  - `./gradlew :client:run` starts the client app
