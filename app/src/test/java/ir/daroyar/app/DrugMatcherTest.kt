package ir.daroyar.app

import ir.daroyar.app.drugs.DrugDatabase
import ir.daroyar.app.drugs.DrugMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrugMatcherTest {

    // The detailed reference only, so results don't depend on whether another test loaded the name index.
    private fun ids(name: String) = DrugMatcher.matchDetailed(name).map { it.id }

    @Test
    fun everyDrugIsFoundByItsOwnNames() {
        DrugDatabase.drugs.forEach { drug ->
            assertEquals("Persian name of ${drug.id}", listOf(drug.id), ids(drug.fa))
            assertEquals("English name of ${drug.id}", listOf(drug.id), ids(drug.en))
        }
    }

    @Test
    fun ignoresDosesUnitsAndLetterVariants() {
        assertEquals(listOf("metformin"), ids("قرص متفورمین ۵۰۰"))
        assertEquals(listOf("atorvastatin"), ids("Atorvastatin 20mg"))
        assertEquals(listOf("atorvastatin"), ids("آتورواستاتين")) // Arabic yeh
        assertEquals(listOf("escitalopram"), ids("اس سیتالوپرام ۱۰"))
        assertEquals(listOf("escitalopram"), ids("اس‌سیتالوپرام"))
        assertEquals(listOf("citalopram"), ids("سیتالوپرام"))
        assertEquals(listOf("esomeprazole"), ids("نکسیوم ۴۰"))
    }

    @Test
    fun recognizesBrandNames() {
        assertEquals(listOf("sertraline"), ids("زولوفت"))
        assertEquals(listOf("ibuprofen"), ids("ژلوفن"))
        assertEquals(listOf("metformin"), ids("Glucophage"))
        assertEquals(listOf("levothyroxine"), ids("لوتیروکسین ۱۰۰"))
    }

    @Test
    fun findsEveryDrugInCombinations() {
        assertEquals(listOf("acetaminophen", "codeine"), ids("استامینوفن کدئین"))
        assertEquals(listOf("losartan", "hydrochlorothiazide"), ids("لوزارتان اچ"))
        assertEquals(listOf("calcium", "vitamin_d"), ids("کلسیم د"))
        assertEquals(listOf("iron", "folic_acid"), ids("فروفولیک"))
    }

    @Test
    fun shortNamesOnlyMatchWholeWords() {
        assertEquals(listOf("iron"), ids("قرص آهن"))
        assertEquals(listOf("zinc"), ids("قرص روی"))
        assertEquals(listOf("oral_contraceptive"), ids("ال دی"))
        assertEquals(listOf("pyridoxine"), ids("ویتامین ب۶"))
        assertTrue(ids("ویتامین ب۳").isEmpty())
        assertTrue(ids("قرص ناشناخته").isEmpty())
        assertTrue(ids("").isEmpty())
    }

    @Test
    fun recognizesSupplementsAndTellsVitaminsApart() {
        assertEquals(listOf("vitamin_b12"), ids("ویتامین ب۱۲ ۱۰۰۰"))
        assertEquals(listOf("vitamin_b12"), ids("B12"))
        assertEquals(listOf("b_complex"), ids("ویتامین ب کمپلکس"))
        assertEquals(listOf("vitamin_d"), ids("ویتامین د۳ ۵۰۰۰۰"))
        assertEquals(listOf("vitamin_e"), ids("ویتامین ای ۴۰۰"))
        assertEquals(listOf("vitamin_a"), ids("ویتامین آ"))
        assertEquals(listOf("omega3"), ids("امگا ۳ ۱۰۰۰"))
        assertEquals(listOf("coq10"), ids("کوآنزیم کیو ۱۰"))
        assertEquals(listOf("calcium", "magnesium", "zinc"), ids("کلسیم منیزیم زینک"))
        assertTrue(DrugMatcher.match("ملاتونین ۳").single().isSupplement)
        assertTrue(DrugMatcher.match("متفورمین").none { it.isSupplement })
    }

    @Test
    fun similarNamesInTheLargerDatabaseStayApart() {
        assertEquals(listOf("metoclopramide"), ids("متوکلوپرامید"))
        assertEquals(listOf("loperamide"), ids("لوپرامید"))
        assertEquals(listOf("desloratadine"), ids("دسلوراتادین"))
        assertEquals(listOf("levocetirizine"), ids("لووستیریزین"))
        assertEquals(listOf("valacyclovir"), ids("والاسیکلوویر"))
        assertEquals(listOf("methylprednisolone"), ids("متیل پردنیزولون ۴"))
        assertEquals(listOf("ciprofloxacin"), ids("Ciprofloxacin 500"))
        assertEquals(listOf("ofloxacin"), ids("افلوکساسین"))
        assertEquals(listOf("thiamine"), ids("ویتامین ب۱"))
        assertEquals(listOf("fluticasone_salmeterol"), ids("فلوتیکازون سالمترول"))
        assertEquals(listOf("terazosin"), ids("ترازوسین"))
        assertEquals(listOf("trazodone"), ids("ترازودون"))
        assertEquals(listOf("sitagliptin", "metformin"), ids("جانومت"))
        assertEquals(listOf("orphenadrine", "acetaminophen"), ids("نورژزیک"))
    }

    @Test
    fun suggestsWhileTyping() {
        assertTrue(DrugMatcher.suggest("متف").any { it.id == "metformin" })
        assertTrue(DrugMatcher.suggest("آموکسی").map { it.id }.containsAll(listOf("amoxicillin", "co_amoxiclav")))
        assertEquals("metformin", DrugMatcher.suggest("متف").first().id)
        // From the very first letter, Persian names that start with it come first.
        val m = DrugMatcher.suggest("م")
        assertEquals(8, m.size)
        assertTrue(m.joinToString { it.fa }, m.all { DrugMatcher.normalize(it.fa).startsWith("م") })
        // Brand and English names work too.
        assertTrue(DrugMatcher.suggest("zol").any { it.id == "sertraline" })
        assertTrue(DrugMatcher.suggest("زولو").any { it.id == "sertraline" })
        assertTrue(DrugMatcher.suggest("").isEmpty())
    }
}
