# Next update: feedback backlog

Feedback collected while testing the current release. Nothing here is
implemented yet. When Sir says "execute", every open item is implemented
test-first, the SRS and DEVLOG are updated, and the update ships through a PR
to `main` (which publishes the release).

Status values: **open** (logged, not started), **done in vX.Y**.

## Items

### 1. Swipe direction flips after the card is flipped
- **Reported:** 2026-11-12
- **Status:** open
- **What happens:** In Study, swiping right means "Got it" and left means
  "Again" on the front of the card. After tapping to flip the card, the
  directions are reversed.
- **What we want:** The swipe direction stays the same whichever side is
  showing. Right is always "Got it", left is always "Again".
- **Likely cause (not yet verified by a test):** In
  `app/src/main/kotlin/app/vocablo/ui/study/StudyScreen.kt`, `SwipeCard` puts
  `pointerInput` after the `graphicsLayer` that applies `rotationY = 180f`.
  Pointer input then reads drag deltas in the mirrored layer, so the sign of
  the drag is inverted once the card is flipped.
- **Planned fix:** Detect the drag in unrotated screen space (move the gesture
  handling outside the rotated layer) and add a UI test that swipes right on a
  flipped card and expects a "Got it" answer.

### 2. Rating button labels break mid-word
- **Reported:** 2026-11-12 (with a screenshot from a phone)
- **Status:** open
- **What happens:** On the Study screen, after the card is flipped, the four
  rating buttons at the bottom are too narrow for their labels. The text wraps
  in the middle of the word: "Aga / in", "Har / d", "Goo / d", "Eas / y".
- **What we want:** Each label fits on one line and is fully readable, on a
  normal phone and with a larger system font size.
- **Likely cause (not yet verified by a test):** `RatingBar` in
  `app/src/main/kotlin/app/vocablo/ui/study/StudyScreen.kt` splits the row into
  four equal buttons (`weight(1f)`) and keeps Material's default button padding
  (24dp left and right). That leaves too little room for the text, and the
  `Text` is allowed to wrap.
- **Planned fix:** Give the rating buttons less side padding, keep each label
  on one line (shrinking it a little only if it still cannot fit), and add a UI
  test that checks every label is drawn on a single line at a narrow width and
  a large font scale.
