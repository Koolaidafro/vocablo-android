package app.vocablo.verbs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PracticeSettingsTest {

    @Test
    fun `locked tenses are never practiced, even if stored as enabled`() {
        val s = PracticeSettings(unlocked = Stage.PRESENT, enabledTenses = setOf(Tense.PRESENT, Tense.FUTURE))
        assertEquals(listOf(Tense.PRESENT), s.practiceTenses)
        assertEquals(s, s.toggle(Tense.PRETERITE)) // can't switch on a locked tense
    }

    @Test
    fun `unlocking a stage turns its tenses on, and the last tense can't be switched off`() {
        val s = PracticeSettings().unlockNext()
        assertEquals(Stage.PAST, s.unlocked)
        assertEquals(listOf(Tense.PRESENT, Tense.PRETERITE, Tense.IMPERFECT), s.practiceTenses)
        val onlyPresent = PracticeSettings()
        assertEquals(onlyPresent, onlyPresent.toggle(Tense.PRESENT))
        assertEquals(PracticeSettings().unlockNext().unlockNext().unlockNext().unlocked, Stage.FUTURE)
    }

    @Test
    fun `vosotros is left out unless asked for`() {
        assertFalse(Person.VOSOTROS in PracticeSettings().persons)
        assertTrue(Person.VOSOTROS in PracticeSettings(includeVosotros = true).persons)
    }

    @Test
    fun `the next stage is suggested after 80 percent of the last 40 answers are right`() {
        assertFalse(StageProgress.readyForNext(List(39) { true }))
        assertTrue(StageProgress.readyForNext(List(8) { false } + List(32) { true }))
        assertFalse(StageProgress.readyForNext(List(9) { false } + List(31) { true }))
    }
}

class DrillPlannerTest {

    private val verbs = VerbList.all.take(300)
    private val now = 1_000_000L

    @Test
    fun `new learners get the most common verbs first, present only`() {
        val plan = DrillPlanner.plan(PracticeSettings(), verbs, emptySet(), emptyMap(), now, newLimit = 3)
        assertEquals(VerbList.all.take(3).map { it.verb.infinitive }, plan.map { it.verb.verb.infinitive })
        assertTrue(plan.all { it.tense == Tense.PRESENT })
        assertEquals((1..3).toList(), plan.map { it.verb.rank })
    }

    @Test
    fun `due items come before new ones, and items not yet due wait`() {
        val due = DrillItem(verbs[50], Tense.PRESENT)
        val later = DrillItem(verbs[60], Tense.PRESENT)
        val schedules = mapOf(
            due.id to ItemSchedule(isNew = false, dueMillis = now - 1),
            later.id to ItemSchedule(isNew = false, dueMillis = now + 1),
        )
        val plan = DrillPlanner.plan(PracticeSettings(), verbs, emptySet(), schedules, now, newLimit = 2)
        assertEquals(due.id, plan.first().id)
        assertFalse(plan.any { it.id == later.id })
        assertEquals(3, plan.size)
    }

    @Test
    fun `tier and my-cards filters choose the verb pool`() {
        val tierOnly = DrillPlanner.plan(PracticeSettings(), verbs, emptySet(), emptyMap(), now, newLimit = 500)
        assertTrue(tierOnly.all { it.verb.rank <= 100 })
        val mine = DrillPlanner.plan(PracticeSettings(source = VerbSource.MY_CARDS), verbs, setOf("bailar"), emptyMap(), now, newLimit = 500)
        assertEquals(listOf("bailar"), mine.map { it.verb.verb.infinitive })
    }

    @Test
    fun `item ids round-trip`() {
        val id = DrillItem.idOf(1234, Tense.FUTURE)
        assertEquals(1234, DrillItem.rankOf(id))
        assertEquals(Tense.FUTURE, DrillItem.tenseOf(id))
    }
}

class DrillTest {

    private val tener = VerbList.byInfinitive.getValue("tener").verb

    @Test
    fun `choices hold the answer and three different wrong forms, including the regular-looking trap`() {
        repeat(20) { seed ->
            val c = Drill.choices(tener, Tense.PRETERITE, Person.YO, Random(seed))
            assertEquals(4, c.size)
            assertEquals(4, c.toSet().size)
            assertTrue("tuve" in c)
            assertTrue("tení" in c, "regularized form missing from $c")
        }
    }

    @Test
    fun `answers ignore case and spaces and flag a missing accent`() {
        assertEquals(Check.CORRECT, Drill.check("  Tuve ", "tuve"))
        assertEquals(Check.ACCENT_ONLY, Drill.check("comio", "comió"))
        assertEquals(Check.WRONG, Drill.check("tení", "tuve"))
    }

    @Test
    fun `explanations name the rule behind an irregular form`() {
        assertTrue(Explain.why(tener, Tense.PRESENT, Person.YO).single().contains("adds a g"))
        assertTrue(Explain.why(tener, Tense.PRESENT, Person.TU).single().contains("e → ie"))
        assertTrue(Explain.why(tener, Tense.PRETERITE, Person.EL).single().contains("tuv-"))
        assertTrue(Explain.why(tener, Tense.FUTURE, Person.NOSOTROS).single().contains("tendr-"))
        val hablar = VerbList.byInfinitive.getValue("hablar").verb
        assertTrue(Explain.why(hablar, Tense.PRESENT, Person.YO).isEmpty())
        val buscar = VerbList.byInfinitive.getValue("buscar").verb
        assertTrue(Explain.why(buscar, Tense.PRETERITE, Person.YO).single().contains("spelling"))
    }
}

class PlacementTest {
    @Test
    fun `scores map to a starting stage and tier`() {
        assertEquals(Placement.Result(Stage.PRESENT, Tier.TOP_100), Placement.score(List(8) { false }))
        assertEquals(Placement.Result(Stage.PAST, Tier.TOP_100), Placement.score(listOf(true, true, true, false, false, false, false, false)))
        assertEquals(Placement.Result(Stage.PAST, Tier.TOP_250), Placement.score(listOf(true, true, true, true, true, false, false, false)))
        assertEquals(Placement.Result(Stage.FUTURE, Tier.TOP_500), Placement.score(List(8) { true }))
    }

    @Test
    fun `every placement verb is in the list`() {
        Placement.items.forEach { assertTrue(it.infinitive in VerbList.byInfinitive, it.infinitive) }
    }
}
