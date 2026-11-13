# Development Log: Vocablo

## Current state
Version: 1.1 (released) | Last updated: 2026-11-13

### Asset inventory

Only assets added or changed since this log began are listed one by one. Everything else from v1.0 is grouped by module; see the README's module table.

| Asset | Type | Purpose | Added in | Last changed in |
| --- | --- | --- | --- | --- |
| `core/*` modules | Kotlin code and tests | Learning logic: verbs, scheduling, cards, dictionary, games, tutor, themes | 1.0 | 1.0 |
| `app/` (other screens, data, widget, speech, updates) | Kotlin code | The Android app | 1.0 | 1.1 (icon and emoji swaps) |
| `app/.../ui/study/StudyScreen.kt` | Kotlin code | Study session screen and its ViewModel | 1.0 | 1.1 |
| `app/.../ui/study/SwipeCard.kt` | Kotlin code | The flippable, swipeable study card | 1.1 | 1.1 |
| `app/.../ui/study/RatingBar.kt` | Kotlin code | "Show answer" and the four rating buttons | 1.1 | 1.1 |
| `app/.../ui/theme/AppIcons.kt` | Kotlin code | The app's icon set by meaning, plus `AppIcon`, `IconTile`, `IconLabel` | 1.1 | 1.1 |
| `app/.../ui/theme/Type.kt` | Kotlin code | Nunito family and the type scale | 1.1 | 1.1 |
| `app/.../ui/theme/Shape.kt` | Kotlin code | Shared corner radii | 1.1 | 1.1 |
| `app/.../ui/theme/Spacing.kt` | Kotlin code | Shared spacing steps | 1.1 | 1.1 |
| `app/.../ui/theme/VocabloTheme.kt` | Kotlin code | Applies colours, type and shapes | 1.0 | 1.1 |
| `app/.../ui/components/Illustrations.kt` | Kotlin code | Empty-deck and celebration art | 1.1 | 1.1 |
| `app/src/main/res/font/nunito_*.ttf` | Font files (5) | Regular, Medium, SemiBold, Bold, ExtraBold | 1.1 | 1.1 |
| `app/src/test/.../ui/study/SwipeCardTest.kt` | Test | Swipe direction on both sides (FR-11) | 1.1 | 1.1 |
| `app/src/test/.../ui/study/RatingBarTest.kt` | Test | Rating labels on one line (FR-12) | 1.1 | 1.1 |
| `app/src/test/.../ui/theme/DesignSystemTest.kt` | Test | Type, shapes, icons, illustrations (FR-13 to FR-16) | 1.1 | 1.1 |
| `app/src/test/.../update/ReleaseParserTest.kt` | Test | Release tag parsing | 1.0 | 1.1 |
| `docs/SRS.md` | Document | Requirements | 1.1 | 1.1 |
| `docs/DEVLOG.md` | Document | This log | 1.1 | 1.1 |
| `docs/NEXT_UPDATE.md` | Document | Testing feedback waiting for the next update | 1.1 | 1.1 |
| `docs/uml-v1.1.svg` | Diagram | Class diagram with members: study screen and design system | 1.1 | 1.1 |
| `docs/uml-v1.1-overview.svg` | Diagram | Class diagram overview: every module and package, types by name | 1.1 | 1.1 |
| `docs/licenses/OFL-Nunito.txt`, `docs/licenses/ISC-Lucide.txt` | Licence texts | Redistribution terms for the font and icons (NFR-5) | 1.1 | 1.1 |
| `.github/workflows/publish.yml` | CI config | Publishes a signed APK on every merge to `main` | 1.0 | 1.1 |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Build config | Dependencies and version | 1.0 | 1.1 |

## Log

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
