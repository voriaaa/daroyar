package ir.daroyar.app

import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.DrugIndex
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.drugs.InteractionChecker
import ir.daroyar.app.drugs.isIndexed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class DrugIndexTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun loadIndex() {
            // Unit tests run from the module directory.
            File("src/main/assets/drug_index.tsv").inputStream().use { DrugIndex.load(it) }
        }
    }

    private fun first(name: String) = DrugMatcher.match(name).firstOrNull()

    @Test
    fun indexIsLargeAndLeavesOutWhatTheDetailedReferenceKnows() {
        assertTrue("${DrugIndex.drugs.size} names", DrugIndex.drugs.size > 2500)
        assertTrue(DrugIndex.drugs.none { DrugMatcher.matchDetailed(it.fa).isNotEmpty() })
    }

    @Test
    fun detailedEntriesStillWin() {
        val metformin = first("متفورمین ۵۰۰")!!
        assertEquals("metformin", metformin.id)
        assertFalse(metformin.isIndexed)
    }

    @Test
    fun genericsFromTheIranianListAreRecognized() {
        val entacapone = first("قرص انتاکاپون ۲۰۰")!!
        assertTrue(entacapone.isIndexed)
        assertTrue(entacapone.use.contains("پارکینسون"))
        assertFalse(entacapone.isSupplement)
        assertEquals(null, entacapone.suggestedTimes)
        assertTrue(first("Midazolam")!!.isIndexed)
    }

    @Test
    fun herbalBrandsAreSupplementsAndMatchWholeWordsOnly() {
        val brand = first("قرص آلگوفیل")!!
        assertTrue(brand.isIndexed && brand.isSupplement)
        assertEquals(null, first("آلگوفیلاتی"))
    }

    @Test
    fun typingAProblemListsTheDrugsForIt() {
        val inflammation = DrugMatcher.byUse("التهاب")
        val ids = inflammation.map { it.id }
        assertTrue(ids.containsAll(listOf("ibuprofen", "naproxen", "diclofenac", "prednisolone")))
        assertFalse("acetaminophen" in ids) // a pain killer, but not anti-inflammatory
        assertTrue(inflammation.any { it.isIndexed })
        // Detailed entries come first.
        assertFalse(inflammation.first().isIndexed)

        val parasites = DrugMatcher.byUse("انگل").map { it.id }
        assertTrue(parasites.containsAll(listOf("mebendazole", "albendazole", "metronidazole")))
        assertTrue(DrugMatcher.byUse("ضد انگل").map { it.id }.containsAll(parasites.take(3)))

        // Short words count only as whole topic words.
        assertTrue("acetaminophen" in DrugMatcher.byUse("تب").map { it.id })
        assertTrue(DrugMatcher.byUse("کلس").isEmpty())
        // A drug's own name is not a topic.
        assertTrue(DrugMatcher.byUse("متفورمین").isEmpty())
        // Herbal brands' groups are only their maker, so they never come up for a problem.
        assertTrue(DrugMatcher.byUse("درد").none { DrugIndex.BRAND_TAG in it.tags })
    }

    @Test
    fun indexedDrugsTakeNoPartInInteractionChecks() {
        val meds = listOf(
            Medication(id = 1, name = "انتاکاپون", times = listOf(9 * 60)),
            Medication(id = 2, name = "وارفارین", times = listOf(18 * 60)),
        )
        assertTrue(InteractionChecker.check(meds).isEmpty())
    }
}
