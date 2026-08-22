# Refresh Token — Implementation Plan

## Background

Right now, `AuthController.login()` issues a single JWT access token that expires in 30 minutes.
Once it expires, the user has to log in again with their password. Refresh tokens solve this:

- The **access token** stays short-lived (30 min) for security — if it leaks, the damage window is small.
- The **refresh token** is long-lived (e.g. 7 days), stored in the database, and used only to issue new access tokens.
- When the access token expires, the client silently calls `POST /auth/refresh` with the refresh token to get a new access token — no password re-entry needed.
- On logout, the refresh token is deleted from the DB, permanently ending the session.

The `RefreshToken` entity, repository, and empty service/controller shells are already in the codebase.
This plan fills them in.

---

## Files to Create

### `dto/LoginResponseDTO.java`

Currently `login()` returns a plain `String` (just the access token). We need to return both tokens,
so create a new DTO record:

```java
public record LoginResponseDTO(String accessToken, String refreshToken) {}
```

The client will store both values. The access token goes in the `Authorization: Bearer ...` header
for every request. The refresh token is stored somewhere safe (e.g. an httpOnly cookie or secure
local storage) and only sent to `/auth/refresh`.

---

### `dto/RefreshTokenRequestDTO.java`

The request body for the refresh endpoint — just the refresh token string:

```java
public record RefreshTokenRequestDTO(String refreshToken) {}
```

---

## Files to Modify

### `application.properties`

Add a configurable expiry for refresh tokens. Since refresh tokens are stored with a `LocalDateTime expiresAt`
(not milliseconds like the JWT), a "days" value is more readable:

```properties
jwt.refresh-expiration-days=7
```

This will be `@Value`-injected into `RefreshTokenService`, the same pattern used for `jwt.expiration`
in `JwtService`.

---

### `service/RefreshTokenService.java`

This is the core of the feature. Inject `RefreshTokenRepo`, `AuthRepo`, and
`@Value("${jwt.refresh-expiration-days}") long refreshExpirationDays`.

#### Method 1 — `createRefreshToken(String username)`

Called right after a successful login. Steps:
1. Look up the `User` by username via `authRepo.findByUsername()`.
2. Generate a random opaque token string: `UUID.randomUUID().toString()`.
3. Build a `RefreshToken` entity with `expiresAt = LocalDateTime.now().plusDays(refreshExpirationDays)`.
4. Save and return it.

Unlike the JWT (which is self-contained and signed), the refresh token is just a random string.
Its validity is checked by looking it up in the database — if it's in the DB and not expired, it's valid.

#### Method 2 — `verifyRefreshToken(String tokenValue)`

Called when the client wants a new access token. Steps:
1. Call `refreshTokenRepo.findByToken(tokenValue)` — throw `ResourceNotFoundException` if not found.
2. Call `isExpired()` on the result.
   - If expired: **delete it from the DB** (`refreshTokenRepo.delete(token)`), then throw a `RuntimeException`
     with a message like `"Refresh token expired. Please log in again."` This cleans up stale tokens
     automatically and tells the client they need to re-authenticate with a password.
   - If valid: return the `RefreshToken` entity so the caller can get the associated `User`.

#### Method 3 — `deleteTokensForUser(String username)`

Called during logout. Steps:
1. Look up the `User` by username.
2. Call `refreshTokenRepo.deleteByUser(user)` (already defined in the repo).

This method needs `@Transactional` on it — `deleteByUser` is a Spring Data derived delete query,
and Spring requires an active transaction for delete operations that aren't a simple `deleteById`.

---

### `controller/AuthController.java`

Two changes here.

#### Update `login()`

Add `RefreshTokenService` to the constructor. After authentication succeeds:
1. Generate the access token as before: `jwtService.generateToken(user.getUsername())`.
2. Create a refresh token: `refreshTokenService.createRefreshToken(user.getUsername())`.
3. Return `ResponseEntity<LoginResponseDTO>` with both tokens instead of the plain `String`.

```java
// Before
if (authentication.isAuthenticated()) {
    return jwtService.generateToken(user.getUsername());
}
return "Login failed";

// After
if (authentication.isAuthenticated()) {
    String accessToken = jwtService.generateToken(user.getUsername());
    String refreshToken = refreshTokenService.createRefreshToken(user.getUsername()).getToken();
    return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken));
}
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
```

#### Implement `logout()`

Currently a placeholder. The endpoint is already behind the `JwtFilter` (authenticated), so the
current user's identity is available from the security context:

```java
@PostMapping("/logout")
public ResponseEntity<String> logout() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    refreshTokenService.deleteTokensForUser(username);
    return ResponseEntity.ok("Logged out successfully");
}
```

Important: the access token **cannot** be invalidated — it's stateless and self-verifying. It will
simply expire on its own in 30 minutes. Deleting the refresh token means the user cannot silently
renew after that window, which is the correct behaviour for logout.

---

### `controller/RefreshTokenController.java`

Add `@RestController` and `@RequestMapping("/auth")` annotations. Inject `RefreshTokenService`
and `JwtService` via constructor.

#### `POST /auth/refresh`

```java
@PostMapping("/refresh")
public ResponseEntity<LoginResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO request) {
    RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken());
    String username = refreshToken.getUser().getUsername();
    String newAccessToken = jwtService.generateToken(username);
    return ResponseEntity.ok(new LoginResponseDTO(newAccessToken, request.refreshToken()));
}
```

Steps:
1. Verify the refresh token — `verifyRefreshToken` will throw if it's missing or expired.
2. Get the username from the `RefreshToken` entity's associated `User`.
3. Generate a new access token.
4. Return it in a `LoginResponseDTO`.

**Note on token rotation (optional enhancement):** A more secure pattern is to also issue a *new*
refresh token on each call to `/refresh` and delete the old one. This is called token rotation — it
means a stolen refresh token can only be used once before it's invalidated. To add this later:
delete the old refresh token and call `createRefreshToken(username)` to get a fresh one, then
return the new refresh token alongside the new access token.

---

### `config/SecurityConfig.java`

`/auth/refresh` must be **public** (permitAll). The reason: the whole point of calling this endpoint
is that the access token has already expired. If `/auth/refresh` was protected, the `JwtFilter`
would reject the request with a 401 before it even reached the controller.

```java
// Before
.requestMatchers("/auth/register", "/auth/login")
.permitAll()

// After
.requestMatchers("/auth/register", "/auth/login", "/auth/refresh")
.permitAll()
```

`/auth/logout` does NOT need to be added here — it should remain authenticated. The user needs
a still-valid access token to call logout. This is intentional: it prevents random unauthenticated
requests from wiping refresh tokens.

---

## End-to-End Flow Summary

```
1. POST /auth/login
   → returns { accessToken, refreshToken }

2. Client uses accessToken in Authorization header for all API calls.
   Client stores refreshToken securely.

3. Access token expires (30 min).
   Client calls POST /auth/refresh with { refreshToken }
   → returns { new accessToken, same refreshToken }
   → client resumes using the API transparently

4. Refresh token expires (7 days) or is never used.
   POST /auth/refresh returns 500 / "Refresh token expired."
   → Client must redirect user to login screen.

5. POST /auth/logout (with valid access token)
   → Refresh token deleted from DB.
   → Access token expires naturally within 30 min.
   → Session fully ended.
```

---

## Summary Table

| File | Action | Key detail |
|---|---|---|
| `dto/LoginResponseDTO.java` | Create | `record(String accessToken, String refreshToken)` |
| `dto/RefreshTokenRequestDTO.java` | Create | `record(String refreshToken)` |
| `application.properties` | Edit | Add `jwt.refresh-expiration-days=7` |
| `service/RefreshTokenService.java` | Implement | 3 methods: create, verify, delete |
| `controller/AuthController.java` | Edit | Update `login()`, implement `logout()` |
| `controller/RefreshTokenController.java` | Implement | `POST /auth/refresh` endpoint |
| `config/SecurityConfig.java` | Edit | Add `/auth/refresh` to permitAll |


---

---

# Remaining Gaps — Implementation Plan

## Gap 1 — Board Member Management

### What's missing

The `Board` entity has a `members` field (`@ManyToMany Set<User>`) and the `board_members` join
table is fully set up, but there are no endpoints to add or remove members. The `members` set is
always empty unless directly manipulated in the DB.

### Plan

#### New DTO — `MemberUpdateDTO`

A simple DTO to carry the target user's ID in the request body:

```java
public record MemberUpdateDTO(Long userId) {}
```

#### New methods in `BoardService`

**`addMember(Integer boardId, Long userId)`**
1. Fetch the `Board` by `boardId` — throw `ResourceNotFoundException` if not found.
2. Fetch the `User` by `userId` from `AuthRepo` — throw `ResourceNotFoundException` if not found.
3. Check if the user is already a member: `board.getMembers().contains(user)` — throw a
   `RuntimeException("User is already a member")` to avoid silent duplicates.
4. Call `board.getMembers().add(user)`, then `boardRepo.save(board)`.
5. Return the updated `BoardResponseDTO`.

**`removeMember(Integer boardId, Long userId)`**
1. Fetch the `Board` and `User` the same way.
2. Check if the user IS a member — throw if not, to avoid a silent no-op.
3. Call `board.getMembers().remove(user)`, then `boardRepo.save(board)`.
4. Return the updated `BoardResponseDTO`.

#### New endpoints in `BoardController`

```java
// Add a member
PATCH /boards/{boardId}/members/add
@RequestBody MemberUpdateDTO

// Remove a member
PATCH /boards/{boardId}/members/remove
@RequestBody MemberUpdateDTO
```

Using `PATCH` is appropriate here — you're partially modifying the board's member set, not replacing
the whole resource. Both endpoints delegate to their respective service methods and return
`ResponseEntity<BoardResponseDTO>`.

---

## Gap 2 — Ownership / Authorization Checks

### What's missing

Any authenticated user can update or delete any board or task, even ones they don't own. For
example, user A can call `DELETE /boards/5` even if user B created board 5. The only thing
currently protected is resource existence (404 if not found) — not ownership (403 if not yours).

### Plan

The current user's username is always available from `SecurityContextHolder`. The check pattern is
the same everywhere: fetch the resource, compare its owner to the current user, throw if they
don't match.

#### Helper method (add to each service, or a shared utility)

```java
private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
}
```

#### In `BoardService`

Add ownership checks to `updateBoard()` and `deleteBoard()`:

```java
// After fetching the board:
String currentUsername = getCurrentUsername();
if (!board.getOwner().getUsername().equals(currentUsername)) {
    throw new RuntimeException("You are not the owner of this board");
}
```

`GlobalExceptionHandler` will catch `RuntimeException` and return HTTP 500. For a cleaner API you
could create a dedicated `ForbiddenException` that maps to HTTP 403, but that's optional for a
practice project.

#### In `TaskService` (optional, lower priority)

Task ownership is less clear since tasks don't have a direct owner — they have an optional `assignee`
and belong to a board. The appropriate check here would be: verify the requesting user is the owner
of the board the task belongs to before allowing mutations. This requires an extra traversal:
`task → taskList → board → owner`.

```java
String currentUsername = getCurrentUsername();
String boardOwner = task.getTaskList().getBoard().getOwner().getUsername();
if (!boardOwner.equals(currentUsername)) {
    throw new RuntimeException("You are not the owner of this board");
}
```

Add this check to `updateTask()` and `deleteTask()`.

---

## Gap 3 — `boardId` / `listId` Cross-Validation in `TaskService`

### What's missing

`TaskService` already checks that the board exists and the list exists independently, but it never
verifies they are actually related to each other or to the task. This means:

- `GET /boards/1/lists/99/tasks/5` succeeds even if task 5 belongs to list 2 on board 1.
- `GET /boards/99/lists/1/tasks/5` succeeds even if board 99 doesn't own list 1.

The URL implies a hierarchy (`board → list → task`) but the lookups don't enforce it.

### Plan

Two assertions to add in `TaskService`, after fetching both the list and the task:

**Assert the list belongs to the board:**

```java
if (!taskList.getBoard().getBoardId().equals(boardId)) {
    throw new ResourceNotFoundException("List does not belong to this board");
}
```

**Assert the task belongs to the list:**

```java
if (!task.getTaskList().getListId().equals(listId)) {
    throw new ResourceNotFoundException("Task does not belong to this list");
}
```

Add both checks to every method that receives all three path variables: `getTask()`, `updateTask()`,
`deleteTask()`, and `moveTask()`. The `getTasks()` and `createTask()` methods don't need the second
check (they operate on the list, not a specific task).

Currently the list is fetched but not stored in a variable in some methods — change
`listRepo.findById(listId).orElseThrow(...)` from a fire-and-forget call to an assigned variable
so the result can be used in the assertion:

```java
// Before (fire and forget — result discarded)
listRepo.findById(listId).orElseThrow(() -> new ResourceNotFoundException("List not found"));

// After (assign so we can use it)
TaskList taskList = listRepo.findById(listId)
        .orElseThrow(() -> new ResourceNotFoundException("List not found"));
// then assert:
if (!taskList.getBoard().getBoardId().equals(boardId)) { ... }
```

---

## Gap 4 — `authenticationManager` Field Visibility in `AuthController`

### What's missing

In `AuthController`, the `authenticationManager` field is declared as:

```java
final AuthenticationManager authenticationManager;
```

It's missing the `private` modifier. Every other field in the class is `private final`. This is
a minor inconsistency but breaks encapsulation — the field is package-visible, meaning any class
in the same package can access it directly.

### Plan

`AuthController.java` — add `private` to the field declaration:

```java
// Before
final AuthenticationManager authenticationManager;

// After
private final AuthenticationManager authenticationManager;
```

No other changes needed.

---

## Remaining Gaps Summary Table

| # | Gap | Files to touch |
|---|---|---|
| 1 | Board member add/remove | `dto/MemberUpdateDTO.java` (create), `BoardService.java`, `BoardController.java` |
| 2 | Ownership checks on mutations | `BoardService.java`, `TaskService.java` |
| 3 | `boardId`/`listId` cross-validation | `TaskService.java` |
| 4 | `authenticationManager` not `private` | `AuthController.java` |

claude --resume d74b6acf-3eeb-4633-b4ca-696f3bcc0b66