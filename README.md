# Nexus Chat

A real-time chat application built with Spring Boot 3.5, Thymeleaf, WebSocket (STOMP), and Spring Security. Features multi-room messaging, live presence indicators, typing notifications, and a dark terminal-style UI.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.11-6db33f?style=flat-square)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.5-6db33f?style=flat-square)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blue?style=flat-square)
![H2](https://img.shields.io/badge/Database-H2_In--Memory-lightblue?style=flat-square)

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Default Credentials](#default-credentials)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [API & Routes](#api--routes)
- [WebSocket Protocol](#websocket-protocol)
- [Database Schema](#database-schema)
- [Security](#security)

---

## Features

- **Real-time messaging** — messages delivered instantly via WebSocket (STOMP over SockJS)
- **Multi-room chat** — create, join, and leave chat rooms
- **Live presence** — see who is online/offline in real time
- **Typing indicators** — shows when another user is typing
- **Message history** — previous messages loaded on room entry
- **Own vs others layout** — your messages appear on the right, others on the left
- **User registration & login** — session-based authentication with BCrypt password hashing
- **Random avatar colors** — each user gets a unique color assigned at registration
- **Seeded demo data** — admin user and 3 default rooms created on startup
- **H2 console** — in-browser database inspector at `/h2-console`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| Web / MVC | Spring MVC + Thymeleaf |
| Real-time | Spring WebSocket, STOMP, SockJS |
| Security | Spring Security 6.5 (session-based form login) |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (in-memory) |
| Templating | Thymeleaf + thymeleaf-extras-springsecurity6 |
| Build | Gradle (Groovy DSL) |
| Utilities | Lombok |
| Frontend | Vanilla JS, CSS custom properties |

---

## Project Structure

```
src/
└── main/
    ├── java/com/example/demo/
    │   ├── DemoApplication.java              # Entry point
    │   ├── config/
    │   │   ├── DataInitializer.java          # Seeds admin user + 3 default rooms
    │   │   ├── PasswordEncoderConfig.java    # BCrypt bean (isolated to break circular dep)
    │   │   ├── SecurityConfig.java           # HTTP security, form login, CSRF rules
    │   │   └── WebSocketConfig.java          # STOMP broker, SockJS endpoint, WS security
    │   ├── controller/
    │   │   ├── AuthController.java           # GET/POST /auth/login, /auth/register
    │   │   ├── ChatMvcController.java        # Room listing, room page, create/leave room
    │   │   └── WebSocketController.java      # STOMP message handlers (send, typing, presence)
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── ChatRoom.java
    │   │   └── Message.java
    │   ├── enums/
    │   │   ├── MessageType.java              # TEXT | JOIN | LEAVE | SYSTEM
    │   │   └── UserStatus.java              # ONLINE | OFFLINE | AWAY
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── ChatRoomRepository.java
    │   │   └── MessageRepository.java
    │   └── service/
    │       ├── chat/ChatService.java         # Room and message business logic
    │       └── user/UserService.java         # User registration, auth, status
    └── resources/
        ├── application.yaml
        ├── static/
        │   ├── css/main.css                  # All styles including own/other message layout
        │   └── js/chat.js                    # WebSocket client (STOMP + SockJS)
        └── templates/
            ├── auth/
            │   ├── login.html
            │   └── register.html
            └── chat/
                ├── home.html                 # Room grid + create room form
                ├── room.html                 # Chat room with live messaging
                └── members.html             # All users with online status
```

---

## Getting Started

### Prerequisites

- Java 17+
- Git

### Run

```bash
# Clone the repository
git clone <your-repo-url>
cd demo

# Run with Gradle wrapper
./gradlew bootRun
```

Then open your browser at **http://localhost:8080**

On first startup the app automatically seeds:
- A default `admin` account
- Three chat rooms: `#general`, `#random`, `#tech-talk`

---

## Default Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Default user |

You can register additional accounts at `/auth/register`.

---

## Configuration

All configuration lives in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8080                          # Change port here

spring:
  datasource:
    url: jdbc:h2:mem:nexusmvc         # In-memory DB, wiped on restart
  h2:
    console:
      enabled: true
      path: /h2-console               # Database browser UI
  jpa:
    hibernate:
      ddl-auto: create-drop           # Schema recreated on every start
  servlet:
    session:
      timeout: 1d                     # Login session duration
```

### Switching to a persistent database (e.g. PostgreSQL)

1. Add the driver to `build.gradle`:
```groovy
runtimeOnly 'org.postgresql:postgresql'
```

2. Update `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nexus
    username: your_user
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

---

## Architecture

```
Browser
  │
  ├── HTTP (Thymeleaf SSR)
  │     └── Spring MVC Controllers
  │           ├── AuthController       → login / register pages
  │           └── ChatMvcController    → room list, room view, leave
  │
  └── WebSocket (STOMP over SockJS)
        └── WebSocketController
              ├── /app/chat/{roomId}/send    → save + broadcast message
              ├── /app/typing/{roomId}       → broadcast typing event
              └── /app/presence             → update + broadcast online status

Services
  ├── UserService    → registration, UserDetailsService, status updates
  └── ChatService    → room CRUD, message persistence

Persistence (JPA / H2)
  ├── users
  ├── chat_rooms
  ├── room_members   (join table)
  └── messages
```

### Circular Dependency Solution

`UserService` needs `PasswordEncoder`, and `SecurityConfig` needs both — which creates a circular dependency at startup. This is solved by:

1. **`PasswordEncoderConfig`** — a standalone `@Configuration` class that declares only the `PasswordEncoder` bean with no other dependencies.
2. **`@Lazy` on `UserService`** in `SecurityConfig`'s constructor — Spring injects a proxy at startup and resolves the real bean only on first use (at login time).

---

## API & Routes

### HTTP Routes

| Method | Path | Description | Auth |
|---|---|---|---|
| `GET` | `/` | Redirect to `/chat` | ✅ |
| `GET` | `/auth/login` | Login page | ❌ |
| `POST` | `/auth/login` | Submit login form | ❌ |
| `GET` | `/auth/register` | Registration page | ❌ |
| `POST` | `/auth/register` | Submit registration | ❌ |
| `POST` | `/auth/logout` | Log out | ✅ |
| `GET` | `/chat` | Room list home page | ✅ |
| `POST` | `/chat/rooms` | Create a new room | ✅ |
| `GET` | `/chat/rooms/{id}` | Enter a room (auto-joins) | ✅ |
| `POST` | `/chat/rooms/{id}/leave` | Leave a room | ✅ |
| `GET` | `/chat/members` | View all members | ✅ |
| `GET` | `/h2-console/**` | H2 database console | ❌ |

### Form Validation Rules

**Register:**
- `username` — required, 3–50 characters
- `password` — required, minimum 6 characters
- `email` — required, valid email format

**Create Room:**
- `name` — required, 2–80 characters
- `description` — optional, max 255 characters

---

## WebSocket Protocol

### Connection

```javascript
const socket = new SockJS('/ws');
const client = new StompJs.Client({ webSocketFactory: () => socket });
client.activate();
```

### Client → Server (publish destinations)

| Destination | Payload | Description |
|---|---|---|
| `/app/chat/{roomId}/send` | `{ "content": "hello" }` | Send a message |
| `/app/typing/{roomId}` | `{ "typing": true }` | Typing indicator |
| `/app/presence` | `{ "status": "ONLINE" }` | Update presence |

### Server → Client (subscribe topics)

| Topic | Payload | Description |
|---|---|---|
| `/topic/room/{roomId}` | Message object | Incoming room message |
| `/topic/room/{roomId}/typing` | `{ username, typing }` | Typing event |
| `/topic/presence` | `{ username, status }` | Presence update |

### Message Payload (broadcast)

```json
{
  "id": 42,
  "content": "hello world",
  "sentAt": "2026-03-13T19:22:00",
  "senderName": "shaker",
  "senderUsername": "shaker",
  "senderColor": "#4ECDC4"
}
```

---

## Database Schema

### `users`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-generated |
| `username` | VARCHAR(50) | Unique |
| `password` | VARCHAR | BCrypt hashed |
| `email` | VARCHAR(100) | Unique |
| `display_name` | VARCHAR(80) | Defaults to username |
| `avatar_color` | VARCHAR(20) | Random hex color |
| `status` | ENUM | `ONLINE / OFFLINE / AWAY` |
| `created_at` | TIMESTAMP | Set on creation |
| `last_seen` | TIMESTAMP | Updated on status change |

### `chat_rooms`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-generated |
| `name` | VARCHAR(80) | Room name |
| `description` | VARCHAR(255) | Optional |
| `created_by` | FK → users | Room creator |
| `created_at` | TIMESTAMP | |

### `room_members` (join table)
| Column | Type |
|---|---|
| `room_id` | FK → chat_rooms |
| `user_id` | FK → users |

### `messages`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | Auto-generated |
| `content` | TEXT | Message body |
| `sender_id` | FK → users | |
| `room_id` | FK → chat_rooms | |
| `type` | ENUM | `TEXT / JOIN / LEAVE / SYSTEM` |
| `sent_at` | TIMESTAMP | |
| `deleted` | BOOLEAN | Soft delete flag |

---

## Security

- **Authentication** — Spring Security form login with HTTP session (`JSESSIONID` cookie)
- **Passwords** — BCrypt hashed via `PasswordEncoderConfig`
- **CSRF** — enabled for all HTTP forms; disabled only for `/ws/**` and `/h2-console/**`
- **WebSocket auth** — STOMP connections inherit the authenticated HTTP session; `@EnableWebSocketSecurity` with explicit `AuthorizationManager` required for Spring Security 6.5+
- **Permitted paths** — `/auth/**`, `/css/**`, `/js/**`, `/ws/**`, `/h2-console/**` are open; everything else requires login
- **Session timeout** — 1 day (configurable in `application.yaml`)

---

## Development Notes

- **H2 is in-memory** — all data is lost on restart. This is intentional for development. Switch to PostgreSQL for persistence (see [Configuration](#configuration)).
- **Thymeleaf cache is disabled** — template changes reflect immediately without restart during development.
- **SockJS CDN** — the frontend loads SockJS and STOMP from jsDelivr CDN. For offline/production use, bundle them locally.
- **Spring Boot 3.5 + Spring Security 6.5** — requires `@EnableWebSocketSecurity` in `WebSocketConfig` with an explicit `AuthorizationManager<Message<?>>` bean. Without this,STOMP messages are blocked even for authenticated users.