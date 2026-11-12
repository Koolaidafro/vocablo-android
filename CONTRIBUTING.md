# Contributing

## Test-driven, always

1. Write a failing test that describes the behavior.
2. Write the smallest code that makes it pass.
3. Clean up with the tests green.

Pull requests without tests for new behavior are not merged. CI must be green.

## Where code goes

- **Pure Kotlin first.** Learning logic (conjugation, scheduling, grading, duplicate detection) lives in `core/*` modules with no Android imports, so its tests run in seconds.
- **Android only where needed.** `app` holds Compose UI, ViewModels and platform code.

## Verb engine rules

- Each verb stores its pattern (stem change, yo form, strong preterite stem, future stem). The engine never guesses a pattern from spelling, because look-alike verbs (sentar and sentir) conjugate differently.
- Anything the rules get wrong goes in `overrides`, and gets a test.
- Expected forms follow the RAE's current spelling (rio, guie, dio without accents).
- Reference datasets with non-commercial licenses may be used to check the engine locally but are never committed or shipped.

## Branches and commits

- Branch from `main`, open a pull request, squash merge.
- Keep pull requests focused on one feature.
