package app.vocablo.srs

import app.vocablo.srs.Fsrs.Companion.DAY_MS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StudyQueueTest {

    private val now = 1_800_000_000_000L
    private fun due(id: Long, daysAgo: Int) = QueueItem(id, ReviewState(stability = 5.0, difficulty = 5.0, reps = 2, dueMillis = now - daysAgo * DAY_MS, lastReviewMillis = now - 10 * DAY_MS))
    private fun future(id: Long) = QueueItem(id, ReviewState(stability = 5.0, difficulty = 5.0, reps = 2, dueMillis = now + DAY_MS))
    private fun new(id: Long) = QueueItem(id, ReviewState())

    @Test
    fun `due cards come first, most overdue first, then new cards`() {
        val q = StudyQueue(listOf(new(1), due(2, 1), future(3), due(4, 5)), now)
        assertEquals(3, q.total)
        assertEquals(4L, q.current?.cardId)
    }

    @Test
    fun `new cards are capped per session`() {
        val q = StudyQueue((1L..50L).map(::new), now, newLimit = 20)
        assertEquals(20, q.total)
    }

    @Test
    fun `a card swiped left comes back before the session ends`() {
        val q = StudyQueue(listOf(new(1), new(2)), now)
        q.answer(Rating.AGAIN, now)
        assertEquals(2L, q.current?.cardId)
        q.answer(Rating.GOOD, now)
        assertEquals(1L, q.current?.cardId)
        q.answer(Rating.GOOD, now)
        assertTrue(q.isFinished)
        assertNull(q.current)
        assertEquals(2, q.reviewed)
    }

    @Test
    fun `answering returns the updated state to save`() {
        val q = StudyQueue(listOf(new(7)), now)
        val saved = q.answer(Rating.GOOD, now)
        assertEquals(7L, saved.cardId)
        assertEquals(1, saved.state.reps)
        assertTrue(saved.state.dueMillis > now)
    }
}
