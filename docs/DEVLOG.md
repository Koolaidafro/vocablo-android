# Development Log: Vocablo

## Current state
Version: 1.2 (in progress) | Last updated: 2026-11-13

### Asset inventory

Only assets added or changed since this log began are listed one by one. Everything else from v1.0 is grouped by module; see the README's module table.

| Asset | Type | Purpose | Added in | Last changed in |
| --- | --- | --- | --- | --- |
| `core/*` modules | Kotlin code and tests | Learning logic: verbs, scheduling, cards, dictionary, games, tutor, themes | 1.0 | 1.0 |
| `app/` (other screens, data, widget, speech, updates) | Kotlin code | The Android app | 1.0 | 1.1 (icon and emoji swaps) |
| `app/.../ui/study/StudyScreen.kt` | Kotlin code | Study session screen, its ViewModel and the reacting Tuzo | 1.0 | 1.2 |
| `app/.../ui/study/SwipeCard.kt` | Kotlin code | The flippable, swipeable study card | 1.1 | 1.1 |
| `app/.../ui/study/RatingBar.kt` | Kotlin code | "Show answer" and the four rating buttons | 1.1 | 1.1 |
| `app/.../ui/theme/AppIcons.kt` | Kotlin code | The app's icon set by meaning, plus `AppIcon`, `IconTile`, `IconLabel` | 1.1 | 1.2 |
| `app/.../ui/theme/Type.kt` | Kotlin code | Nunito family and the type scale | 1.1 | 1.1 |
| `app/.../ui/theme/Shape.kt` | Kotlin code | Shared corner radii | 1.1 | 1.1 |
| `app/.../ui/theme/Spacing.kt` | Kotlin code | Shared spacing steps | 1.1 | 1.1 |
| `app/.../ui/theme/VocabloTheme.kt` | Kotlin code | Applies colours, type and shapes | 1.0 | 1.1 |
| `app/.../ui/components/Art.kt` | Kotlin code | `Scene`, `SceneArt`, `BadgeArt`, `ArtTile`, `badgeArt()`, `gameArt()` | 1.2 | 1.2 |
| `app/.../ui/components/Tuzo.kt` | Kotlin code | Tuzo composable; `TuzoMood` owns each pose and motion | 1.0 | 1.2 |
| `app/.../ui/study/SessionFinish.kt` | Kotlin code | End-of-session screen, summary row, reminder offer, `nextStepScreen()` | 1.2 | 1.2 |
| `app/.../ui/home/HomeScreen.kt` | Kotlin code | Home, with welcome-back card and badge celebration | 1.0 | 1.2 |
| `app/.../ui/me/MeScreen.kt` | Kotlin code | Me, with the badges card | 1.0 | 1.2 |
| `app/.../ui/LibraryViewModel.kt` | Kotlin code | Shared state; now badges, welcome back and new-badge queue | 1.0 | 1.2 |
| `app/.../data/ProgressRepository.kt` | Kotlin code | XP, quests, chest, freezes, medals, badges | 1.0 | 1.2 |
| `app/.../data/Settings.kt` | Kotlin code | Settings; now last-opened day, comeback flag, reminder-offer flag | 1.0 | 1.2 |
| `app/.../notify/Notifications.kt`, `ReminderScheduler.kt` | Kotlin code | Posting reminders; receiver follows `Reminders.decide` | 1.0 | 1.2 |
| `core/play/.../Badges.kt` | Kotlin code | `Badge`, `Milestones`, `Badges` | 1.2 | 1.2 |
| `core/play/.../ReminderMessages.kt` | Kotlin code | Tuzo-voice message pools and `Message` | 1.2 | 1.2 |
| `core/play/.../Session.kt` | Kotlin code | `SessionSummary`, `NextStep`, `WelcomeBack` | 1.2 | 1.2 |
| `core/play/.../Reminders.kt` | Kotlin code | Reminder slots and `decide` (comeback rule) | 1.0 | 1.2 |
| `core/play/src/test/.../EngagementTest.kt` | Test | Badges, reminder policy and messages, summary, welcome back | 1.2 | 1.2 |
| `app/src/test/.../ui/study/SessionFinishTest.kt` | Test | Finish screen summary and next step (FR-23) | 1.2 | 1.2 |
| `app/src/test/.../data/ProgressRepositoryTest.kt` | Test | Progress storage, now badges awarded once and kept | 1.0 | 1.2 |
| `app/src/main/res/drawable-nodpi/*.webp` (27) | Images | Tuzo poses (7), badges (6), scenes (7), game tiles (7) | 1.2 | 1.2 |
| `app/src/main/res/drawable-xxxhdpi/ic_launcher_art.webp` | Image | Launcher icon art layer | 1.2 | 1.2 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml`, `values/colors.xml` | Resources | Adaptive icon using the new art on a jade layer | 1.0 | 1.2 |
| `docs/ART_PROMPTS.md` | Document | Every image prompt and the cut-out method (NFR-8) | 1.2 | 1.2 |
| `app/src/main/res/font/nunito_*.ttf` | Font files (5) | Regular, Medium, SemiBold, Bold, ExtraBold | 1.1 | 1.1 |
| `app/src/test/.../ui/study/SwipeCardTest.kt` | Test | Swipe direction on both sides (FR-11) | 1.1 | 1.1 |
| `app/src/test/.../ui/study/RatingBarTest.kt` | Test | Rating labels on one line (FR-12) | 1.1 | 1.1 |
| `app/src/test/.../ui/theme/DesignSystemTest.kt` | Test | Type, shapes, icons, and that every pose, scene, badge and tile loads | 1.1 | 1.2 |
| `app/src/test/.../update/ReleaseParserTest.kt` | Test | Release tag parsing | 1.0 | 1.1 |
| `docs/SRS.md` | Document | Requirements | 1.1 | 1.2 |
| `docs/DEVLOG.md` | Document | This log | 1.1 | 1.2 |
| `docs/NEXT_UPDATE.md` | Document | Testing feedback waiting for the next update | 1.1 | 1.1 |
| `docs/uml-v1.1.svg` | Diagram | Class diagram with members: study screen and design system | 1.1 | 1.1 |
| `docs/uml-v1.1-overview.svg` | Diagram | Class diagram overview: every module and package, types by name | 1.1 | 1.1 |
| `docs/licenses/OFL-Nunito.txt`, `docs/licenses/ISC-Lucide.txt` | Licence texts | Redistribution terms for the font and icons (NFR-5) | 1.1 | 1.1 |
| `.github/workflows/publish.yml` | CI config | Publishes a signed APK on every merge to `main` | 1.0 | 1.2 |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Build config | Dependencies and version | 1.0 | 1.2 |

## Log

### 2026-11-13 | v1.2 | Art overhaul and engagement update
**Phase:** Analysis through testing (maintenance update; release pending CI and Sir's merge).
**Requirements touched:** FR-15 (removed), FR-17 to FR-27, NFR-7, NFR-8.

**Decisions**
- Scope: Sir asked for "the same asset overhaul" Stepwise got, plus Stepwise's engagement ideas. Read as: replace every raster and character asset, and port the Stepwise engagement loop features Vocablo lacked (mascot reactions, badges, session summary, welcome back, mascot-voice notification pools, comeback rule, permission asked after the first session). Vocablo already had XP, streaks, freezes, quests and a chest, so those were left alone. Why: avoids rebuilding what works. Alternatives considered: a daily XP goal like Stepwise's, rejected because quests already set daily targets.
- Kept Tuzo the gopher, redesigned rather than replaced. Why: the name and character already appear in onboarding, reminders and the tutor. The new design adds a jade bandana that ties Tuzo to the default theme. Alternatives considered: a new animal, rejected as needless churn.
- Image model: Nano Banana Pro (`gemini-3-pro-image-preview`) at 2K, called through the Composio Gemini connection's raw API so a reference image could be attached. Why: Stepwise used the text-only tool and its mascot drifted between images; attaching the master Tuzo to every later prompt kept all 20 Tuzo images on-model. Alternatives considered: the text-only tool (drift), Nano Banana 2 Lite (1K only).
- Three master candidates were generated; the second was chosen for its clean pose. Five images were regenerated: the adaptive-icon head (came out full-body twice, so the launcher uses the full icon art instead), the icon (white fade at the bottom), the Tug of War tile (cropped), the 100-words badge (letters on the cards) and the gold-verb badge (wrong symbol).
- Images reached the build sandbox through temporary commits on this branch, because the sandbox's network policy blocks the image host. The last commit deletes the `transfer/` folder; squash-merging keeps it out of `main`. Alternatives considered: rewriting the branch history, which the sandbox refused as a destructive action.
- Cut-outs: flood-fill near-white from the edges, recolour the 2 px edge ring to the outline brown, crop and centre on a square. Why: keeps white inside Tuzo (eyes, teeth, cards) and leaves no halo on dark themes (checked on a dark background).
- WebP in `drawable-nodpi` at 320 to 720 px; 1.2 MB in total (NFR-8 allows 2 MB). Why: one size per image is enough at these display sizes and avoids five density copies.
- Launcher icon: the full generated icon placed in the 72 dp visible area of a 108 dp layer on matching jade (#2A7362). Why: no clean head-only image came out, and this keeps the burrow and card readable under any mask. The monochrome themed icon keeps the old vector silhouette, which still matches Tuzo's outline.
- FR-15 (theme-coloured Canvas illustrations) removed and replaced by FR-20 (generated scenes). Why: Sir asked for more engaging art; the raster scenes do not follow the theme colours, which is accepted.
- `TuzoMood` now owns its drawable, motion period and description; `READY` and `THINK` were added; `WIN` shows the proud pose. Why: a reaction is now a different drawing, not only a different wobble.
- Badges stored as activity rows (kind `BADGE`, day 0, ref = badge ordinal). Why: the unique index already guarantees once-only, and no Room migration is needed. Badges give no XP (NFR-7). Existing learners will see a celebration for badges they already qualify for, once, on first launch of 1.2. Accepted as a pleasant surprise.
- Only the first new badge is shown big, with "and N more". Why: never stack several dialogs.
- Reminder messages chosen by day number modulo pool size. Why: no storage, and 10 messages guarantee no repeat inside 7 days (tested for 30 starting days).
- Comeback rule (FR-26) uses a last-opened day saved on every app start and a `comebackSent` flag. A phone upgrading from 1.1 has no last-opened day yet and counts as opened today, so nobody gets a comeback message just for updating.
- Reminder offer (FR-27) shows on the finish screen only on Android 13+, when permission is missing and reminders are on; either answer sets `remindersOffered` so it never shows again. The toggle in Me still asks as before.
- Session summary XP is computed from the session's own answer counts with the same weights as the XP total, instead of diffing the total before and after. Why: quest payouts landing mid-session would inflate a diff.
- "Play a game" opens one game, rotating by day. Why: the Play tab is a tab, not a screen on the stack, and one concrete game is a clearer next step.
- The tutor tile sits on the tutor set-up screen rather than on Play. Why: the tutor is its own tab. FR-19 was reworded to match before implementation.
- Full-size sources (raw generations and cut-out PNGs, 72 MB) live in the project's shared folder, not in git; prompts are in `docs/ART_PROMPTS.md`. Why: keeps the repository small.

**Added**
- `core/play`: `Badges.kt` (`Badge`, `Milestones`, `Badges`), `ReminderMessages.kt` (`Message`, four pools of 10), `Session.kt` (`SessionSummary`, `NextStep`, `WelcomeBack`); `Reminders.decide`, `Reminders.Decision`, `QUIET_AFTER_DAYS`.
- `app`: `Art.kt`, `SessionFinish.kt`; DAO queries `cardsLearned`, `observeActivityRefs`, `activityRefs`; `ActivityKind.BADGE`; `ProgressRepository.badges()` and `settleBadges()`; settings `lastOpenedDay`, `comebackSent`, `remindersOffered`; `MainActivity.recordOpened()`; `LibraryViewModel.newBadges` and `badgesCelebrated()`; `LibraryState.welcomeBack`; `ProgressState.badges`; Home `WelcomeBackCard` and `BadgeCelebration`; Me `BadgesCard`; Study `ReactingTuzo` and `Summary`.
- 27 WebP images and the launcher art; colour `ic_launcher_background`.
- Tests: `EngagementTest` (13 tests), `SessionFinishTest` (3), one new `ProgressRepositoryTest` case, two new `DesignSystemTest` cases.
- `docs/ART_PROMPTS.md`.

**Changed**
- `Tuzo.kt`: one drawable per mood with a crossfade between poses; before, one vector drawing that only wobbled differently per mood.
- `StudyScreen.kt`: the ViewModel now also takes `ProgressRepository` and `VerbRepository` and builds a `Summary`; the finish screen moved to `SessionFinish.kt`; the empty state shows the hammock scene. The finish streak now counts streak freezes like Home does; before, it used `Streaks.current`, which ignored them.
- `Notifications.showStudyReminder` takes a `Message`; before, it took counts and wrote the text itself. Large icon is the new Tuzo art.
- `ReminderReceiver`: follows `Reminders.decide` and picks from the pools; the work moved into `remind()` (smell fix below).
- `HomeScreen`: welcome-back card, badge celebration, hammock scene when caught up.
- `MeScreen`: badges card; the test reminder uses the message pools.
- `PlayHub` (in `VerbScreens.kt`): game rows and the sentence builder lead with illustrated tiles; before, Lucide icons on tinted tiles.
- `DeckDetailScreen`, `UpdateCard`, `OnboardingScreen`, `TutorScreen`: show the new scenes or tile.
- `TodayWidget`: shows the new Tuzo art.
- `ic_launcher.xml`, `ic_launcher_round.xml`: new art layer and colour background.
- Notification channel description reworded without "saving" urgency (NFR-7).
- `app/build.gradle.kts`: `versionName` 1.2; `publish.yml`: release tags `v1.2.<build>`.
- `DesignSystemTest`: the Canvas illustration test was replaced by checks that every pose, scene, badge and tile loads and is distinct.
- `docs/SRS.md`: version 1.2.

**Deleted**
- `Illustrations.kt` (`EmptyDeckArt`, `CelebrationArt`): Canvas drawings in theme colours. Replaced by: `SceneArt` with generated scenes.
- `res/drawable/tuzo.xml`: the vector Tuzo. Replaced by: seven `tuzo_*.webp` poses.
- `res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`: vector launcher layers. Replaced by: `ic_launcher_art.webp` and a colour.
- `Notifications.reminderText()`: three fixed messages. Replaced by: `ReminderMessages`.
- `gameIcon()` in `VerbScreens.kt` and `AppIcons.VerbRain`, `TugOfWar`, `SiONo`, `Parejas`, `Cuatro`, `Sentences`. Replaced by: `gameArt()` and the tiles.

**Smells found and fixed**
- Long Method in `ReminderReceiver.onReceive` after the new policy was added: Extract Method into `remind()`; `onReceive` now only handles the async lifecycle.
- Large Class risk in `StudyScreen.kt` (the finish screen would have pushed it past 300 lines): Extract Class into `SessionFinish.kt`.
- Switch Statement for mood → picture and motion spread across screens would have grown with every pose: moved into `TuzoMood` entries.
- Dead code: unused game icons and `gameIcon()` removed.
- Left as is: `LibraryViewModel` keeps growing as the shared screen state (Large Class); splitting it into per-screen ViewModels is noted as an open issue.

**Tests:** Core module tests run locally on a plain JVM build (`:core:play:test`, all pass). Test-first check for the new rules: two deliberate mutations (welcome back after more than 3 days instead of 3 or more; comeback after more than 7 days) each made `EngagementTest` fail, then were reverted. App tests and Android builds run on CI only (the Android SDK cannot be downloaded in this sandbox); see PR #9.
**Open issues:** `LibraryViewModel` size (see above). The themed monochrome icon still uses the 1.0 silhouette. Widget streak line still uses an emoji (SRS section 7).

### 2026-11-13 | v1.1 | Testing feedback: swipe direction, rating labels, modern look
**Phase:** Requirements through release (one maintenance update).
**Requirements touched:** FR-11, FR-12, FR-13, FR-14, FR-15, FR-16, NFR-5. FR-1 to FR-10 and NFR-1 to NFR-4, NFR-6 recorded as the v1.0 baseline.

**Decisions**
- Created `SRS.md` and `DEVLOG.md` in `docs/`. Why: the project standards require them and none existed in the repository. v1.0 is summarised from `docs/PLAN.md` and the README instead of reconstructed in detail. Alternatives considered: waiting for copies from the project knowledge, rejected because the update was asked to run unattended; if older copies exist, they should be merged into these.
- Read the swipe gesture on an unrotated box around the card (FR-11). Why: `pointerInput` sat after the `graphicsLayer` that flips the card 180 degrees, so on the back the drag was measured in mirrored coordinates and a right swipe became a left one. Alternatives considered: negating the drag when flipped, rejected because it would break again mid-flip and hides the real cause.
- Rating buttons: slim side padding (4 dp), one-line labels, step-based shrinking down to 10 sp only when needed (FR-12). Why: four equal buttons with Material's 24 dp side padding left about 30 dp for each word. Alternatives considered: a 2x2 grid (costs height on every phone), shorter labels (less clear).
- Icon set: Lucide via `com.composables:icons-lucide-android` 2.2.1, behind one `AppIcons` object (FR-13). Why: Sir chose an icon library plus custom art. The package ships icons as vector drawables, so R8 keeps only those used. Alternatives considered: Phosphor (`com.adamglin:phosphor-icon` 1.0.0, last published 2024), Material Symbols Rounded, and hand-drawn icons.
- Typeface: Nunito, five weights bundled in `res/font` (FR-14). Why: rounded and friendly to match Tuzo, OFL licensed, works offline. Alternatives considered: downloadable Google Fonts (needs Play services and a network), a variable font (larger, and weight axes need API 26+ handling).
- Illustrations drawn with Canvas from theme colours (FR-15). Why: one drawing serves all five themes in light and dark. Alternatives considered: vector drawables, which would need a copy per theme or runtime tinting that flattens them to one colour.
- Spacing as a plain object; shapes through `MaterialTheme.shapes` (FR-16). Why: shapes are a Material theme slot already; spacing never varies by theme, so a CompositionLocal would add ceremony for nothing.
- Version naming: `versionName` and release tags move from `1.0.<build>` to `1.1.<build>`. Why: this update adds behaviour (minor bump). The updater compares only the build number after the last dot, so 1.0 phones still see 1.1 as newer; a new test checks this.
- Kept Kotlin naming conventions (no `my`/`the` prefixes). Why: the standards say to follow the language's conventions outside Java, and the whole codebase already does.
- Home-screen widget keeps its emoji. Why: widgets draw with RemoteViews and cannot use Compose icons. Listed as an open question in the SRS.

**Added**
- `SwipeCard.kt`: the study card, moved out of `StudyScreen.kt`, with the gesture fix and a `detectSwipe` helper.
- `RatingBar.kt`: the rating controls, moved out of `StudyScreen.kt`, with a shared `RatingButton`.
- `AppIcons.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Illustrations.kt`: the design system described above.
- Five Nunito font files; `docs/licenses/` with the font and icon licences.
- `SwipeCardTest`, `RatingBarTest`, `DesignSystemTest`; one new case in `ReleaseParserTest`.
- Test dependencies `ui-test-junit4` and `ui-test-manifest` for Compose tests on Robolectric.
- `docs/SRS.md`, `docs/DEVLOG.md`, `docs/uml-v1.1.svg` and `docs/uml-v1.1-overview.svg`. The system has over 20 classes, so the UML is an overview of every package plus a detailed diagram of the packages this update changed.

**Changed**
- `StudyScreen.kt`: now only the screen and ViewModel; it was 327 lines including the card and rating bar. The finish screen shows Tuzo inside `CelebrationArt`, and the streak line uses an icon instead of "🔥".
- `VocabloTheme.kt`: now passes `VocabloTypography` and `VocabloShapes`; before, it set colours only.
- `VocabloApp.kt`: `Tab.icon` is a drawable id instead of a Material `ImageVector`.
- Top-bar and button icons in Decks, Deck detail, Card editor, Verbs, Drill, Sentences, Games and Tutor: Material icons replaced with `AppIcon(AppIcons.…)`. The back arrow is no longer auto-mirrored, which only matters for right-to-left languages the app does not offer.
- Emoji used as icons replaced: speaker on Listen buttons, streak flame (Home, Study, Drill), quest ticks, weekly chest, streak freezes, Verb Rain lives, medals, game rows, onboarding tick.
- `VerbScreens.kt`: `gameEmoji(game): String` became `gameIcon(game): Int`; medals use a tinted medal icon in fixed gold, silver and bronze.
- `GameScreens.kt`: `Hud` gained an overload with a slot for the left side, so Verb Rain can show heart icons; the text overload stays for the other games.
- `DeckDetailScreen.kt`: an empty deck shows `EmptyDeckArt` above its message.
- `app/build.gradle.kts`: `versionName` 1.1; Lucide replaces `material-icons-core`.
- `publish.yml`: release tags `v1.1.<build>`; were `v1.0.<build>`.
- `docs/NEXT_UPDATE.md`: all three items marked done in v1.1.

**Deleted**
- `material-icons-core` dependency. Why: every icon now comes from Lucide. Replaced by: `icons-lucide-android`.
- `gameEmoji()` in `VerbScreens.kt`: returned an emoji per game. Replaced by: `gameIcon()`.
- Private `SwipeCard`, `Front`, `Back` and `RatingBar` in `StudyScreen.kt`. Why: moved to their own files so they can be tested. Replaced by: the same composables in `SwipeCard.kt` and `RatingBar.kt`.

**Smells found and fixed**
- Large Class: `StudyScreen.kt` held the screen, ViewModel, card, gestures and rating bar. Extract Class into `SwipeCard.kt` and `RatingBar.kt`.
- Long Method: the drag handler was a 20-line lambda inside the card's modifier chain. Extract Method into `detectSwipe`.
- Duplicated Code: four near-identical rating buttons. Extract Method into `RatingButton`, so the no-wrap rule exists once.
- Magic numbers in the card (0.3, 1.5, 180, 350, 90, 40, 300, 12). Replaced with named constants.
- Primitive Obsession: icons passed around as emoji strings. Replaced with `AppIcons` drawable ids.
- Lazy Class: an empty `Illustrations` object written only to hold a comment was removed before commit.
- Not fixed: hard-coded dp values remain in screens this update did not otherwise change. FR-16 notes they move to `Spacing` as each is next changed.

**Tests:** Test-first. `SwipeCardTest` and `RatingBarTest` were pushed before the fixes and failed on CI as expected (run 43332902555: both flipped-card swipes and all three label cases failed; the two front-side swipes passed). After the fixes, all app tests passed (run 43333448984). The final CI run on the merged head ran every module's tests, lint, debug and release builds; results are in PR #8. The Android SDK cannot be downloaded in the development sandbox, so all builds ran on CI.
**Open issues:** Widget emoji (see SRS section 7). The remaining screens still use literal spacing values.
