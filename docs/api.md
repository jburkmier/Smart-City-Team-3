# SmartCity Hub – REST API Contract

This is the agreement between the web client (HTML/JavaScript pages in `server/src/main/resources/public`) and the Javalin REST API. Change it here first (and tell your teammate) before changing either side.

| Increment | Due | Endpoints |
|---|---|---|
| **Part 1** | Nov 7 | Health, Auth, `/me` |
| **Part 2** | Nov 14 | Departments, Events, Event registration, Service requests (submit, list, review, assign, reject) |
| **Part 3** | Nov 21 | Tasks, Request start/resolve, Flags, Announcements, Notifications, Reports, Transit |

---

## 1. Conventions

- **Base URL:** `http://localhost:7070/api`
- **Format:** JSON, `camelCase` field names, `Content-Type: application/json`
- **IDs:** integers
- **Dates/times:** ISO-8601 local time strings, e.g. `"2026-11-05T18:00:00"`
- **Enums:** uppercase strings (see [Enums](#9-enums)). Java enums on the server; the web client uses the same strings.
- **Lists:** returned as plain JSON arrays. No pagination (data sets are small).
- **DTOs:** every request/response body is a Java record in the server's `dto` package. Domain classes (`Citizen`, `Request`, …) are never serialized directly, and **no response ever contains a password or password hash**.

### URL structure

| Group | Paths | Rule |
|---|---|---|
| Session | `/auth/*`, `/health` | No token needed |
| Caller | `/me`, `/me/*` | Always the logged-in user's own data. No IDs of other users. |
| Resources | `/events`, `/requests`, `/departments`, `/announcements`, `/flags`, `/transit` | Plural nouns. Lists may be role-scoped (an admin sees more than a manager). |
| Reports | `/reports/*` | Read-only, computed on request |

- State changes on a service request are **action endpoints** (`POST /requests/{id}/assign`). Each one maps to one method on the State pattern's `RequestState`.

### Authentication

1. `POST /auth/login` returns a `token` (random UUID).
2. Every other endpoint (except `/health` and `/auth/*`) requires the header:
   ```
   Authorization: Bearer <token>
   ```
3. The server resolves the token through `SessionManager` (Singleton, `Map<token, SystemUser>`).
4. Tokens live in server memory only. Logout or a server restart invalidates them.
5. The web client keeps the token in `sessionStorage` (the `Session` object in `js/api.js`), which the browser clears when the tab closes. Never `localStorage` or cookies.
6. **Client rule:** any `401` response (except a failed login) clears `Session` and returns to the login page. `js/api.js` does this automatically.

### Authorization

The server checks the caller's role on every protected endpoint. Hiding buttons in the UI is not enough.

- Wrong role → `403 FORBIDDEN`
- Right role but not your resource (e.g. another citizen's request, another department's request) → `404 NOT_FOUND` (do not reveal that it exists)

### Error format

Every error response uses the same shape:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Some fields are invalid.",
  "fields": {
    "email": "Invalid email format",
    "password": "Must be at least 8 characters"
  }
}
```

- `error`: fixed code the client can switch on
- `message`: human-readable text the client can show as-is
- `fields`: only present for `VALIDATION_FAILED`

| Status | `error` code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Missing/invalid fields, unparsable JSON |
| 401 | `UNAUTHORIZED` | Missing, unknown, or logged-out token |
| 401 | `INVALID_CREDENTIALS` | Login failed (same response for wrong email or wrong password) |
| 403 | `FORBIDDEN` | Caller's role cannot do this |
| 404 | `NOT_FOUND` | Resource doesn't exist or caller can't see it |
| 409 | `EMAIL_TAKEN` | Register with an existing email |
| 409 | `ALREADY_REGISTERED` | Citizen is already registered for the event |
| 409 | `ALREADY_FLAGGED` | Caller already has an open flag on that content |
| 409 | `INVALID_TRANSITION` | Request state change not allowed from its current state |
| 500 | `INTERNAL_ERROR` | Unexpected server error |
| 503 | `DATABASE_UNAVAILABLE` | Server can't reach MySQL |

---

## 2. Part 1: Health, Auth, Me

### `GET /health`
No auth. The client calls it on startup and shows "Server unavailable" on failure.

- `200` → `{ "status": "UP", "database": "UP" }`
- `503` → `{ "status": "UP", "database": "DOWN" }`

### `POST /auth/register`
No auth. Always creates a **CITIZEN**. The request has no `role` field, and the server ignores one if sent.

```json
{
  "name": "Alex Rivera",
  "email": "alex@example.com",
  "password": "secret123",
  "address": "123 Main St",
  "phone": "480-555-0101"
}
```

| Field | Rule |
|---|---|
| `name` | required, 1–100 chars, trimmed |
| `email` | required, valid format, ≤ 255 chars, trimmed + lowercased before saving |
| `password` | required, 8–72 chars (BCrypt limit). Stored as a BCrypt hash. |
| `address` | optional, ≤ 255 chars |
| `phone` | optional, ≤ 20 chars |

- `201` → [`UserDto`](#userdto). The client then shows the login screen with the email prefilled and "Account created. Please log in."
- `400 VALIDATION_FAILED` · `409 EMAIL_TAKEN`

### `POST /auth/login`
No auth.

```json
{ "email": "alex@example.com", "password": "secret123" }
```

- `200` →
  ```json
  { "token": "3f9c2a1e-8b7d-4c1a-9e2f-0a1b2c3d4e5f", "user": { /* UserDto */ } }
  ```
  Starting in Part 3, the server also updates `users.last_login_at` (used by the engagement report). Part 1 does not update it.
- `400 VALIDATION_FAILED` (missing fields) · `401 INVALID_CREDENTIALS`, message `"Invalid email or password."`

Server flow: `UserDAO.findByEmail` → `BCrypt.checkpw` → `UserCreator` for the role (Factory) builds `Citizen`/`CityAdministrator`/`DepartmentManager`/`Worker` → `SessionManager.login(user)` returns the token.

### `POST /auth/logout`
Auth header. Removes the token from `SessionManager`.

- `204`, always, even if the token was already invalid (logout is idempotent).

### `GET /me`
Auth required. The home screen uses `role` to choose which navigation to show.

- `200` → [`UserDto`](#userdto)
- `401 UNAUTHORIZED`

#### `UserDto`
```json
{
  "id": 7,
  "name": "Alex Rivera",
  "email": "alex@example.com",
  "role": "CITIZEN",
  "address": "123 Main St",
  "phone": "480-555-0101",
  "departmentId": null
}
```
`departmentId` is set for `DEPARTMENT_MANAGER` and `WORKER`, `null` otherwise.

### Other `/me` endpoints (added in later parts)

| Endpoint | Roles | Part | Section |
|---|---|---|---|
| `GET /me/registrations` | `CITIZEN` | 2 | [Events](#get-meregistrations) |
| `GET /me/requests` | `CITIZEN` | 2 | [Service Requests](#get-merequests) |
| `GET /me/tasks` | `WORKER` | 3 | [Tasks](#get-metasks) |
| `GET /me/notifications` | any | 3 | [Notifications](#get-menotifications) |
| `POST /me/notifications/{id}/read` | any | 3 | [Notifications](#post-menotificationsidread) |
| `POST /me/notifications/read-all` | any | 3 | [Notifications](#post-menotificationsread-all) |

---

## 3. Part 2: Departments

### `GET /departments`
Roles: `CITY_ADMINISTRATOR`. Used for the "assign to department" dropdown.

- `200` → `[ { "id": 1, "name": "Public Works" }, … ]`

### `GET /departments/{id}/workers`
Roles: `DEPARTMENT_MANAGER` (own department only), `CITY_ADMINISTRATOR`. Used for the "assign worker" dropdown (Part 3).

- `200` → `[ { "id": 12, "name": "Sam Lee", "skill": "Electrical" }, … ]`

---

## 4. Part 2: Events

### `EventDto`
```json
{
  "id": 3,
  "title": "Riverside Park Cleanup",
  "description": "Bring gloves; bags provided.",
  "category": "ENVIRONMENT",
  "startTime": "2026-11-14T09:00:00",
  "district": "DOWNTOWN",
  "locationDetail": "Riverside Park, north entrance",
  "price": 0.00,
  "registrationCount": 18,
  "registeredByMe": false,
  "createdById": 2
}
```
`price` of `0` means the event is free.

### `GET /events`
Roles: any logged-in user. Returns **upcoming, non-cancelled, non-hidden** events.

Query parameters (all optional, combinable):

| Param | Example | Strategy class |
|---|---|---|
| `q` | `cleanup` (case-insensitive match on title or description) | `KeywordFilter` (optional extra) |
| `category` | `SPORTS` | `EventTypeFilter` |
| `from`, `to` | `2026-11-10`, `2026-11-20` | `DateFilter` |
| `district` | `DOWNTOWN` | `LocationFilter` |
| `free` | `true` (only `price == 0`) / `false` (only `price > 0`) | `PriceFilter` (optional extra) |
| `sort` | `date` (default) or `popularity` | sorts by `startTime` or `registrationCount` |

Server flow: `EventService` validates the parameters and creates an `EventFilterStrategy` for each present filter. It wraps them in a `CompositeFilter`, sets that strategy on an `EventCatalog`, then calls `getEvents()`. The catalog starts with all upcoming, visible events; with no filter parameters, the empty composite returns all of them (Browse Community Events). Sorting by date or popularity is applied when loading that collection.

- `200` → `[ EventDto, … ]`
- `400 VALIDATION_FAILED` (unknown category/district, bad date)

### `GET /events/{id}`
Roles: any logged-in user. `200` → `EventDto` · `404`

### `POST /events`
Roles: `CITY_ADMINISTRATOR`.

```json
{
  "title": "Riverside Park Cleanup",
  "description": "Bring gloves; bags provided.",
  "category": "ENVIRONMENT",
  "startTime": "2026-11-14T09:00:00",
  "district": "DOWNTOWN",
  "locationDetail": "Riverside Park, north entrance",
  "price": 0.00
}
```

| Field | Rule |
|---|---|
| `title` | required, 1–150 chars |
| `description` | optional, ≤ 2000 chars |
| `category` | required, `EventCategory` |
| `startTime` | required, must be in the future |
| `district` | required, `District` |
| `locationDetail` | optional, ≤ 255 chars |
| `price` | required, ≥ 0, at most 2 decimal places (`0` = free) |

- `201` → `EventDto` · `400` · `403`

### `PUT /events/{id}`
Roles: `CITY_ADMINISTRATOR`. Same body and rules as `POST`. *(Optional, cut first if behind.)*
- `200` → `EventDto` · `400` · `403` · `404`

### `DELETE /events/{id}`
Roles: `CITY_ADMINISTRATOR`. Soft delete: the event is marked cancelled and disappears from `GET /events`. *(Part 3: registered citizens get an `EVENT_CANCELLED` notification.)*
- `204` · `403` · `404`

### `POST /events/{id}/registration`
Roles: `CITIZEN`. Registers the caller for the event.
- `201` → `EventDto` (with `registeredByMe: true`)
- `404` · `409 ALREADY_REGISTERED` · `400 VALIDATION_FAILED` (event already started)

### `DELETE /events/{id}/registration`
Roles: `CITIZEN`. Cancels the caller's registration.
- `204` · `404` (not registered)

### `GET /me/registrations`
Roles: `CITIZEN`. Events the caller is registered for, soonest first. Includes past events (so the citizen can see what they attended) but not cancelled or hidden ones.

| Param | Example |
|---|---|
| `upcoming` | `true` (only events that haven't started yet) |

- `200` → `[ EventDto, … ]` (every item has `registeredByMe: true`)

---

## 5. Part 2–3: Service Requests

### Workflow (State pattern)

```
SUBMITTED ──review──▶ UNDER_REVIEW ──assign──▶ ASSIGNED ──start──▶ IN_PROGRESS ──resolve──▶ RESOLVED
                           │                 (dept chosen)  (needs a worker)
                           └──reject──▶ REJECTED
```

Review is a required step: an administrator must review a request before it can be assigned or rejected.

| Action | Endpoint | Allowed roles | From → To | Part |
|---|---|---|---|---|
| review | `POST /requests/{id}/review` | `CITY_ADMINISTRATOR` | SUBMITTED → UNDER_REVIEW | 2 |
| assign | `POST /requests/{id}/assign` | `CITY_ADMINISTRATOR` | UNDER_REVIEW → ASSIGNED | 2 |
| reject | `POST /requests/{id}/reject` | `CITY_ADMINISTRATOR` | UNDER_REVIEW → REJECTED | 2 |
| start | `POST /requests/{id}/start` | assigned `WORKER`, `DEPARTMENT_MANAGER` of its department | ASSIGNED → IN_PROGRESS | 3 |
| resolve | `POST /requests/{id}/resolve` | assigned `WORKER`, `DEPARTMENT_MANAGER` of its department | IN_PROGRESS → RESOLVED | 3 |

- Each action calls the matching method on the request's current `RequestState`. If the state doesn't allow it → `409 INVALID_TRANSITION`, e.g. `"Cannot resolve a request that is SUBMITTED."`
- Wrong role → `403 FORBIDDEN`.
- **The request's `status` is the only status in the system.** Tasks don't have their own status.
- Every successful action returns `200` → updated `RequestDto`. *(Part 3: also notifies the citizen through the Observer pattern.)*

### `RequestDto`
```json
{
  "id": 41,
  "type": "POTHOLE",
  "description": "Large pothole in the right lane.",
  "location": "5th St & Mill Ave",
  "status": "ASSIGNED",
  "citizenId": 7,
  "citizenName": "Alex Rivera",
  "departmentId": 1,
  "departmentName": "Public Works",
  "assignedWorkerId": null,
  "assignedWorkerName": null,
  "rejectionReason": null,
  "resolutionNotes": null,
  "submittedAt": "2026-11-09T10:15:00",
  "updatedAt": "2026-11-10T08:02:00",
  "resolvedAt": null
}
```

### `POST /requests`
Roles: `CITIZEN`. Created in `SUBMITTED`.

```json
{ "type": "POTHOLE", "description": "Large pothole in the right lane.", "location": "5th St & Mill Ave" }
```

| Field | Rule |
|---|---|
| `type` | required, `RequestType` |
| `description` | required, 1–2000 chars |
| `location` | required, 1–255 chars (free-text address) |

- `201` → `RequestDto` · `400`

### `GET /me/requests`
Roles: `CITIZEN`. The caller's own requests, newest first (Track Request Status).
- `200` → `[ RequestDto, … ]`

### `GET /requests`
Roles: `CITY_ADMINISTRATOR` (all requests), `DEPARTMENT_MANAGER` (own department only).

| Param | Example |
|---|---|
| `status` | `SUBMITTED` |
| `type` | `GRAFFITI` |
| `departmentId` | `1` (admin only) |

- `200` → `[ RequestDto, … ]` · `403`

### `GET /requests/{id}`
Roles: owning `CITIZEN`, `CITY_ADMINISTRATOR`, `DEPARTMENT_MANAGER` of its department, assigned `WORKER`.
- `200` → `RequestDto` · `404`

### `POST /requests/{id}/review`
No body. `200` → `RequestDto`

### `POST /requests/{id}/assign`
```json
{ "departmentId": 1 }
```
`200` → `RequestDto` · `400` (unknown department)

### `POST /requests/{id}/reject`
```json
{ "reason": "Duplicate of request #38." }
```
`reason` required, 1–500 chars. `200` → `RequestDto`

### `POST /requests/{id}/start` *(Part 3)*
No body. Requires a worker to be assigned ([`PUT /requests/{id}/task`](#put-requestsidtask)), otherwise `409 INVALID_TRANSITION`. `200` → `RequestDto`

### `POST /requests/{id}/resolve` *(Part 3)*
```json
{ "resolutionNotes": "Patched and sealed." }
```
`resolutionNotes` optional, ≤ 1000 chars. `200` → `RequestDto`

---

## 6. Part 3: Tasks

A request has at most one task: the worker currently responsible for it.

### `TaskDto`
```json
{
  "id": 9,
  "requestId": 41,
  "request": { /* RequestDto */ },
  "workerId": 12,
  "workerName": "Sam Lee",
  "assignedById": 4,
  "notes": "Bring cold patch.",
  "assignedAt": "2026-11-16T09:30:00"
}
```

### `PUT /requests/{id}/task`
Roles: `DEPARTMENT_MANAGER` of the request's department. The request must be `ASSIGNED` or `IN_PROGRESS`. Creates the task, or replaces it to reassign the request to a different worker. Does not change the request's status.

```json
{ "workerId": 12, "notes": "Bring cold patch." }
```
- `201` → `TaskDto` (created) · `200` → `TaskDto` (reassigned). The new worker gets a `TASK_ASSIGNED` notification.
- `400` (worker not in this department) · `403` · `404` · `409 INVALID_TRANSITION` (request not `ASSIGNED`/`IN_PROGRESS`)

### `GET /me/tasks`
Roles: `WORKER`. Tasks assigned to the caller, newest first (Receive Assigned Task).
- `200` → `[ TaskDto, … ]`

---

## 7. Part 3: Flags, Announcements, Notifications

### Flags (Manage flagged content)

#### `FlagDto`
```json
{
  "id": 5,
  "contentType": "REQUEST",
  "contentId": 41,
  "contentSummary": "POTHOLE – 5th St & Mill Ave",
  "reason": "Offensive language in description.",
  "flaggedById": 12,
  "flaggedByName": "Sam Lee",
  "status": "OPEN",
  "createdAt": "2026-11-16T11:00:00"
}
```

#### `POST /flags`
Who can flag depends on the content type:

| `contentType` | Allowed roles | Purpose |
|---|---|---|
| `EVENT` | any logged-in user | Report an event as inappropriate/misleading |
| `REQUEST` | `CITY_ADMINISTRATOR`, `DEPARTMENT_MANAGER`, `WORKER` | Report a request with abusive/spam content |

```json
{ "contentType": "REQUEST", "contentId": 41, "reason": "Offensive language in description." }
```
`reason` required, 1–500 chars.
- `201` → `FlagDto`
- `400` · `403` (e.g. a citizen flagging a request) · `404` (content not found) · `409 ALREADY_FLAGGED`

#### `GET /flags`
Roles: `CITY_ADMINISTRATOR`. Query `status` (default `OPEN`). `200` → `[ FlagDto, … ]`

#### `POST /flags/{id}/dismiss`
Roles: `CITY_ADMINISTRATOR`. Content stays visible; flag → `DISMISSED`. `200` → `FlagDto`

#### `POST /flags/{id}/remove`
Roles: `CITY_ADMINISTRATOR`. Content is hidden (event hidden from `/events`, request description shown as "[removed by administrator]"); flag → `REMOVED`. `200` → `FlagDto`

### Announcements

#### `AnnouncementDto`
```json
{
  "id": 2,
  "type": "EMERGENCY",
  "title": "Heat Advisory",
  "message": "Temperatures above 110°F expected. Stay hydrated.",
  "createdById": 2,
  "createdAt": "2026-11-17T07:30:00"
}
```

#### `POST /announcements`
Roles: `CITY_ADMINISTRATOR`.
```json
{ "type": "EMERGENCY", "title": "Heat Advisory", "message": "Temperatures above 110°F expected." }
```
`type` required (`AnnouncementType`), `title` 1–150 chars, `message` 1–2000 chars.
- `201` → `AnnouncementDto`. Every citizen gets a notification (`ANNOUNCEMENT` or `EMERGENCY_ALERT`).

#### `GET /announcements`
Roles: any logged-in user. Newest first. `200` → `[ AnnouncementDto, … ]`

#### `DELETE /announcements/{id}`
Roles: `CITY_ADMINISTRATOR`. Removes an outdated or mistaken announcement from `GET /announcements`. Notifications that were already delivered stay in citizens' inboxes.
- `204` · `403` · `404`

### Notifications (Observer output)

`Subject.notify()` → each `Observer.update()` → `NotificationService` inserts a row. The client loads `/me/notifications` on the home screen and has a refresh button. There is no live push.

#### `NotificationDto`
```json
{
  "id": 88,
  "type": "REQUEST_STATUS",
  "message": "Your request #41 (POTHOLE) is now IN_PROGRESS.",
  "relatedId": 41,
  "read": false,
  "createdAt": "2026-11-17T09:00:00"
}
```

| `type` | Triggered by | Recipient |
|---|---|---|
| `ANNOUNCEMENT` | `POST /announcements` (GENERAL) | all citizens |
| `EMERGENCY_ALERT` | `POST /announcements` (EMERGENCY) | all citizens |
| `REQUEST_STATUS` | any request state change | the request's citizen |
| `TASK_ASSIGNED` | `PUT /requests/{id}/task` | the (new) worker |
| `EVENT_CANCELLED` | `DELETE /events/{id}` | registered citizens |

#### `GET /me/notifications`
Roles: any logged-in user. Query `unreadOnly=true` (optional). Newest first.
- `200` → `[ NotificationDto, … ]`

#### `POST /me/notifications/{id}/read`
`204` · `404`

#### `POST /me/notifications/read-all`
`204`

---

## 8. Part 3: Reports, Transit

### `GET /reports/engagement`
Roles: `CITY_ADMINISTRATOR` (Monitor Citizen Engagement Statistics).
```json
{
  "generatedAt": "2026-11-18T14:00:00",
  "totalUsers": 42,
  "totalCitizens": 35,
  "activeUsersLast7Days": 19,
  "upcomingEvents": 11,
  "totalEventRegistrations": 128,
  "requestsByStatus": { "SUBMITTED": 4, "UNDER_REVIEW": 2, "ASSIGNED": 3, "IN_PROGRESS": 5, "RESOLVED": 20, "REJECTED": 1 },
  "requestsByType": { "STREETLIGHT": 6, "POTHOLE": 12, "GRAFFITI": 5, "WATER_LEAK": 4, "NOISE": 7, "OTHER": 1 },
  "openFlags": 2
}
```

### `GET /reports/departments/{id}`
Roles: `CITY_ADMINISTRATOR` (any department), `DEPARTMENT_MANAGER` (own department only, otherwise `404`). Computed on request, not stored (Generate Report).
```json
{
  "departmentId": 1,
  "departmentName": "Public Works",
  "generatedAt": "2026-11-18T14:00:00",
  "totalRequests": 23,
  "requestsByStatus": { "ASSIGNED": 3, "IN_PROGRESS": 5, "RESOLVED": 15 },
  "averageResolutionHours": 30.5,
  "resolvedLast30Days": 12,
  "openTasksByWorker": [ { "workerId": 12, "workerName": "Sam Lee", "openTasks": 2 } ]
}
```

### `GET /transit`
Roles: any logged-in user. Read-only seeded data (Check Public Transit Schedule). Query `route` (optional). *(Cut first if behind.)*
```json
[ { "id": 1, "route": "21A", "stop": "City Hall Plaza", "departureTime": "07:15", "arrivalTime": "07:42" } ]
```

---

## 9. Enums

| Enum | Values |
|---|---|
| `Role` | `CITIZEN`, `CITY_ADMINISTRATOR`, `DEPARTMENT_MANAGER`, `WORKER` |
| `EventCategory` | `COMMUNITY_SERVICE`, `SPORTS`, `PUBLIC_SAFETY`, `EDUCATION`, `ENVIRONMENT`, `CULTURAL` |
| `District` | `DOWNTOWN`, `NORTH`, `SOUTH`, `EAST`, `WEST` |
| `RequestType` | `STREETLIGHT`, `POTHOLE`, `GRAFFITI`, `WATER_LEAK`, `NOISE`, `OTHER` |
| `RequestStatus` | `SUBMITTED`, `UNDER_REVIEW`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REJECTED` |
| `AnnouncementType` | `GENERAL`, `EMERGENCY` |
| `NotificationType` | `ANNOUNCEMENT`, `EMERGENCY_ALERT`, `REQUEST_STATUS`, `TASK_ASSIGNED`, `EVENT_CANCELLED` |
| `FlagContentType` | `EVENT`, `REQUEST` |
| `FlagStatus` | `OPEN`, `DISMISSED`, `REMOVED` |

---

## 10. Permissions Summary

| Endpoint | Citizen | City Admin | Dept Manager | Worker |
|---|:-:|:-:|:-:|:-:|
| `/health`, `/auth/*`, `GET /me` | ✓ | ✓ | ✓ | ✓ |
| `/me/notifications/*` | ✓ | ✓ | ✓ | ✓ |
| `GET /me/registrations` | ✓ | | | |
| `GET /me/requests` | ✓ | | | |
| `GET /me/tasks` | | | | ✓ |
| `GET /events`, `GET /events/{id}` | ✓ | ✓ | ✓ | ✓ |
| `POST /events`, `PUT/DELETE /events/{id}` | | ✓ | | |
| `POST/DELETE /events/{id}/registration` | ✓ | | | |
| `POST /requests` | ✓ | | | |
| `GET /requests` | | all | own dept | |
| `GET /requests/{id}` | own | ✓ | own dept | assigned |
| review / assign / reject | | ✓ | | |
| `PUT /requests/{id}/task` | | | own dept | |
| start / resolve | | | own dept | assigned |
| `GET /departments` | | ✓ | | |
| `GET /departments/{id}/workers` | | ✓ | own dept | |
| `POST /flags` (`EVENT`) | ✓ | ✓ | ✓ | ✓ |
| `POST /flags` (`REQUEST`) | | ✓ | ✓ | ✓ |
| `GET /flags`, dismiss / remove | | ✓ | | |
| `POST /announcements`, `DELETE /announcements/{id}` | | ✓ | | |
| `GET /announcements` | ✓ | ✓ | ✓ | ✓ |
| `GET /reports/engagement` | | ✓ | | |
| `GET /reports/departments/{id}` | | any | own dept | |
| `GET /transit` | ✓ | ✓ | ✓ | ✓ |

---

## 11. Out of Scope (future work for the write-up)

- JWT tokens with expiry, refresh tokens, HTTPS
- Admin user management (staff accounts come from `db/seed.sql`)
- Password reset, email/SMS notifications
- Live push notifications (WebSockets/SSE). The client refreshes `/me/notifications` instead.
- Pagination, file/photo uploads on requests, maps
