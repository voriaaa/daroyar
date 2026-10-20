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
    fun indexedDrugsTakeNoPartInInteractionChecks() {
        val meds = listOf(
            Medication(id = 1, name = "انتاکاپون", times = listOf(9 * 60)),
            Medication(id = 2, name = "وارفارین", times = listOf(18 * 60)),
        )
        assertTrue(InteractionChecker.check(meds).isEmpty())
    }
}
