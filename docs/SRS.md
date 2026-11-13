# Software Requirements Specification: Vocablo
Version 1.2 | Last updated 2026-11-13

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
| Tuzo | The app's mascot, a Mexican pocket gopher (*tuza*) wearing a jade bandana. |
| Badge | A one-time achievement shown on the Me screen, earned when a milestone is first reached. |
| Session | One run of swipe study, from the first card to the finish screen. |

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
| FR-15 | ~~The app shall show custom illustrations, in the current theme's colours, for an empty deck and for the end of a study session.~~ | could | removed in v1.2, replaced by FR-20 |
| FR-16 | Every screen shall take its corner radii and spacing from one shared set of design values. | could | implemented in v1.1 (applied to the study screens; other screens adopt it as they change) |
| FR-17 | Tuzo shall be drawn in one consistent character design with a separate pose for each mood: waving hello, ready, cheering, thinking, oops, sleeping and proud. | should | implemented in v1.2 |
| FR-18 | The launcher icon shall show the redesigned Tuzo, and the home-screen widget and notifications shall use the same Tuzo art. | should | implemented in v1.2 |
| FR-19 | Each game and the sentence builder shall show its own illustrated tile on the Play screen, and the tutor's set-up screen shall show the tutor tile. | could | implemented in v1.2 |
| FR-20 | The app shall show an illustrated Tuzo scene for an empty deck, nothing left to study, a finished session, an update ready, welcome back, the reminder offer and first-launch onboarding. | could | implemented in v1.2 |
| FR-21 | In swipe study, Tuzo shall cheer after a Good or Easy answer and give a kind "oops" after Again, then return to the ready pose. | should | implemented in v1.2 |
| FR-22 | The system shall award six badges (First steps, 7-day streak, 30-day streak, 100 words, Gold verb, Treasure), each once, when its milestone is first reached; shall celebrate newly earned badges on Home; and shall show every badge, earned or locked, on the Me screen. | should | implemented in v1.2 |
| FR-23 | The end-of-session screen shall show the XP earned in that session, how many of today's three quests are done, the streak, and one suggested next step. | must | implemented in v1.2 |
| FR-24 | When the app is opened after 3 or more days without study, Home shall show a welcome-back card, worded without blame, with a button to start studying. | should | implemented in v1.2 |
| FR-25 | Reminder notifications shall be written in Tuzo's voice, picked from at least 10 messages per kind, with no message repeated within 7 days. | should | implemented in v1.2 |
| FR-26 | After 7 days without the app being opened, the system shall send one "Tuzo misses you" reminder and then no reminders until the app is opened again. | must | implemented in v1.2 |
| FR-27 | On Android 13 and newer, after the learner's first finished study session, the system shall offer reminders on a Tuzo screen that explains them, shall keep working normally if declined, and shall not offer again on its own. | must | implemented in v1.2 |

## 4. Non-functional requirements

| ID | Requirement | Priority | Status |
| --- | --- | --- | --- |
| NFR-1 | All learning logic shall be covered by unit tests that run on a plain JVM. | must | implemented in v1.0 |
| NFR-2 | Every pull request shall pass CI (tests, lint, debug and release builds) before merging. | must | implemented in v1.0 |
| NFR-3 | Text and icons shall meet WCAG AA contrast in every theme, light and dark. | must | implemented in v1.0 |
| NFR-4 | Animations shall be replaced by still images when the phone's animations are turned off. | should | implemented in v1.0 |
| NFR-5 | Third-party assets shipped in the app shall have licences that allow redistribution, kept in `docs/licenses/`. | must | implemented in v1.1 |
| NFR-6 | Every behaviour change shall start with a failing test. | must | implemented in v1.0 |
| NFR-7 | Engagement features shall motivate without manipulating: no guilt or shame wording, no fake urgency, no loss of earned XP, badges or streak freezes, and no more than two notifications a day. | must | implemented in v1.2 |
| NFR-8 | Raster art shall be original to this app, with its generation prompts kept in `docs/ART_PROMPTS.md` and full-size sources in the project's shared art folder, and shall add no more than 2 MB to the APK. | should | implemented in v1.2 |

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
- **Generated raster art (FR-17 to FR-20).** Tuzo, the icon, badges, scenes and game tiles were generated with Gemini's Nano Banana Pro image model. Every prompt after the first attached the chosen master Tuzo as a reference image, which keeps the character the same across 20 images. Shipped as WebP in `drawable-nodpi`, cut out from a white background. Rejected: theme-coloured Canvas drawings (FR-15), which the owner found not engaging enough.
- **Mood owns its drawing (FR-17, FR-21).** Each `TuzoMood` entry names its pose drawable and motion, so adding a mood is one enum entry, not a new branch in every screen.
- **Badges as data (FR-22).** `Badge` is an enum whose entries carry their own condition over a `Milestones` snapshot, like `QuestKind`. Earned badges are stored as activity rows (kind `BADGE`, ref = badge), so the existing unique index pays each one once and no database migration is needed.
- **Stateless message rotation (FR-25).** The message is picked by the day number modulo the pool size, so with 10 or more messages none repeats inside 7 days, and nothing has to be stored.
- **Pure reminder policy (FR-26, NFR-7).** `Reminders.decide` turns the slot, study state and days since the app was opened into show, comeback or stay quiet. The receiver only acts on the answer, so every rule is unit-tested.

## 7. Open questions

- ~~Should the home-screen widget use the new icons?~~ Resolved in v1.2: the widget shows the new Tuzo picture (FR-18). Its streak line still uses an emoji, because RemoteViews cannot draw the Compose icon set.
- Should badges ever give XP? v1.2 keeps them as recognition only, so XP keeps tracking real practice.
- Should the remaining screens be moved onto the shared spacing values in one pass, or as each is next changed?

## 8. Revision history

| Version | Date | Change | IDs |
| --- | --- | --- | --- |
| 1.0 | 2026-11-13 | Document created; v1.0 features recorded from `docs/PLAN.md` and the README. | FR-1 to FR-10, NFR-1 to NFR-4, NFR-6 |
| 1.1 | 2026-11-13 | Added the testing feedback from 2026-11-12: swipe direction, rating labels, and the visual refresh. | FR-11 to FR-16, NFR-5 |
| 1.2 | 2026-11-13 | Art overhaul and engagement update: generated Tuzo poses, launcher icon, game tiles and scenes; Tuzo reactions, badges, session summary, welcome back, Tuzo-voice reminders with a comeback rule, and a reminder offer after the first session. FR-15 removed (replaced by FR-20). | FR-15, FR-17 to FR-27, NFR-7, NFR-8 |
