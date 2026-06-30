# 67-quiz — Feature Inventory

A complete catalogue of every feature in the codebase as of this writing, organized by domain. Each section names the concrete files, endpoints, validation rules, and design decisions involved so this doc doubles as a map of the code.

> **Stack at a glance:** Spring Boot 4.0.6 (Java 25) + MariaDB + Liquibase + JDBC sessions on the backend; Vue 3.5 + Vite + Pinia + TanStack Query + Orval-generated client on the frontend; Playwright e2e suite (~80 specs) covering the full stack. Feature-package layout under `backend/src/main/java/dev/six_seven_quiz/`: `authentication`, `authorization`, `user`, `quiz`, `search`, `notification`, `shared`.

## Table of contents

1. [Authentication & sessions](#1-authentication--sessions)
2. [Authorization](#2-authorization)
3. [User accounts & profiles](#3-user-accounts--profiles)
4. [Avatars](#4-avatars)
5. [Quiz authoring](#5-quiz-authoring)
6. [Quiz taking (attempts)](#6-quiz-taking-attempts)
7. [Quiz ratings](#7-quiz-ratings)
8. [Leaderboards](#8-leaderboards)
9. [Profile comments](#9-profile-comments)
10. [Notifications](#10-notifications)
11. [Quiz images (cover / question / option)](#11-quiz-images-cover--question--option)
12. [Search & discovery](#12-search--discovery)
13. [Dashboard](#13-dashboard)
14. [Navigation & app shell](#14-navigation--app-shell)
15. [Branding & shared UI primitives](#15-branding--shared-ui-primitives)
16. [Error handling pipeline](#16-error-handling-pipeline)
17. [Infrastructure, build, deploy](#17-infrastructure-build-deploy)
18. [Testing](#18-testing)

---

## 1. Authentication & sessions

Three endpoints under `/api/authentication`, all under `authentication/`:

- **Register** — `POST /authentication/register` (`AuthenticationController` → `RegistrationService`). Validates username (5–16 chars, regex `^[a-zA-Z][a-zA-Z0-9_]+$`), password (≥8 chars), checks uniqueness, hashes with `BCryptPasswordEncoder`, then auto-logs in the new user. Domain exceptions: `DuplicateUsernameException`, `InvalidUsernameException`, `UsernameTooShortException`, `UsernameTooLongException`, `PasswordTooShortException`.
- **Login** — `POST /authentication/login` (`LogInService.loginUser`). Authenticates via Spring Security's `AuthenticationManager`, builds a `SecurityContext`, writes it to the JDBC session under `HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY`. Wrong password → `UNAUTHORIZED` (401).
- **Logout** — `POST /authentication/logout` (Spring default), clears the session.

Plus `GET /authentication/me` (`AuthenticationStateController`) for the FE's anonymous-probe pattern.

**Session storage** is Spring Session JDBC with `initialize-schema: never` (`application.yaml`) — the schema lives in `db/changelog/000-security.yaml`, not the Spring Session bootstrapper. Session cookies are HTTP-only, `same-site=lax`, `secure` defaulting to false so plain-HTTP prod containers work out of the box (overridable via `SERVER_SESSION_COOKIE_SECURE`).

**Security configuration** (`shared/configuration/SecurityConfiguration.java`):
- CSRF disabled globally.
- CORS allows `http://localhost:5173`, `http://192.168.*.*:5173`, `https://output.center`; all methods + headers; credentials allowed.
- Public endpoints: `/`, `/index.html`, `/assets/**`, `/favicon.ico`, OpenAPI docs, Swagger UI, `/authentication/**`.
- Everything else requires `authenticated()`.

**Frontend** (`stores/auth.ts`, `router/index.ts`):
- `useAuthStore` tracks `status: 'unknown' | 'authenticated' | 'anonymous'`, plus `username`, `displayName`, `hasAvatar`, `avatarVersion` (bumped after upload so the cached `<img>` refetches).
- Router guard waits for the first auth probe before resolving any route; routes marked `meta.anonymous` redirect authenticated users away, `meta.landing` does the same, the rest redirect to `/login?next=…` if anonymous.
- Login/register forms surface field-level errors via the `Input.error` prop, populated from `validationFieldErrors(e)` in `lib/errors.ts`.

## 2. Authorization

- `Role` entity (`authorization/Role.java`) joined to users via `user_roles` (many-to-many, eager-fetched on `ApplicationUser`).
- `SecurityConfiguration.userDetailsService` translates roles to `SimpleGrantedAuthority("ROLE_" + name)`.
- `@EnableMethodSecurity(jsr250Enabled = true)` is on, but in practice **all authorization is owner-based, not role-based**: services explicitly check `quiz.getAuthor().equals(user)` (`QuizValidator.requireOwner`) and similar for attempts, ratings, comments. There's no `@RolesAllowed` in production code.
- The Failure/`NO_ACCESS_TO_QUIZ` / `NO_ACCESS_TO_ATTEMPT` / `NO_ACCESS_TO_COMMENT` paths return 403 with structured errors so the FE can map them to specific copy.

## 3. User accounts & profiles

**`ApplicationUser`** (`user/ApplicationUser.java`):
- PK: UUID. Fields: `username` (unique, immutable), `password` (bcrypt hash), `displayName` (defaults to username at register), `bio` (≤280 chars), `avatarPath` (relative path, nullable), `roles` (eager many-to-many).
- JPA `JOINED` inheritance strategy for future extensibility.

**Endpoints under `/api/users/`** (`user/profile/controller/UserProfileController.java`):
- `GET /users/me` — own profile + `canEdit=true`. Returns a `UserProfileDto` with computed stats: `quizzesAuthored` (count by author), `attemptsTaken` (count of finished attempts), `averageScorePercent` (looped in Java by `UserProfileService.computeAverageScorePercent`).
- `GET /users/{username}` — public profile, `canEdit=true` iff the caller is that user.
- `PATCH /users/me` (`UpdateProfileRequest`) — updates `displayName` (trimmed, 1–32 chars) and/or `bio` (max 280). Bad length → `INVALID_DISPLAY_NAME` / `INVALID_BIO`.
- `GET /users/{username}/quizzes?page=N` — paginated `QuizSummaryDto` of that user's authored quizzes.

**Frontend** (`views/ProfileView.vue`, `views/UserProfileView.vue`):
- Editable on `/app/profile`: avatar tile, display name + bio modal (with inline `INVALID_DISPLAY_NAME` and `INVALID_BIO` field errors).
- Three stats cards. Two of them (Quizzes authored, Attempts taken) are actionable buttons with right-chevron affordances — vertically centered, hover-shift the arrow. The third (Average score) is passive.
- Public view at `/app/users/:username` shows the same stats but no edit affordances.
- Both pages mount the shared `ProfileComments` component (see §9).

## 4. Avatars

Distinct from the broader quiz-image pipeline because it predates it.

**Backend** (`user/profile/service/AvatarStorageService.java`):
- Accepts `image/png`, `image/jpeg`, `image/jpg`, `image/webp` (MIME whitelist).
- Size cap **2 MB** (also enforced by Spring's multipart filter for defence in depth).
- Decodes via `javax.imageio.ImageIO.read` — **WebP** decoding comes from the `com.twelvemonkeys.imageio:imageio-webp` plugin (`backend/build.gradle.kts`); stock JDK only ships PNG/JPEG/GIF/BMP.
- Crops to a centred square, scales to 256×256, re-encodes as JPEG.
- Atomic write: temp file with UUID suffix → `Files.move(..., REPLACE_EXISTING)`. A crash mid-write can't leave a corrupt avatar on disk.
- Stored at `{APP_UPLOADS_DIR}/avatars/{userId}.jpg`; entity stores the relative path `avatars/{userId}.jpg`.

**Endpoints**:
- `PUT /users/me/avatar` (multipart `file=`) — owner-only.
- `DELETE /users/me/avatar` — best-effort filesystem cleanup; the row is the source of truth.
- `GET /users/{username}/avatar` — public, `produces=image/jpeg`, `Cache-Control: max-age=300`.

**Frontend** (`views/ProfileView.vue`):
- File picker accepts `image/png,image/jpeg,image/webp`.
- Optimistic preview while upload round-trips; on success, `useAuthStore.bumpAvatarVersion()` increments a counter the topbar `<img>` uses as a query string so the browser refetches.
- Errors: `INVALID_IMAGE` ("That image couldn't be read..."), `AVATAR_TOO_LARGE` (resolver interpolates `details.maxBytes` into "That image is over 2 MB.").

## 5. Quiz authoring

**Domain model** (`quiz/model/`):
- **`Quiz`** — `id` UUID, `name`, `author` (ManyToOne), `duration` (Duration, e.g. `PT5M`), `coverImagePath`, `questions` (eager OneToMany via junction `quiz_questions(quiz_id, question_id, position)` ordered by `@OrderColumn`).
- **`Question`** — `id`, `text`, `type` enum, `imagePath`, eager OneToMany `options` with cascade + orphan removal.
- **`QuestionType`** — `SINGLE_CHOICE` or `MULTI_CHOICE`. Drives scoring + the UI marker (radio vs checkbox).
- **`Option`** — `id`, `text`, `correct` Boolean, `imagePath`.

**Shape validation** lives on the model itself in `Quiz.validateQuestionShape`:
- Question type required.
- ≥2 options per question (`InvalidQuestionShapeException`).
- No blank option text (`BlankIndexedOptionException(index)` — the index lets the FE point at the offending field).
- `SINGLE_CHOICE` must have exactly 1 correct option.

**DTO split** is per-actor (see CLAUDE.md):
- `quiz/dto/response/authoring/` — `QuizDto`, `QuestionDto`, `OptionDto` (with `hasImage` flags and `correct` exposed; for the author).
- `quiz/dto/response/viewing/` — `QuizSummaryDto` (with `hasCover`, `ratingSummary`, embedded `AuthorSummaryDto`, the `youAreAuthor` flag), `FinishedQuestionDto`, `FinishedOptionDto` (the latter two used post-finish).
- `quiz/dto/response/attempt/` — `AttemptInProgressDto`, `AttemptQuestionDto`, `AttemptOptionDto` (no `correct` field — answer-correctness is redacted during the attempt).

**Endpoints**:
- `POST /quiz` — create. Body `CreateQuizRequest(quizName, quizDuration)`. Returns the new `QuizDto`.
- `GET /quiz/authoring/{quizId}` — author-only deep view including questions, options, image flags.
- `GET /quiz?page=N` — paginated `QuizSummaryDto`. Page size is 21 so a 3-column grid fills exactly 7 rows (`QuizService.QUIZZES_PER_PAGE`).
- `DELETE /quiz/{quizId}` — author-only.
- Under `/question/...`: `addQuizQuestion` (POST), `editQuizQuestion` (PATCH), `deleteQuizQuestion` (DELETE). Each rebuilds shape validation.

**Mappers** are MapStruct interfaces in `quiz/component/mapper/` with `unmappedTargetPolicy=ERROR` so every field is accounted for. `QuizMapper` includes explicit element mappers for `Question` → `QuestionDto` and `Option` → `OptionDto` so the `hasImage` / `correct` expressions land on every collection element.

**Frontend**:
- `views/QuizCreateView.vue` — name + duration form. Submit goes to the authoring view.
- `views/QuizAuthorView.vue` — inline edit per question (toggle to `QuestionForm` and back), confirm dialogs on destructive actions, scroll-to-add-form after creation. The shared image uploader (see §11) attaches to each saved question + option.
- `components/QuestionForm.vue` — controlled by the parent, handles the type toggle, blank-option validation client-side, marker rendering (radio for single, checkbox for multi).
- New questions and options get IDs only after they're persisted, so image upload is gated to the read-only display mode of each saved card.

## 6. Quiz taking (attempts)

**Domain** (`quiz/model/`):
- **`Attempt`** — `id`, `user`, `quiz`, `finished`, `startedAt`, `finishDeadline`, `questions` (eager ordered list of `AttemptQuestion`, cascade ALL, orphan removal). Constructor `new Attempt(user, quiz, deadline)` snapshots the quiz's questions into `AttemptQuestion`s at start time so subsequent edits to the source quiz don't bleed into in-flight attempts.
- **`AttemptQuestion`** — `id`, `question` (ref to original), eager `selectedOptions` (ManyToMany via `attempt_question_selected_options`). Computes `getEarnedScore()` per question:
  - `SINGLE_CHOICE`: 1 if the single correct option is the only selection, else 0.
  - `MULTI_CHOICE`: +1 per option correctly classified (correctly selected or correctly skipped).

**Endpoints** (`quiz/controller/AttemptController.java`):
- `POST /attempt` (`AttemptQuizRequest{quizId}`) — start an attempt. Returns `AttemptInProgressDto` including a server-set `finishDeadline = now + quiz.duration`.
- `PATCH /attempt/commit` (`CommitAttemptActionsRequest{attemptId, actions[]}`) — each action `{questionId, optionId, selected}`. Single-choice questions clear other selections automatically. Refreshes the deadline-sweep before validating.
- `PATCH /attempt/finish` (`FinishAttemptRequest{attemptId}`) — sets `finished=true`, emits the `QUIZ_ATTEMPTED` notification to the author (unless self-attempt), returns `FinishedAttemptSummaryDto` (score, max, per-question + per-option breakdown).
- `GET /attempt/in-progress?page=N` and `GET /attempt/finished?page=N` — paginated per-user (10/page).

**Deadline & auto-finish** — `AttemptService.refreshFinishedAttempts` runs before every read/commit; any unfinished attempt whose `finishDeadline` has passed is force-finished server-side. The frontend's countdown timer is a UI hint; the server is the source of truth.

**Frontend**:
- `views/AttemptView.vue` — sticky progress bar (questions left, time remaining), tabular-numeral timer, per-option button. Option toggles fire `commitAttemptActions` optimistically with cache update + rollback on failure. Single-choice already-picked clicks animate a pulse instead of toggling (radio-like behaviour). Auto-finish on timer reaching 0 routes to the result view. Question and option images render lazily.
- `views/AttemptResultView.vue` — result hero with score ring (conic gradient, colour by percentage band: green ≥85%, yellow ≥50%, red <50%), per-question card with the option sort: correctly classified first, then skipped, then missed, then wrong. Rating widget mounted inline (see §7). Confetti via `components/FinishCelebration.vue` on initial visit (controlled by `?just=1` query param).

## 7. Quiz ratings

Captured after a user finishes an attempt of a quiz. **One rating per (user, quiz)**; updatable in place; doesn't re-notify.

**Entity** (`quiz/model/QuizRating.java`):
- `id`, `quiz`, `user`, `score` (1–10), `comment` (≤500 chars, nullable), `createdAt`, `updatedAt`.
- DB-level unique constraint on `(quiz_id, user_id)` and a `CHECK score BETWEEN 1 AND 10` (`db/changelog/004-quiz-ratings.yaml`).

**Eligibility** — `QuizRatingService.upsert` requires `attemptRepository.existsByUser_IdAndQuiz_IdAndFinishedIsTrue` to be true, else throws `RatingNotEligibleException` (`RATING_NOT_ELIGIBLE` / 403). Score outside `[1, 10]` throws `InvalidRatingException` (`INVALID_RATING` / 400, details `{minimum, maximum}`).

**Endpoints under `/quiz/{quizId}/ratings`**:
- `PUT /me` (`UpsertRatingRequest{score, comment}`) — upsert; first creation also fires the `QUIZ_RATED` notification to the quiz author (unless self-rating).
- `GET /me` — `200 + QuizRatingDto` if rated, `204 No Content` if not.
- `GET ?page=N` — paginated list of all ratings on the quiz (10/page, newest first).
- `GET /summary` — `{average, count}`.

The `ratingSummary` is also embedded into every `QuizSummaryDto` (`average`, `count`) so quiz cards can render `★ 8.4 (12)` without an extra round-trip.

**DTO mapping is inside `@Transactional` boundaries** — the service returns `QuizRatingDto`, not `QuizRating`. This is deliberate: `QuizRating.user` is `FetchType.LAZY` and `open-in-view: false`, so a Jackson serializer reaching into a closed Hibernate session would throw `LazyInitializationException`. (The bug existed briefly in feature 1 and was fixed.)

**Frontend**:
- `views/AttemptResultView.vue` — inline 1–10 widget as a single `role="radiogroup"` (not 10 buttons); hover preview, native arrow-key support, optional comment textarea. Dismissible via a `quiz-rating-dismissed:<quizId>` flag in localStorage, but always shown once you have a saved rating.
- `components/QuizCard.vue` — `★ avg (count)` chip in the meta row when count > 0.
- `views/QuizAuthorView.vue` — paginated rating list with each rater's avatar, score, comment, relative time.

Resolver `INVALID_RATING` interpolates the min/max from details into "Rating must be between 1 and 10." `RATING_NOT_ELIGIBLE` resolves to "Finish the quiz before rating it." Both in `lib/errors.ts`.

## 8. Leaderboards

Two global boards, **paginated server-side, with the caller's overall rank computed across the full qualifying population**.

**Top players** (avg attempt score, Bayesian-shrunk):
- Qualifying threshold: ≥3 finished attempts (`MIN_PLAYER_ATTEMPTS`).
- For each qualifying user, compute `adjusted = (sumOfPercentages + K * priorMean) / (attempts + K)` with `K = 5` and `priorMean = 50.0`. This is the deliberate fix to the original "100% on 1 attempt beats 90% on 50 attempts" behaviour. Examples:
  - `100% × 1` → 58.3 (heavily shrunk)
  - `100% × 3` → 68.75 (still shrunk)
  - `100% × 10` → 83.3
  - `90% × 50` → 86.4
- Sort by `(adjusted DESC, attempts DESC)`. The comparator builds the natural-order chain first and then reverses the *composed* result — chaining `.reversed()` per key would flip the second key back to ascending (a real bug that shipped briefly and got fixed with an explicit regression spec).
- Compute happens in Java because `Attempt.maximumScore` is derived from quiz question shapes, not stored on the row.

**Top authors** (avg rating across each user's quizzes):
- Qualifying threshold: ≥5 total ratings spread across the author's quizzes (`MIN_AUTHOR_RATINGS`).
- DB-side aggregation via JPQL (`ApplicationUserRepository.findAuthorRankings`).

**Endpoints** (`user/controller/LeaderboardController.java`):
- `GET /leaderboards/players?page=N`
- `GET /leaderboards/authors?page=N`

Both return `LeaderboardPageDto{entries[], page, totalPages, totalElements, you}`. `you` carries the caller's rank, even if they're off the current page. `LeaderboardEntryDto` is `(rank: int, user: AuthorSummaryDto, primaryValue: double, secondaryValue: long)` — primary is "Score" (adjusted) for players, "Avg rating" for authors; secondary is "attempts" or "ratings".

**Frontend** (`views/LeaderboardsView.vue`):
- Tabs for Players / Authors. Sticky "You are ranked X of N" pill at the top of each board. The user's own row gets a primary-tinted background. Mobile collapses to a 2-column tab grid + condensed row layout.

Internal helpers `LeaderboardService.rankedPlayerIds()` / `rankedAuthorIds()` return the unpaginated ranking — used by the snapshot job (see §10).

## 9. Profile comments

Basic: post + list + delete. No edits, no replies, no threading.

**Entity** (`user/profile/model/ProfileComment.java`):
- `id`, `target` (the profile owner, ManyToOne), `author` (commenter, ManyToOne), `body` (≤1000 chars, NOT NULL), `createdAt`. Index on `(target_user_id, created_at DESC)`.
- Both FKs cascade `ON DELETE`, so deleting a user wipes their incoming + outgoing comments.

**Endpoints** under `/users/{username}/comments`:
- `GET ?page=N` — paginated 10/page, newest first.
- `POST {body}` — any authenticated user; `INVALID_COMMENT` on blank or >1000 chars.
- `DELETE /{commentId}` — allowed if requester is **the author OR the profile owner**, else `NO_ACCESS_TO_COMMENT`.

`ProfileCommentDto` includes `canDelete: boolean` computed for the requesting user so the FE doesn't re-derive the rule.

**Frontend** (`components/ProfileComments.vue`):
- Composer with char counter (goes red when negative).
- Paginated list with avatar, display name, relative time, trash icon (visible only when `canDelete`).
- Delete uses the shared `confirmDialog`.
- Mounted on both own profile and public profile views.

## 10. Notifications

Four trigger types reach a single inbox per user.

**Types** (`notification/model/NotificationType.java`):
1. `COMMENT_RECEIVED` — someone commented on your profile.
2. `QUIZ_RATED` — someone rated one of your quizzes (**first** rating only; re-rates don't notify).
3. `QUIZ_ATTEMPTED` — someone finished an attempt of one of your quizzes.
4. `RANK_DROPPED` — your rank on a leaderboard worsened since the last snapshot.

**Sync triggers** are wired into the existing service methods with self-notification guards:
- `ProfileCommentService.post` — every comment posted (recipient ≠ author).
- `QuizRatingService.upsert` — only when `isNew && !self` (re-rates skipped).
- `AttemptService.finishAttemptAsUser` — every finish (recipient ≠ attempter).

**Async trigger** is the **leaderboard snapshot job** (`notification/service/LeaderboardSnapshotService.java`):
- `@EnableScheduling` on `BackendApplication`.
- `@Scheduled(fixedDelayString = "${notifications.snapshot.interval-ms:900000}", initialDelayString = "${notifications.snapshot.initial-delay-ms:60000}")` — default 15 min, 60 s initial delay; e2e overrides to 2 s.
- On each run: fetch ranked player IDs + ranked author IDs from `LeaderboardService`, look up the latest stored snapshot per (user, board), compare. If `newRank > prev.rank` (i.e. the rank number grew), emit `RANK_DROPPED`. First snapshot per user/board doesn't emit (no baseline). After comparison, persist a fresh snapshot row.

**Schema** — two migrations:
- `006-notifications.yaml`: `notifications(id, recipient_user_id [FK CASCADE], type VARCHAR(40), payload VARCHAR(2000), read_at, created_at)` + index on `(recipient_user_id, created_at DESC)`.
- `007-leaderboard-snapshots.yaml`: `leaderboard_snapshots(id, user_id [FK CASCADE], board VARCHAR(16), rank INT, snapshot_at)` + index on `(user_id, board, snapshot_at DESC)`.

**Payload** is a small JSON blob (Jackson) so each type can carry its own context:
- `COMMENT_RECEIVED` → `{actorUsername, actorDisplayName, commentId, preview}`
- `QUIZ_RATED` → `{actorUsername, actorDisplayName, quizId, quizName, score}`
- `QUIZ_ATTEMPTED` → `{actorUsername, actorDisplayName, quizId, quizName, attemptId, score, maxScore}`
- `RANK_DROPPED` → `{board: "PLAYERS"|"AUTHORS", from: int, to: int}`

**Endpoints** under `/notifications`:
- `GET ?page=N` — paginated 20/page, newest first.
- `GET /unread-count` — `{count: long}`.
- `POST /{id}/read` — marks `readAt = now()` and returns the DTO. 404 if not yours.
- `POST /read-all` — bulk update, returns `{updated: int}`.

**Frontend**:
- `components/NotificationBell.vue` — bell icon (outline SVG, inherits `currentColor`) in the AppShell topbar. Unread badge auto-refetches every 30 s via TanStack Query `refetchInterval`. Dropdown lazy-fetches the list only when the menu opens. Per-item click marks read + navigates to the type's target (profile, quiz, leaderboard). "Mark all read" + "See all" actions.
- `views/NotificationsView.vue` — full history at `/app/notifications`. Same activation behaviour, paginated, mark-all-read button.
- Mobile thumb menu carries a Notifications link.

## 11. Quiz images (cover / question / option)

Generalisation of the avatar pipeline to three new attachment points. Same Twelvemonkeys WebP plugin handles decoding.

**`QuizImageStorageService`** (`quiz/service/QuizImageStorageService.java`) takes a `Kind` enum:

| Kind | Subdir | Max longest side |
|---|---|---|
| COVER | `quizzes/` | 1280 px |
| QUESTION | `questions/` | 640 px |
| OPTION | `options/` | 480 px |

- Cap: **4 MB** per upload (`MAX_UPLOAD_BYTES`); multipart filter also raised to **5 MB** in `application.yaml` for headroom.
- MIME whitelist: PNG, JPEG, WebP.
- Decodes via ImageIO, scales down so the longest side fits the cap (preserves aspect ratio, smooth interpolation), re-encodes as JPEG, writes via atomic rename. Image content already smaller than the cap is still re-encoded to strip metadata and normalise to `TYPE_INT_RGB`.

**Entity columns** (Liquibase `008-quiz-images.yaml`):
- `quizzes.cover_image_path` VARCHAR(255)
- `questions.image_path` VARCHAR(255)
- `options.image_path` VARCHAR(255)

DTOs surface this as boolean flags (`hasCover` on `QuizDto` and `QuizSummaryDto`, `hasImage` on `QuestionDto` / `OptionDto` / `AttemptQuestionDto` / `AttemptOptionDto` / `FinishedQuestionDto` / `FinishedOptionDto`). The FE builds URLs from the entity ID — no need to ship the path through the API.

**Endpoints** (`quiz/controller/QuizImageController.java`):
- `PUT/DELETE /quiz/{quizId}/cover` (owner-only)
- `PUT/DELETE /question/{questionId}/image` (owner-only, ownership through the parent quiz)
- `PUT/DELETE /option/{optionId}/image` (owner-only)
- `GET /quiz/{quizId}/cover`, `GET /question/{questionId}/image`, `GET /option/{optionId}/image` — all **public** so any signed-in user taking the quiz can fetch them. `produces=image/jpeg`, `Cache-Control: max-age=300`.

Reuses error codes `INVALID_IMAGE` and `AVATAR_TOO_LARGE` rather than fragmenting copy.

**Frontend**:
- `components/ImageUploader.vue` — shared file picker + preview + Replace/Remove overlay. Accepts `image/png,image/jpeg,image/webp`.
- `lib/quizImages.ts` — `coverUrl(quizId, v?)`, `questionImageUrl(questionId, v?)`, `optionImageUrl(optionId, v?)` build URLs with an optional `?v=...` cache-bust.
- `views/QuizAuthorView.vue` — cover uploader above the quiz title; per-question and per-option uploaders on each saved question card.
- `views/AttemptView.vue` — renders question images (lazy-loaded, max-height 320 px) and option images (max-height 160 px) inline.
- `views/AttemptResultView.vue` — same in the read-only view.
- `components/QuizCard.vue` — full-width cover image above the card title when `hasCover`.

## 12. Search & discovery

`SearchController` (`search/controller/SearchController.java`) exposes one endpoint:

- `GET /search?q={query}` returns `SearchResponseDto{quizzes[], users[]}`.
- Substring, case-insensitive on quiz names (`findByNameContainingIgnoreCaseOrderByNameAsc`) and on `username + displayName` for users.
- Short-circuits to empty when `q.trim().length < 2` (the FE also gates at 2 chars).

**Frontend** (`views/BrowseView.vue`):
- Debounced 250 ms via `lib/useDebouncedRef`.
- The results render as an **overlay panel above** the dashboard rather than replacing it (early polish fix — searching no longer hides everything else).
- `aria-live="polite"` count for screen readers.
- Esc closes the overlay and re-focuses the search input.
- `/` and `Cmd/Ctrl+K` from anywhere in `/app/*` focus the search input (handled in `AppShell.vue`).

## 13. Dashboard

`views/BrowseView.vue` — the `/app` landing for signed-in users.

- **Hero**: greeting using `displayName ?? username`, three stat cards:
  - Quizzes authored (count)
  - Attempts taken (count)
  - Average score (% or em-dash when no attempts)
- **Rank pills**: under the stats, if the user qualifies on either leaderboard, primary-tinted pills like `#7 Top players` link to `/app/leaderboards`. Both can show together.
- **Search**: described in §12.
- **Continue** section — in-progress attempts as accent-bordered "Resume" cards, with a `warning`-toned chip when the attempt is >1 h old; section hides entirely when empty (no "No active attempts." copy noise).
- **Browse quizzes** — `auto-fill, minmax(280px, 1fr)` grid, total count in the heading, **numeric pager** with `« 1 … 5 … 12 »` collapse on mobile and `aria-current="page"` on the active page.
- **Past results** — finished-attempt cards linking to the result view. Hash anchor `#past-results` is supported via a one-shot `watchEffect` guarded by `hasScrolled` so it doesn't re-scroll on a refetch.
- **Loading states** use inline shimmer skeleton blocks rather than "Loading…" text, so the layout doesn't reflow when data arrives.

## 14. Navigation & app shell

`components/AppShell.vue` wraps every authenticated route at `/app/*`.

- **Desktop topbar**: BrandMark (links to `/app`), nav (`Browse`, `Leaderboards`, `New quiz`), `NotificationBell`, signed-in user chip (avatar + name) linking to `/app/profile`, sign-out button (with `confirmDialog` warning).
- **Mobile**: a thumb-zone FAB in the bottom-right opens a slide-up menu (`Browse`, `Leaderboards`, `New quiz`, `Your profile`, `Notifications`, `Sign out`). Outside-click closes, Escape closes + refocuses the FAB.
- **Keyboard shortcuts**: `/` and `Cmd/Ctrl+K` focus the dashboard search input from anywhere in `/app/*` (skipped when typing in another input).
- The `<ToastStack />` global toast surface is mounted at the App.vue level so unauthenticated routes (landing, login, register) can also surface toasts.

## 15. Branding & shared UI primitives

**`BrandMark`** (`components/BrandMark.vue`):
- Italic 67quiz wordmark with three sizes: `sm` (topbar, 22 px), `md` (auth cards, 32 px), `lg` (landing hero, `clamp(2.75rem, 8vw, 4.5rem)`).
- Used on AppShell topbar, LandingView hero, LoginView card header, RegisterView card header.

**Reusable components** (`components/`):
- `Card.vue`, `Chip.vue`, `Button.vue`, `Input.vue`, `Modal.vue`, `Avatar.vue` — UI primitives. All six previously failed `vue/multi-word-component-names`; resolved by `defineOptions({ name: 'UiX' })` without renaming files.
- `ConfirmDialog.vue` + `lib/confirmDialog.ts` — promise-based imperative dialog reused for delete confirmations app-wide.
- `ToastStack.vue` + `stores/toast.ts` — Pinia-backed global toast queue. Each toast has tone (`error`/`warning`/`info`/`success`), auto-dismiss timer per tone, optional inline action button. Mobile-aware (sits above the FAB), respects `prefers-reduced-motion`.
- `ProgressBar.vue` — used in `AttemptView` for the timer ring.
- `FinishCelebration.vue` — confetti overlay on the first visit to a result page.
- `ImageUploader.vue` — see §11.
- `QuestionForm.vue`, `QuizCard.vue`, `UserCard.vue`, `ProfileComments.vue`, `NotificationBell.vue` — feature-specific.

## 16. Error handling pipeline

**Backend** (`shared/dto/`, `shared/component/GlobalExceptionHandler.java`):
- Every error response is a `Failure(HttpStatus, List<ApiError>)`. Each `ApiError(code, details)` carries a SCREAMING_SNAKE code and an optional `Map<String, Object>` of context.
- Per-controller `@RestControllerAdvice`s narrow exceptions to specific codes:
  - `QuizControllerExceptionHandler`, `QuestionControllerExceptionHandler`, `AttemptExceptionHandler`, `QuizRatingControllerExceptionHandler`, `QuizImageControllerExceptionHandler` (all in `quiz/component/`)
  - `LoginExceptionHandler`, `RegistrationExceptionHandler` (in `authentication/component/`)
  - `UserProfileExceptionHandler`, `ProfileCommentControllerExceptionHandler` (in `user/profile/component/`)
  - `NotificationControllerExceptionHandler` (in `notification/component/`)
- `GlobalExceptionHandler` handles framework exceptions: `MethodArgumentNotValidException` → `VALIDATION_ERROR` per field, `HttpMessageNotReadableException` → `INVALID_FORMAT`, `HttpMediaTypeNotSupportedException` → `UNSUPPORTED_MEDIA_TYPE`, `HttpRequestMethodNotSupportedException` → `METHOD_NOT_ALLOWED`, `NoHandlerFoundException` / `NoResourceFoundException` → `NOT_FOUND`, catch-all → `INTERNAL_SERVER_ERROR`.
- `application.yaml` strips stack traces, messages, binding errors, and exception class from the default Spring error body so nothing leaks past the structured `Failure`.
- The `ApiError.code` field has an `@Schema(allowableValues = {...})` enumerating every code so the generated OpenAPI surfaces them as a union.

**Frontend** (`lib/errors.ts`):
- `errorMessage(err)` resolves any axios error to a user-facing string. Detail-aware resolvers per code:
  - `INVALID_USERNAME` reads `details.violation` (`TOO_SHORT`/`TOO_LONG`) + `details.minimumLength`/`maximumLength` → "Username must be at least 5 characters."
  - `INVALID_PASSWORD` → "Password must be at least 8 characters."
  - `BLANK_OPTION_TEXT` reads `details.index` → "Option 3 text cannot be blank."
  - `AVATAR_TOO_LARGE` reads `details.maxBytes` → "That image is over 2 MB. Pick a smaller one."
  - `INVALID_RATING` reads `details.minimum`/`maximum`.
  - `USER_NOT_FOUND` reads `details.username` and quotes it.
  - `VALIDATION_ERROR` and `INVALID_FORMAT` read `details.field` to point at a specific input.
- `validationFieldErrors(err)` returns a `Record<field, message>` map so forms can highlight specific `<Input>`s via the existing `error` prop.
- `httpStatus(err)` plus a `STATUS_FALLBACK` map gives sane messages for 401/403/404/408/413/415/429/500/502/503/504 when no domain code is present.

**Axios interceptor** (`lib/axiosInterceptors.ts`, installed in `main.ts`):
- 401 on protected routes → clear auth, toast "Your session has expired.", redirect to `/login?next=…`. Skipped for the anonymous-probe endpoints (`/users/me`, `/authentication/me`) and for `/authentication/login` and `/register` (those views own the 401).
- 5xx → error toast with the resolved message.
- 413 (size cap) → error toast.
- 429 → warning toast.
- 403 with code `FORBIDDEN` → warning toast (specific domain codes pass through to view-level handlers).
- **Network failure** (no response) → "Network problem. Check your connection and try again."
- **Cancelled requests** (`axios.isCancel(err)` or `err.code === 'ERR_CANCELED'`) → pass through without toasting. This was a real bug — vue-query cancels in-flight requests on cache invalidation, which made every rating save flash a bogus "Network problem" toast until fixed.

**Inline form errors** in `LoginView`, `RegisterView`, `QuizCreateView`, `ProfileView`'s edit modal, and the new `ProfileComments` composer, all driven by `validationFieldErrors` + the `Input.error` prop. Field errors render as sentence-case body text (not `label-sm` which would uppercase them).

## 17. Infrastructure, build, deploy

**Backend** (`backend/build.gradle.kts`):
- Java 25 toolchain via Gradle's `JavaLanguageVersion.of(25)`.
- Spring Boot 4.0.6 starters: `data-jpa`, `liquibase`, `webmvc`, `validation`, `security`, `hateoas`, `session:spring-session-jdbc`, `security:spring-security-config`.
- `springdoc-openapi-starter-webmvc-ui:3.0.2` exposes Swagger UI; `springdoc-openapi-gradle-plugin:1.9.0` writes the OpenAPI spec to `../api/openapi.json` via `./gradlew generateOpenApiDocs`.
- `mariadb-java-client:3.5.8`, `mapstruct:1.6.3` (+ annotation processor), `imageio-webp:3.12.0` (Twelvemonkeys WebP decoder).
- Test starters for boot, security, webmvc.

**`application.yaml`** highlights:
- `spring.profiles.default: prod` — fresh deployments don't run dev seeders.
- `spring.jpa.open-in-view: false` — lazy associations must be initialised inside the service tx.
- `spring.session.store-type: jdbc` with `initialize-schema: never` (Liquibase owns the schema).
- `spring.servlet.multipart.max-file-size: 5MB` (Spring's filter; per-feature caps go lower).
- `spring.web.error.*` scrubbing of stack traces, messages, binding errors.
- `spring.datasource` driven by `DATABASE_URL/HOST/PORT/NAME/USERNAME/PASSWORD` env vars (defaults match the local Compose).
- `spring.liquibase.change-log: classpath:/db/changelog/main.yaml`.

**`application-openapi.yaml`** is a minimal profile activated by the OpenAPI plugin's bootRun so spec generation doesn't need the full prod config.

**Liquibase changelog files** (under `backend/src/main/resources/db/changelog/`):
1. `000-security.yaml` — Spring Security + Spring Session JDBC tables.
2. `001-domain.yaml` — `users`, `roles`, `user_roles`, `quizzes`, `questions`, `options`, `quiz_questions`, `quiz_attempts`, `attempt_questions`, `attempt_question_selected_options`.
3. `002-user-profiles.yaml` — `avatar_path`, `bio` columns on `users`.
4. `003-question-types.yaml` — `type` column on `questions` with backfill.
5. `004-quiz-ratings.yaml` — `quiz_ratings` table + unique constraint + score CHECK.
6. `005-profile-comments.yaml` — `profile_comments` table.
7. `006-notifications.yaml` — `notifications` table.
8. `007-leaderboard-snapshots.yaml` — `leaderboard_snapshots` table.
9. `008-quiz-images.yaml` — `cover_image_path`, `image_path` columns on `quizzes` / `questions` / `options`.
10. `009-drop-sampler-fixture.yaml` — one-shot prod cleanup (deletes the `sampler` user; CASCADE removes their seeded quizzes/attempts/etc).

**`SamplerSeeder`** (`shared/component/SamplerSeeder.java`):
- An `ApplicationRunner` annotated `@Profile("!prod")`. With the default profile set to `prod`, it doesn't run in production. Locally (`SPRING_PROFILES_ACTIVE=local`) it creates a `sampler` user and two demo quizzes ("Sampler 10", "Mixed types") that the e2e suite consumes.
- Idempotent on the user: if `sampler` exists, no-op. The two quizzes are seeded in the same transaction that creates the user — once the user exists, subsequent boots leave the data alone.

**Frontend** (`frontend/package.json`):
- Vite 8 + Vue 3.5 + Vue Router 5 + Pinia 3 + TanStack Query 5 + Axios 1.16 + TypeScript 6.
- `orval` 8 generates the typed API client from `api/openapi.json` (mutator wired to `src/lib/axios.ts` so cookies + base URL flow through one place).
- `npm run dev` → vite, `npm run build` → type-check + bundle, `npm run lint` runs both `oxlint` and `eslint --fix`. Lint is currently **clean across 45 files**.

**CI** (`.github/workflows/build-and-push.yml`):
- Generates the OpenAPI spec, uploads it as an artifact, then runs the FE build against the freshly generated spec. The contract gate prevents BE/FE drift.
- Builds + pushes a unified Docker image (`ghcr.io/suohcnamid/67-quiz`) for deploy.

**Local dev DB** — `backend/local/67quiz/docker-compose.yaml` runs MariaDB; `podman compose -f ... up -d` brings it up on port 3306 with the credentials the default `application.yaml` expects.

## 18. Testing

**Backend** — `BackendApplicationTests` loads the full Spring context, exercising bean wiring and Liquibase against the real DB. Run with `./gradlew test`.

**End-to-end** — Playwright suite at `e2e/tests/`. Currently **79 specs across 17 files**, all green:

| Spec file | Coverage |
|---|---|
| `landing.spec.ts` | Anonymous landing, get-started and sign-in CTAs |
| `routing.spec.ts` | Route guards, redirects, 404 handling for unknown deep links |
| `not-found.spec.ts` | `NotFoundView` rendering inside the shell + at the top level |
| `mobile.spec.ts` | AppShell at 375px, FAB menu, profile stack, attempt header |
| `profile.spec.ts` | Own + public profile, edit modal, avatar upload, author chip linkage |
| `my-quizzes.spec.ts` | Authored-quizzes listing on profile, own vs public actions |
| `authoring.spec.ts` | Question add/edit/delete, inline editing, single-choice 2-correct rejection |
| `happy-path.spec.ts` | Full register → create quiz → start → finish → result flow |
| `question-types.spec.ts` | Single vs multi scoring + UI invariants |
| `timer-and-spa.spec.ts` | Timer countdown, auto-finish on expiry, SPA stability |
| `search.spec.ts` | 2-char threshold, debounce, clearing, result navigation |
| `rating.spec.ts` | Eligibility, upsert, dropdown copy, ★ on quiz card, accessible radiogroup, no spurious network-error toast on save |
| `leaderboards.spec.ts` | Nav, "you" pill, Bayesian ordering proof (80%×10 outranks 100%×3), threshold gating, pagination walking |
| `comments.spec.ts` | Post + self-delete, owner-delete, non-owner-non-author can't delete, empty body rejected |
| `notifications.spec.ts` | All four triggers, bell badge, dropdown, mark-read + mark-all, history view, rank-drop with shortened scheduler |
| `webp-upload.spec.ts` | Real WebP avatar via Twelvemonkeys decoder + garbage bytes rejection |
| `images.spec.ts` | Cover/question/option PUT/GET/DELETE round-trip, non-author 403, INVALID_IMAGE on garbage |

The rank-drop spec runs the backend with `NOTIFICATIONS_SNAPSHOT_INTERVAL_MS=2000` so the scheduler is observable inside a test window. The WebP and image specs skip when `/tmp/probe.webp` is missing so CI without `cwebp` installed still passes.
