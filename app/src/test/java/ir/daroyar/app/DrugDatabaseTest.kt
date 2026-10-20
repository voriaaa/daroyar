package ir.daroyar.app

import ir.daroyar.app.drugs.DrugDatabase
import ir.daroyar.app.drugs.Severity
import ir.daroyar.app.drugs.interactionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class DrugDatabaseTest {

    @Test
    fun idsAreUniqueAndTextsFilledIn() {
        val ids = DrugDatabase.drugs.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        DrugDatabase.drugs.forEach {
            assertTrue(it.id, it.fa.isNotBlank() && it.use.isNotBlank() && it.about.isNotBlank() && it.timing.isNotBlank())
            it.suggestedTimes?.let { times ->
                assertTrue(it.id, times.isNotEmpty() && times.all { m -> m in 0 until 24 * 60 })
                assertEquals(it.id, times.sorted(), times)
            }
        }
    }

    @Test
    fun everyRuleTagExistsOnSomeDrug() {
        val known = DrugDatabase.drugs.flatMap { it.allTags }.toSet()
        interactionRules.forEach { rule ->
            (rule.left + rule.right).forEach { tag -> assertTrue("unknown tag '$tag' in: ${rule.text}", tag in known) }
        }
    }

    @Test
    fun timingRulesHaveSpacing() {
        interactionRules.filter { it.severity == Severity.TIMING }.forEach {
            assertTrue(it.text, it.before > 0 || it.after > 0)
        }
    }

    @Test
    fun everySupplementSuggestsATime() {
        val supplements = DrugDatabase.drugs.filter { it.isSupplement }
        assertTrue(supplements.size >= 20)
        supplements.forEach { assertTrue("${it.id} has no suggested time", !it.suggestedTimes.isNullOrEmpty()) }
    }

    @Test
    fun databaseIsLargeAndMostDrugsHaveABestTime() {
        val drugs = DrugDatabase.drugs
        assertTrue("only ${drugs.size} drugs", drugs.size >= 300)
        val withTimes = drugs.count { it.suggestedTimes != null }
        // The rest are "as needed" drugs (pain, migraine, cold, sleep) or prescriber-specific (insulin).
        assertTrue("$withTimes of ${drugs.size} have a best time", withTimes * 100 / drugs.size >= 85)
        drugs.filter { it.weekly }.forEach { assertEquals(it.id, 1, it.suggestedTimes?.size) }
    }

    @Test
    fun combosPointAtRealDrugs() {
        DrugDatabase.combos.values.flatten().forEach { assertTrue(it, it in DrugDatabase.byId) }
    }

    @Test
    fun weeklyDrugsWarnUnlessTakenOnOneDay() {
        val methotrexate = DrugDatabase.byId.getValue("methotrexate")
        assertTrue(methotrexate.weekly && DrugDatabase.byId.getValue("alendronate").weekly)
        assertFalse(DrugDatabase.byId.getValue("metformin").weekly)

        assertTrue(methotrexate.tooFrequent(emptySet())) // every day
        assertTrue(methotrexate.tooFrequent(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
        assertFalse(methotrexate.tooFrequent(setOf(DayOfWeek.SATURDAY)))
        assertFalse(DrugDatabase.byId.getValue("metformin").tooFrequent(emptySet()))
    }
}
