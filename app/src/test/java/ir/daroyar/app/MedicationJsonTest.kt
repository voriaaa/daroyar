package ir.daroyar.app

import ir.daroyar.app.data.Form
import ir.daroyar.app.data.Kind
import ir.daroyar.app.data.Medication
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY

class MedicationJsonTest {

    @Test
    fun roundTripKeepsDays() {
        val med = Medication(id = 3, name = "متوترکسات", times = listOf(9 * 60), days = setOf(SATURDAY, WEDNESDAY))
        assertEquals(med, Medication.fromJson(JSONObject(med.toJson().toString())))
        assertEquals(Medication(id = 4, name = "x"), Medication.fromJson(Medication(id = 4, name = "x").toJson()))
    }

    @Test
    fun kindRoundTripsAndOldEntriesAreMedicines() {
        val vitamin = Medication(id = 5, name = "ویتامین د", times = listOf(13 * 60), kind = Kind.SUPPLEMENT)
        assertEquals(vitamin, Medication.fromJson(JSONObject(vitamin.toJson().toString())))
        val old = JSONObject("""{"id":1,"name":"متفورمین","times":[480]}""")
        assertEquals(Kind.MEDICINE, Medication.fromJson(old).kind)
    }

    @Test
    fun medicationsSavedBeforeWeekdaysAreDaily() {
        val old = JSONObject("""{"id":1,"name":"متفورمین","dose":"","times":[480,1200],"notes":"","stock":-1,"perDose":1,"active":true}""")
        val med = Medication.fromJson(old)
        assertTrue(med.everyDay)
        assertEquals(listOf(480, 1200), med.times)
    }

    @Test
    fun allSevenDaysOrBadValuesAreCleanedUp() {
        val all = JSONObject("""{"id":1,"times":[480],"days":[1,2,3,4,5,6,7]}""")
        assertEquals(emptySet<DayOfWeek>(), Medication.fromJson(all).days)
        val junk = JSONObject("""{"id":1,"times":[480],"days":[0,6,6,9,"x"]}""")
        assertEquals(setOf(SATURDAY), Medication.fromJson(junk).days)
    }

    @Test
    fun formAndColourAreSavedAndOldEntriesGetDefaults() {
        val med = Medication(id = 5, name = "شربت دیفن", times = listOf(480), form = Form.SYRUP, color = 3)
        assertEquals(med, Medication.fromJson(JSONObject(med.toJson().toString())))
        val old = Medication.fromJson(JSONObject("""{"id":6,"name":"x","times":[480]}"""))
        assertEquals(Form.TABLET, old.form)
        assertEquals(0, old.color)
    }

    @Test
    fun formIsGuessedFromTheName() {
        assertEquals(Form.SYRUP, Form.guess("شربت دیفن هیدرامین"))
        assertEquals(Form.DROPS, Form.guess("قطره آهن"))
        assertEquals(Form.SPRAY, Form.guess("اسپری سالبوتامول"))
        assertEquals(Form.SPRAY, Form.guess("Ventolin inhaler"))
        assertEquals(Form.INJECTION, Form.guess("انسولین لانتوس"))
        assertEquals(Form.CAPSULE, Form.guess("کپسول امگا ۳"))
        assertEquals(Form.CAPSULE, Form.guess("ویتامین د سافت ژل"))
        assertEquals(Form.CREAM, Form.guess("پماد بتامتازون"))
        assertEquals(Form.TABLET, Form.guess("قرص متفورمین ۵۰۰"))
        assertEquals(null, Form.guess("متفورمین"))
        // Whole words only: "کرمک" (pinworm) is not a cream.
        assertEquals(null, Form.guess("داروی کرمک"))
    }
}
