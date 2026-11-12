# Vocablo

A native Android app for learning **Latin American Spanish**: unlimited flashcards stored on your phone, swipe study, verb conjugation drills and mini-games, a sentence builder, and an optional AI tutor that uses your own Claude, ChatGPT or Grok API key.

Built with Kotlin, Jetpack Compose and Material 3, test-driven from the first commit.

> Status: **phase 1 (foundation)**. The verb engine and theme system are in place; the app shows the five-tab shell and live theme switching.

## What's here

| Module | What it does |
| --- | --- |
| `core/verbs` | Rules-plus-exceptions conjugation engine (present, preterite, imperfect, future, conditional) and reverse lookup from a conjugated form to its infinitive. Pure Kotlin. |
| `core/design` | The five color themes (Jade, Océano, Atardecer, Lavanda, Grafito), each with a white-based light mode and a dark mode, plus WCAG contrast checks. Pure Kotlin. |
| `app` | The Android app: Compose UI, five-tab shell, theme switching. |

## Build and test

Requires JDK 17 or newer and the Android SDK (API 36).

```bash
./gradlew :core:verbs:test :core:design:test   # fast unit tests, no Android needed
./gradlew :app:assembleDebug                   # build the APK
./gradlew :app:installDebug                    # install on a connected phone
```

## How we work

Every feature starts with a failing test. See [CONTRIBUTING.md](CONTRIBUTING.md).

The product plan, UI mockups and architecture decisions are summarized in [docs/PLAN.md](docs/PLAN.md).

## Data and credits

Planned open data sources, credited in the app's About screen:

- Dictionary entries from [Wiktionary](https://en.wiktionary.org) (CC BY-SA)
- Example sentences from [Tatoeba](https://tatoeba.org) (CC BY 2.0 FR)

## License

Apache 2.0. See [LICENSE](LICENSE).
