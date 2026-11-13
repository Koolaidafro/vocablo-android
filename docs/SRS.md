# Software Requirements Specification: Vocablo
Version 1.1 | Last updated 2026-11-13

## 1. Introduction

**Purpose.** Vocablo is an Android app for learning Spanish vocabulary and verbs, with a focus on Latin American Spanish. This document says what the app must do. How it does it is only in section 6.

**Scope.** In scope: flashcards with spaced repetition, verb conjugation practice, sentence building, offline mini-games, motivation features, an optional AI tutor that uses the learner's own API key, and self-updates from GitHub releases. Out of scope: accounts and cloud sync, ads, in-app purchases, and any server run by the developer.

**Definitions.**

| Term | Meaning |
| --- | --- |
| Card | One Spanish item with its English meaning, optional example and region note. |
| Deck | A named group of cards. |
| Swipe study | Studying cards one at a time: tap to flip, swipe right if you knew it, left if you didn't. |
| Rating | The learner's answer on a card: Again, Hard, Good or Easy. |
| FSRS | The spaced-repetition scheduler that decides when a card is due again. |
| Front / back | The Spanish side and the English side of a card. |

## 2. Overall description

**Users.** One learner per phone, aged 13 and over, from complete beginner upwards.

**Environment.** Android 8.0 (API 26) and newer; phones, tablets and foldables. Works fully offline except dictionary lookups, the AI tutor and update checks.

**Constraints.**
- All learning data stays on the phone.
- The GitHub build updates itself; the Play Store build must not.
- AI keys are encrypted with the Android Keystore and never backed up.

**Assumptions.**
- Learners use the system font size setting, so layouts must survive at least 130% text.
- The narrowest supported phone is 320 dp wide (about 288 dp of usable row width).

## 3. Functional requirements

Requirements FR-1 to FR-10 summarise version 1.0, which was built before this document existed (see `docs/PLAN.md`).

| ID | Requirement | Priority | Status |
| --- | --- | --- | --- |
| FR-1 | The system shall let the learner create, rename and delete decks and add unlimited cards, warning on duplicates across all decks. | must | implemented in v1.0 |
| FR-2 | The system shall schedule card reviews with FSRS and offer swipe study, where a right swipe rates Good and a left swipe rates Again. | must | implemented in v1.0 |
| FR-3 | The system shall look up words in an offline starter dictionary and, when online, in Wiktionary. | should | implemented in v1.0 |
| FR-4 | The system shall import CSV and Anki text, export CSV, and back up and restore all data. | should | implemented in v1.0 |
| FR-5 | The system shall drill conjugations for the 2,081 most common verbs, unlocking tenses in stages and never quizzing an unchecked tense. | must | implemented in v1.0 |
| FR-6 | The system shall offer a sentence builder and five offline games (Verb Rain, Tug of War, Sí o No, Parejas, Cuatro). | should | implemented in v1.0 |
| FR-7 | The system shall award XP and levels, track streaks with freezes, set three daily quests and a weekly chest, and award per-verb medals. | should | implemented in v1.0 |
| FR-8 | The system shall offer an optional AI tutor for role-play using the learner's own Claude, OpenAI, Grok or compatible key. | could | implemented in v1.0 |
| FR-9 | The system shall read Spanish aloud in a voice for the learner's region and offer a home-screen widget. | should | implemented in v1.0 |
| FR-10 | The GitHub build shall offer each newer GitHub release on the Home screen and install it over the current version. | must | implemented in v1.0 |
| FR-11 | In swipe study, a right swipe shall rate the card Good and a left swipe shall rate it Again, whichever side of the card is showing. | must | implemented in v1.1 |
| FR-12 | Each rating button label (Again, Hard, Good, Easy) shall show on one line, uncut, on a 288 dp row at 115% text and on a 328 dp row at 100% and 130% text. | must | implemented in v1.1 |
| FR-13 | Every icon in the app's screens shall come from one consistent icon set, in the theme's colours, in place of emoji used as icons. | should | implemented in v1.1 |
| FR-14 | The app shall display its text in a bundled typeface that is the same on every phone and needs no download. | should | implemented in v1.1 |
| FR-15 | The app shall show custom illustrations, in the current theme's colours, for an empty deck and for the end of a study session. | could | implemented in v1.1 |
| FR-16 | Every screen shall take its corner radii and spacing from one shared set of design values. | could | implemented in v1.1 (applied to the study screens; other screens adopt it as they change) |

## 4. Non-functional requirements

| ID | Requirement | Priority | Status |
| --- | --- | --- | --- |
| NFR-1 | All learning logic shall be covered by unit tests that run on a plain JVM. | must | implemented in v1.0 |
| NFR-2 | Every pull request shall pass CI (tests, lint, debug and release builds) before merging. | must | implemented in v1.0 |
| NFR-3 | Text and icons shall meet WCAG AA contrast in every theme, light and dark. | must | implemented in v1.0 |
| NFR-4 | Animations shall be replaced by still images when the phone's animations are turned off. | should | implemented in v1.0 |
| NFR-5 | Third-party assets shipped in the app shall have licences that allow redistribution, kept in `docs/licenses/`. | must | implemented in v1.1 |
| NFR-6 | Every behaviour change shall start with a failing test. | must | implemented in v1.0 |

## 5. External interfaces

- **Wiktionary** over HTTPS for dictionary lookups (FR-3).
- **AI providers** chosen by the learner, over HTTPS (FR-8).
- **GitHub Releases API** for update checks and APK downloads (FR-10).
- **Android**: text-to-speech, notifications, app widget, package installer.

## 6. Design decisions

- **MVC (Compose screens, ViewModels, repositories).** The UI never touches the database directly, so logic is testable without a screen.
- **Plain-Kotlin core modules.** Learning rules live in `core/*` with no Android imports, so their tests run in seconds (NFR-1).
- **AI providers as data (FR-8).** Each provider is an enum entry with its request format, so adding one is a single entry. The key cipher sits behind an interface so tests can swap in a fake.
- **Swipe detected outside the flip rotation (FR-11).** The drag is read on an unrotated box around the card; reading it on the rotated card mirrored it after a flip.
- **Rating labels never wrap (FR-12).** Slim button padding plus a one-line label that shrinks a step at a time only if it still cannot fit. Rejected: a 2×2 grid, which costs height on every phone to fix a problem only very narrow ones have.
- **Lucide icons behind one `AppIcons` object (FR-13).** Screens name icons by meaning, so the set can be swapped in one file. Lucide was the learner's choice over Material Symbols and hand-drawn icons.
- **Bundled Nunito (FR-14).** Shipped in the APK rather than downloaded, because the app works offline and on phones without Play services.
- **Canvas-drawn illustrations (FR-15).** Drawn from theme colours, so one drawing serves five themes in light and dark.
- **Design tokens (FR-16).** Shapes go through Material's theme; spacing is a plain object because it never changes with the theme.

## 7. Open questions

- Should the home-screen widget use the new icons? Widgets use RemoteViews, which cannot draw Compose icons, so it still shows emoji.
- Should the remaining screens be moved onto the shared spacing values in one pass, or as each is next changed?

## 8. Revision history

| Version | Date | Change | IDs |
| --- | --- | --- | --- |
| 1.0 | 2026-11-13 | Document created; v1.0 features recorded from `docs/PLAN.md` and the README. | FR-1 to FR-10, NFR-1 to NFR-4, NFR-6 |
| 1.1 | 2026-11-13 | Added the testing feedback from 2026-11-12: swipe direction, rating labels, and the visual refresh. | FR-11 to FR-16, NFR-5 |
