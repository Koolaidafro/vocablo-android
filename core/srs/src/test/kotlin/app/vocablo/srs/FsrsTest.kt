package app.vocablo.srs

import app.vocablo.srs.Fsrs.Companion.DAY_MS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FsrsTest {

    private val fsrs = Fsrs()
    private val t0 = 1_800_000_000_000L

    @Test
    fun `recall is 90 percent exactly when a card comes due`() {
        for (s in listOf(1.0, 3.7, 20.0, 180.0)) {
            assertEquals(0.9, fsrs.retrievability(s, s), 1e-9)
        }
    }

    @Test
    fun `a new card swiped right comes back in a few days`() {
        val after = fsrs.review(ReviewState(), Rating.GOOD, t0)
        assertEquals(4, ((after.dueMillis - t0) / DAY_MS).toInt())
        assertEquals(1, after.reps)
        assertEquals(0, after.lapses)
    }

    @Test
    fun `a card swiped left comes back in ten minutes`() {
        val after = fsrs.review(ReviewState(), Rating.AGAIN, t0)
        assertEquals(t0 + 10 * 60_000L, after.dueMillis)
    }

    @Test
    fun `better ratings give longer intervals`() {
        val due = Rating.entries.map { fsrs.review(ReviewState(), it, t0).stability }
        assertTrue(due.zipWithNext().all { (a, b) -> a < b }, "stability should rise with rating: $due")
    }

    @Test
    fun `intervals grow with each successful review`() {
        var state = ReviewState()
        var now = t0
        val gaps = mutableListOf<Long>()
        repeat(5) {
            state = fsrs.review(state, Rating.GOOD, now)
            gaps += (state.dueMillis - now) / DAY_MS
            now = state.dueMillis
        }
        assertTrue(gaps.zipWithNext().all { (a, b) -> b > a }, "gaps should grow: $gaps")
    }

    @Test
    fun `forgetting a known card shrinks stability and counts a lapse`() {
        val learned = fsrs.review(fsrs.review(ReviewState(), Rating.GOOD, t0), Rating.GOOD, t0 + 4 * DAY_MS)
        val forgot = fsrs.review(learned, Rating.AGAIN, learned.dueMillis)
        assertTrue(forgot.stability < learned.stability)
        assertEquals(1, forgot.lapses)
        assertTrue(forgot.difficulty > learned.difficulty)
    }

    @Test
    fun `difficulty stays between 1 and 10`() {
        var state = ReviewState()
        repeat(30) { i -> state = fsrs.review(state, Rating.AGAIN, t0 + i * DAY_MS) }
        assertTrue(state.difficulty in 1.0..10.0)
        repeat(30) { i -> state = fsrs.review(state, Rating.EASY, t0 + (40 + i) * DAY_MS) }
        assertTrue(state.difficulty in 1.0..10.0)
    }
}
