# Security Architecture - How Everything Connects

This document explains how every file, class, and function in the security system
fits together. Think of it as a map of the authentication flow.

---

## The Big Picture

There are **5 layers** that work together:

```
1. Configuration   → SecurityConfig.kt          (wires everything up)
2. JWT Layer        → JwtUtil.kt + JwtAuthFilter.kt (create & validate tokens)
3. User Lookup      → CustomUserDetailsService.kt   (find user in DB)
4. Auth Endpoints   → AuthController.kt              (login & register APIs)
5. Protected APIs   → UserController.kt, ProductController.kt (consume the auth)
```

---

## Layer 1: The Wiring - SecurityConfig.kt

**File:** `common/security/SecurityConfig.kt`

This is the **brain** of Spring Security. It tells Spring:
- Which endpoints are public vs protected
- How passwords are encoded
- Which filter to run on every request
- How to handle unauthenticated access

```
SecurityConfig
├── @Bean passwordEncoder()          → BCryptPasswordEncoder (hashes passwords)
├── @Bean authenticationManager()    → lets AuthController authenticate users
└── @Bean securityFilterChain()      → THE RULES:
    ├── CSRF disabled (no cookies, it's a REST API)
    ├── Sessions = STATELESS (JWT replaces server-side sessions)
    ├── /api/auth/** = permitAll (login & register are public)
    ├── everything else = authenticated
    ├── CustomAuthenticationEntryPoint (JSON 401 instead of default 403)
    └── JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter
```

**Why this matters:** Without this file, Spring Security uses defaults (form login,
all endpoints protected, CSRF enabled). This file is what makes it a JWT-based API.

---

## Layer 2: JWT Token System

### JwtUtil.kt - The Token Factory

**File:** `common/security/JwtUtil.kt`

Creates and validates JWT tokens. Has nothing to do with HTTP or Spring Security -
it's a pure utility.

```
JwtUtil
├── reads from application.properties:
│   ├── jwt.secret = "9a4f2c8d..." (Base64-encoded HMAC key)
│   └── jwt.expiration-ms = 86400000 (24 hours)
├── generateToken(email) → creates JWT with: sub=email, iat=now, exp=24h
├── extractEmail(token)  → parses token, returns the email from "sub" claim
└── validateToken(token, email) → checks: email matches AND not expired
```

**Key insight:** The JWT only stores the email as its subject. No user ID, no roles.
It's the simplest possible token.

### JwtAuthFilter.kt - The Gatekeeper

**File:** `common/security/JwtAuthFilter.kt`

This is a **servlet filter** that runs on **every single HTTP request**. It's
registered in SecurityConfig to run before Spring's own authentication filter.

```
Every incoming request hits JwtAuthFilter.doFilterInternal():
│
├── Read "Authorization" header
│   ├── Header missing or doesn't start with "Bearer " → pass through (unauthenticated)
│   └── Has "Bearer <token>":
│       │
│       ├── 1. jwtUtil.extractEmail(token)  → get email from JWT
│       │
│       ├── 2. SecurityContext already has auth? → skip if yes
│       │
│       ├── 3. userDetailsService.loadUserByUsername(email) → load from DB
│       │
│       ├── 4. jwtUtil.validateToken(token, email) → check expiry & match
│       │
│       ├── 5. If valid → create UsernamePasswordAuthenticationToken
│       │                 → store in SecurityContextHolder
│       │                 (NOW the request is "authenticated" for the rest of the chain)
│       │
│       └── 6. If invalid → log warning, continue (unauthenticated)
│
└── Always: filterChain.doFilter(request, response) → continue to next filter/controller
```

**Why it matters:** This is the bridge between "raw HTTP request" and "authenticated
request". Without it, every request would be anonymous.

---

## Layer 3: User Lookup - CustomUserDetailsService.kt

**File:** `common/security/CustomUserDetailsService.kt`

Spring Security needs a way to load users from your database. This class implements
Spring's `UserDetailsService` interface to do that.

```
CustomUserDetailsService
├── implements UserDetailsService
└── loadUserByUsername(email):
        ├── calls userRepository.findByEmail(email)  ← queries MySQL
        ├── if not found → throws UsernameNotFoundException
        └── if found → returns Spring Security's User object:
                ├── username = user.email
                ├── password = user.password (BCrypt hash from DB)
                └── authorities = emptyList() (no roles yet)
```

**Important quirk:** The method is called `loadUserByUsername` but you're passing
email. That's because Spring's interface uses "username" as a generic concept.
In your app, the "username" IS the email.

**Who calls this?** Two places:
1. `JwtAuthFilter` - to load user details when validating a JWT on each request
2. `AuthenticationManager` - during login to verify credentials

---

## Layer 4: Auth Endpoints - AuthController.kt

**File:** `features/auth/AuthController.kt`

This is the API that clients call to register and login.

### Registration Flow (POST /api/auth/register)

```
Client sends: { "name": "John", "email": "john@example.com", "password": "secret123" }
                    │
                    ▼
RegisterRequest DTO (Jakarta validation: name 3-50 chars, valid email, password 6+)
                    │
                    ▼
AuthController.register():
    1. userRepository.findByEmail(email) → check if user exists
       └── If exists → throw IllegalStateException → GlobalExceptionHandler → 409 Conflict
    2. passwordEncoder.encode(password) → "$2a$10$N9qo8uLOickgx2ZMRZoMy..."
       └── BCrypt automatically salts + hashes (2^10 rounds)
    3. userRepository.save(UserDto(name, email, encodedPassword))
       └── Inserts into MySQL "users" table via Exposed ORM
    4. jwtUtil.generateToken(email) → "eyJhbGciOiJIUzI1..."
    5. Return: 201 Created + { "token": "eyJhbGci..." }
```

### Login Flow (POST /api/auth/login)

```
Client sends: { "email": "john@example.com", "password": "secret123" }
                    │
                    ▼
LoginRequest DTO (Jakarta validation: valid email, password not blank)
                    │
                    ▼
AuthController.login():
    1. authenticationManager.authenticate(
           UsernamePasswordAuthenticationToken(email, password)
       )
       │
       │  Internally, AuthenticationManager:
       │  ├── Calls CustomUserDetailsService.loadUserByUsername(email)
       │  │   └── Returns User with email as username + BCrypt hash as password
       │  ├── BCryptPasswordEncoder.matches(rawPassword, hash) → true/false
       │  ├── If mismatch → throw BadCredentialsException → GlobalExceptionHandler → 401
       │  └── If match → returns authenticated Authentication object
       │
    2. jwtUtil.generateToken(email) → "eyJhbGciOiJIUzI1..."
    3. Return: 200 OK + { "token": "eyJhbGci..." }
```

**Key difference from registration:** Login uses `AuthenticationManager` (which internally
uses `CustomUserDetailsService` + `PasswordEncoder`) while registration does the
encoding and saving directly.

---

## Layer 5: Protected Endpoints - Consuming the Auth

### UserController.kt - GET /api/users/me

**File:** `features/users/UserController.kt`

```
Client sends: GET /api/users/me
              Authorization: Bearer eyJhbGciOiJIUzI1...
                    │
                    ▼
JwtAuthFilter runs first:
    ├── Extracts email from JWT
    ├── Loads UserDetails from DB
    ├── Validates token
    └── Sets Authentication in SecurityContext
                    │
                    ▼
SecurityConfig checks: /api/users/** requires authentication ✓
    └── Authentication exists in SecurityContext → allowed through
                    │
                    ▼
UserController.getCurrentUser(authentication):
    └── authentication.name = email (set by JwtAuthFilter)
        └── userService.getUserByEmail(email) → UserResponse (no password!)
            └── Return: 200 OK + { "id": 1, "name": "John", "email": "john@example.com" }
```

---

## The Database Layer (UserRepository + UserTable)

### UserTable.kt
```
MySQL "users" table:
┌────┬────────┬──────────────────┬──────────────────────────────────────┐
│ id │  name  │      email       │              password                │
├────┼────────┼──────────────────┼──────────────────────────────────────┤
│  1 │  John  │ john@example.com │ $2a$10$N9qo8uLOickgx2ZMRZoMy...     │
└────┴────────┴──────────────────┴──────────────────────────────────────┘
```

### UserRepository.kt
```
├── findByEmail(email) → queries WHERE email = ?, returns UserDto? (includes password)
└── save(user)         → INSERTs new row, returns UserDto with generated ID
```

**Why findByEmail includes password:** It's used by CustomUserDetailsService to
compare against the provided password during login. But it's NOT exposed in API
responses - UserResponse DTO deliberately omits the password field.

---

## Error Handling - GlobalExceptionHandler.kt

**File:** `common/api/GlobalExceptionHandler.kt`

Catches security-related exceptions and returns clean JSON:

```
BadCredentialsException     → 401 "Invalid email or password"
UsernameNotFoundException   → 401 "Invalid email or password"  (same! prevents user enumeration)
AccessDeniedException       → 403 "Access denied"
IllegalStateException       → 409 "User already exists"  (duplicate registration)
MethodArgumentNotValidException → 400 + field-level errors  (validation failures)
```

**Security detail:** Both "user not found" and "wrong password" return the same
401 message. This prevents attackers from guessing which emails are registered.

---

## The Complete Request Lifecycle

Here's what happens end-to-end for a typical authenticated request:

```
1. Client: POST /api/auth/register  { name, email, password }
   │
2. SecurityConfig: /api/auth/** is permitAll → no auth needed
   │
3. AuthController.register():
   ├── Validate input (Jakarta)
   ├── Check email not taken
   ├── BCrypt hash password
   ├── Save to MySQL
   ├── Generate JWT
   └── Return { token: "eyJ..." }
   │
4. Client: GET /api/users/me  +  Authorization: Bearer eyJ...
   │
5. JwtAuthFilter.doFilterInternal():
   ├── Extract "Bearer eyJ..." → token = "eyJ..."
   ├── jwtUtil.extractEmail(token) → "john@example.com"
   ├── userDetailsService.loadUserByUsername("john@example.com") → UserDetails
   ├── jwtUtil.validateToken(token, "john@example.com") → true
   ├── Create UsernamePasswordAuthenticationToken
   └── Set in SecurityContextHolder
   │
6. SecurityConfig: /api/users/** requires authentication ✓ (token is valid)
   │
7. UserController.getCurrentUser(authentication):
   ├── authentication.name → "john@example.com"
   ├── userService.getUserByEmail("john@example.com")
   │   └── userRepository.findByEmail() → UserDto
   │       └── Convert to UserResponse (strip password)
   └── Return { id: 1, name: "John", email: "john@example.com" }
```

---

## File Reference Map

| File | Role | Who calls it |
|------|------|-------------|
| `SecurityConfig.kt` | Wires all security pieces together | Spring Boot (auto-config) |
| `JwtUtil.kt` | Create/validate JWT tokens | JwtAuthFilter, AuthController |
| `JwtAuthFilter.kt` | Extract + validate JWT on every request | Spring Security filter chain |
| `CustomUserDetailsService.kt` | Load user from DB for Spring Security | JwtAuthFilter, AuthenticationManager |
| `CustomAuthenticationEntryPoint.kt` | Return JSON 401 for unauthenticated access | Spring Security filter chain |
| `AuthController.kt` | Login & Register endpoints | HTTP clients |
| `LoginRequest.kt` | Login request body DTO | AuthController |
| `RegisterRequest.kt` | Register request body DTO | AuthController |
| `AuthResponse.kt` | Token response DTO | AuthController |
| `UserTable.kt` | Database table definition | UserRepository |
| `UserRepository.kt` | Database queries for users | CustomUserDetailsService, AuthController, UserService |
| `UserService.kt` | Business logic for user operations | UserController |
| `UserController.kt` | /api/users/me endpoint | HTTP clients |
| `UserDto.kt` | Internal user data (includes password) | UserRepository, AuthController |
| `UserResponse.kt` | API response user data (no password) | UserService |
| `GlobalExceptionHandler.kt` | Catch & format security exceptions | Spring (auto-detected via @RestControllerAdvice) |
| `ErrorResponse.kt` | Standard error response format | GlobalExceptionHandler, CustomAuthenticationEntryPoint |
