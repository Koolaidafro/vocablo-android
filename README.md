# Vocablo

A native Android app for learning **Latin American Spanish** (or the Spanish of your region): unlimited flashcards stored on your phone, swipe study, verb conjugation drills, mini-games, a sentence builder built from your own words, and an optional AI tutor that uses your own Claude, ChatGPT or Grok key.

Built with Kotlin, Jetpack Compose and Material 3, test-driven from the first commit.

## What's in the app

- **Cards and swipe study.** Decks with unlimited cards, FSRS spaced repetition, swipe right for "got it" and left for "again". Dictionary lookup while adding a card, a duplicate warning across every deck, and a hint when you type a conjugated form ("comí" is a form of comer). CSV and Anki text import, CSV export, full backup and restore.
- **Verbs.** The 2,081 most common Spanish verbs, ranked by frequency, in tiers of 100 to 2,000. Present, then preterite and imperfect, then future and conditional unlock in stages, so you are never quizzed on a tense you haven't reached. Multiple choice or typed answers, accent help, and a short "why" for every irregular form.
- **Sentence builder and games.** Build sentences from your own nouns and verbs. Five offline games: Verb Rain, Tug of War, Sí o No, Parejas and Cuatro.
- **Motivation.** XP and levels, streaks with earned freezes, three daily quests, a weekly chest, per-verb medals, and Tuzo the gopher cheering you on. Reminders at most twice a day, and never once you've studied.
- **AI tutor (optional).** Role-play eight everyday situations with your own API key. Tuzo speaks your region's Spanish and corrects mistakes as you go. Keys are encrypted with the phone's keystore and never backed up.
- **Updates.** Install the APK from the newest GitHub release once; after that the app offers each new version on its Home screen.
- **Everywhere else.** Listen buttons with a voice for your region, a home-screen widget, side navigation on tablets and foldables, five color themes with light and dark modes, and still images when the phone's animations are turned off.

## Modules

| Module | What it does |
| --- | --- |
| `core/verbs` | Conjugation engine, the ranked verb list, practice stages, drill planning, explanations and placement. |
| `core/srs` | FSRS scheduling and the study queue. |
| `core/cards` | Card text rules, duplicate checks, CSV and backup formats, streaks. |
| `core/dictionary` | Offline starter dictionary and the Wiktionary parser. |
| `core/play` | XP, quests, chest, streak freezes, medals, reminder timing, sentence builder and game rules. |
| `core/tutor` | AI provider requests and replies, scenarios and region-aware tutor instructions. |
| `core/design` | The five themes and their contrast checks. |
| `app` | The Android app: Room database, settings, screens, reminders, widget, speech and key storage. |

All `core` modules are plain Kotlin and test on any JVM.

## Build and test

Requires JDK 17 or newer and the Android SDK (API 36).

```bash
./gradlew :core:verbs:test :core:srs:test :core:cards:test :core:dictionary:test :core:play:test :core:tutor:test :core:design:test
./gradlew :app:testGithubDebugUnitTest   # database and app logic on Robolectric
./gradlew :app:installGithubDebug        # install on a connected phone
./gradlew :app:bundlePlayRelease         # Play Store bundle (see docs/RELEASE.md for signing)
```

## How we work

Every feature starts with a failing test. See [CONTRIBUTING.md](CONTRIBUTING.md). Settled product decisions are in [docs/PLAN.md](docs/PLAN.md); release steps in [docs/RELEASE.md](docs/RELEASE.md); the privacy policy in [docs/PRIVACY.md](docs/PRIVACY.md).

## Data and credits

- Dictionary text from [Wiktionary](https://en.wiktionary.org) (CC BY-SA 4.0), looked up online when a word isn't in the starter dictionary.
- Verb frequency ranking from [FrequencyWords](https://github.com/hermitdave/FrequencyWords) by Hermit Dave (CC BY-SA 4.0).
- Starter dictionary examples and Tuzo were made for Vocablo.

## License

Apache 2.0. See [LICENSE](LICENSE).
