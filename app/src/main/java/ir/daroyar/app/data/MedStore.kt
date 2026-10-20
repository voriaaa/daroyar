package ir.daroyar.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.time.LocalDate

/**
 * Medications and the record of taken doses, kept in SharedPreferences as JSON.
 *
 * One instance per process, shared by the UI and the alarm receivers, so a dose marked
 * from a notification shows up immediately in an open app.
 */
class MedStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("daroyar", Context.MODE_PRIVATE)

    private val _meds = MutableStateFlow(readMeds())
    val meds: StateFlow<List<Medication>> = _meds.asStateFlow()

    /** Keys from [doseKey] for every dose marked as taken. */
    private val _taken = MutableStateFlow(readTaken())
    val taken: StateFlow<Set<String>> = _taken.asStateFlow()

    fun medication(id: Long): Medication? = _meds.value.firstOrNull { it.id == id }

    /** Inserts (id = 0) or replaces a medication and returns it with its final id. */
    @Synchronized
    fun save(med: Medication): Medication {
        val list = _meds.value
        val saved = if (med.id == 0L) med.copy(id = (list.maxOfOrNull { it.id } ?: 0L) + 1) else med
        val updated = if (list.any { it.id == saved.id }) {
            list.map { if (it.id == saved.id) saved else it }
        } else {
            list + saved
        }
        writeMeds(updated)
        return saved
    }

    @Synchronized
    fun delete(id: Long) {
        writeMeds(_meds.value.filterNot { it.id == id })
        writeTaken(_taken.value.filterNot { it.startsWith("$id|") }.toSet())
    }

    fun isTaken(medId: Long, date: LocalDate, minuteOfDay: Int): Boolean =
        doseKey(medId, date, minuteOfDay) in _taken.value

    /** Marks a dose taken or not, counting the medication's stock down or back up. */
    @Synchronized
    fun setTaken(medId: Long, date: LocalDate, minuteOfDay: Int, taken: Boolean) {
        val key = doseKey(medId, date, minuteOfDay)
        val current = _taken.value
        if ((key in current) == taken) return
        writeTaken(if (taken) current + key else current - key)

        val med = medication(medId) ?: return
        val stock = med.stock ?: return
        val newStock = if (taken) (stock - med.perDose).coerceAtLeast(0) else stock + med.perDose
        writeMeds(_meds.value.map { if (it.id == medId) it.copy(stock = newStock) else it })
    }

    private fun readMeds(): List<Medication> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_MEDS, "[]"))
        List(arr.length()) { Medication.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun writeMeds(list: List<Medication>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_MEDS, arr.toString()).apply()
        _meds.value = list
    }

    private fun readTaken(): Set<String> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_TAKEN, "[]"))
        val oldest = LocalDate.now().minusDays(KEEP_DAYS).toEpochDay()
        // Drop records older than KEEP_DAYS so the list doesn't grow forever.
        List(arr.length()) { arr.getString(it) }
            .filter { key -> (key.split('|').getOrNull(1)?.toLongOrNull() ?: 0L) >= oldest }
            .toSet()
    }.getOrDefault(emptySet())

    private fun writeTaken(set: Set<String>) {
        prefs.edit().putString(KEY_TAKEN, JSONArray(set.toList()).toString()).apply()
        _taken.value = set
    }

    companion object {
        private const val KEY_MEDS = "medications"
        private const val KEY_TAKEN = "taken_doses"
        private const val KEEP_DAYS = 90L

        fun doseKey(medId: Long, date: LocalDate, minuteOfDay: Int) = "$medId|${date.toEpochDay()}|$minuteOfDay"

        @Volatile
        private var instance: MedStore? = null

        fun get(context: Context): MedStore =
            instance ?: synchronized(this) { instance ?: MedStore(context).also { instance = it } }
    }
}
