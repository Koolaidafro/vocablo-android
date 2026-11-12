package app.vocablo.srs

/** A card waiting to be studied, identified by its id. */
data class QueueItem(val cardId: Long, val state: ReviewState)

/**
 * One study session. Due cards come first (most overdue first), then up to
 * [newLimit] new cards. Cards swiped left come back before the session ends.
 */
class StudyQueue(
    items: List<QueueItem>,
    nowMillis: Long,
    newLimit: Int = 20,
    private val fsrs: Fsrs = Fsrs(),
) {
    private val queue: ArrayDeque<QueueItem> = ArrayDeque(
        items.filter { !it.state.isNew && it.state.dueMillis <= nowMillis }.sortedBy { it.state.dueMillis } +
            items.filter { it.state.isNew }.take(newLimit),
    )

    val total: Int = queue.size
    var reviewed: Int = 0
        private set

    val current: QueueItem? get() = queue.firstOrNull()
    val remaining: Int get() = queue.size
    val isFinished: Boolean get() = queue.isEmpty()

    /** Rates the current card and returns its new state so the caller can save it. */
    fun answer(rating: Rating, nowMillis: Long): QueueItem {
        val item = queue.removeFirst()
        val updated = item.copy(state = fsrs.review(item.state, rating, nowMillis))
        if (rating == Rating.AGAIN) {
            queue.addLast(updated) // see it again at the end of this session
        } else {
            reviewed++
        }
        return updated
    }
}
