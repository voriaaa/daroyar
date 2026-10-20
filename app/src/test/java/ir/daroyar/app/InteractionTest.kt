package ir.daroyar.app

import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.InteractionChecker
import ir.daroyar.app.drugs.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionTest {

    private var nextId = 1L
    private fun med(name: String, vararg times: Int) =
        Medication(id = nextId++, name = name, times = times.toList().ifEmpty { listOf(9 * 60) })

    private fun check(vararg meds: Medication) = InteractionChecker.check(meds.toList())

    @Test
    fun warfarinWithIbuprofenIsSerious() {
        val findings = check(med("وارفارین"), med("بروفن ۴۰۰"))
        assertEquals(Severity.HIGH, findings.first().severity)
    }

    @Test
    fun unrelatedDrugsGiveNothing() {
        assertTrue(check(med("متفورمین"), med("آتورواستاتین"), med("آملودیپین")).isEmpty())
    }

    @Test
    fun unknownDrugsGiveNothing() {
        assertTrue(check(med("قرص ناشناخته"), med("وارفارین")).isEmpty())
    }

    @Test
    fun specificHighWarningHidesGenericModerateOnes() {
        // Opioid + benzodiazepine is HIGH; the generic "both are sedating" MODERATE must not repeat it.
        val findings = check(med("ترامادول"), med("آلپرازولام"))
        assertEquals(1, findings.size)
        assertEquals(Severity.HIGH, findings.single().severity)
    }

    @Test
    fun serotoninSyndromeRisk() {
        assertEquals(Severity.HIGH, check(med("سرترالین"), med("ترامادول")).first().severity)
    }

    @Test
    fun statinsWithClarithromycinDependOnTheStatin() {
        assertEquals(Severity.HIGH, check(med("سیمواستاتین"), med("کلاریترومایسین")).first().severity)
        assertTrue(check(med("آتورواستاتین"), med("کلاریترومایسین")).none { it.severity == Severity.HIGH })
    }

    @Test
    fun sameDrugTwiceIsFlagged() {
        val findings = check(med("بروفن"), med("ایبوپروفن ۴۰۰"))
        assertTrue(findings.any { it.text.contains("ایبوپروفن") && it.severity == Severity.MODERATE })
    }

    @Test
    fun levothyroxineAndCalciumSpacing() {
        val ok = check(med("لووتیروکسین", 6 * 60 + 30), med("کلسیم", 13 * 60)).single()
        assertEquals(Severity.TIMING, ok.severity)
        assertTrue(ok.spacingOk)
        assertFalse(ok.needsAttention)

        assertFalse(check(med("لووتیروکسین", 7 * 60), med("کلسیم", 8 * 60)).single().spacingOk)
        // Calcium an hour *before* levothyroxine is just as bad.
        assertFalse(check(med("لووتیروکسین", 7 * 60), med("کلسیم", 6 * 60)).single().spacingOk)
        // Order of the medications in the list doesn't matter.
        assertTrue(check(med("کلسیم", 13 * 60), med("لووتیروکسین", 6 * 60 + 30)).single().spacingOk)
    }

    @Test
    fun ciprofloxacinSpacingIsDirectional() {
        // 2 h before calcium is fine; only 2 h after it is not (needs 6 h).
        assertTrue(check(med("سیپروفلوکساسین", 9 * 60), med("کلسیم", 13 * 60)).single().spacingOk)
        assertFalse(check(med("سیپروفلوکساسین", 15 * 60), med("کلسیم", 13 * 60)).single().spacingOk)
    }

    @Test
    fun supplementsInteractToo() {
        assertEquals(Severity.MODERATE, check(med("وارفارین"), med("ویتامین K")).first().severity)
        assertEquals(Severity.HIGH, check(med("راکوتان"), med("ویتامین آ")).first().severity)
        // St John's wort lowers the effect of the pill.
        assertEquals(Severity.HIGH, check(med("علف چای"), med("قرص ضدبارداری")).first().severity)
        assertTrue(check(med("ویتامین ث"), med("ویتامین ب۱۲")).isEmpty())
    }

    @Test
    fun probioticNeedsSpacingFromAntibiotics() {
        assertFalse(check(med("پروبیوتیک", 9 * 60), med("آموکسی سیلین", 9 * 60)).single().spacingOk)
        assertTrue(check(med("پروبیوتیک", 7 * 60 + 30), med("سفیکسیم", 13 * 60)).single().spacingOk)
    }

    @Test
    fun suggestedSupplementTimesKeepTheirOwnSpacing() {
        // The default suggestions shouldn't trip the app's own spacing warnings.
        val findings = check(
            med("لووتیروکسین", 6 * 60 + 30), med("آهن", 11 * 60), med("کلسیم", 13 * 60), med("روی", 16 * 60),
        )
        assertTrue(findings.joinToString { it.text }, findings.none { it.needsAttention })
    }

    @Test
    fun spacingAcrossMidnight() {
        assertTrue(InteractionChecker.spacingOk(listOf(22 * 60), listOf(7 * 60), 240, 240))
        assertFalse(InteractionChecker.spacingOk(listOf(23 * 60), listOf(60), 240, 240))
    }

    @Test
    fun checkAgainstSkipsTheMedicationItself() {
        val warfarin = med("وارفارین")
        assertTrue(InteractionChecker.checkAgainst(warfarin, listOf(warfarin)).isEmpty())
        val draft = Medication(name = "آسپرین", times = listOf(9 * 60))
        assertEquals(Severity.HIGH, InteractionChecker.checkAgainst(draft, listOf(warfarin)).first().severity)
    }
}
