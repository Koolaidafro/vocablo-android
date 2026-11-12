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
