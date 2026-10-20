package ir.daroyar.app.data

import ir.daroyar.app.reminder.Schedule
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** Medicines and supplements (vitamins, minerals…) are reminded alike; the kind only changes wording and grouping. */
enum class Kind(val noun: String) { MEDICINE("دارو"), SUPPLEMENT("مکمل") }

/**
 * A medicine or supplement taken at fixed times, every day or on some days of the week.
 *
 * @property times dose times as minutes since midnight, sorted and distinct.
 * @property days weekdays the medicine is taken on; empty means every day (never all seven).
 * @property stock units left (pills, sachets…); null when the user doesn't track it.
 * @property perDose units consumed by one dose, used to count [stock] down.
 */
data class Medication(
    val id: Long = 0,
    val name: String = "",
    val dose: String = "",
    val times: List<Int> = emptyList(),
    val notes: String = "",
    val stock: Int? = null,
    val perDose: Int = 1,
    val active: Boolean = true,
    val days: Set<DayOfWeek> = emptySet(),
    val kind: Kind = Kind.MEDICINE,
) {
    val isSupplement: Boolean get() = kind == Kind.SUPPLEMENT

    /** "دارو" or "مکمل", for labels such as "حذف دارو". */
    val noun: String get() = kind.noun

    val everyDay: Boolean get() = days.isEmpty()

    fun isDueOn(date: LocalDate): Boolean = Schedule.isDue(days, date)

    /** Fewer than three dosing days' worth of doses left. */
    val lowStock: Boolean
        get() = stock != null && times.isNotEmpty() && stock < perDose * times.size * 3

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("dose", dose)
        .put("times", JSONArray(times))
        .put("notes", notes)
        .put("stock", stock ?: -1)
        .put("perDose", perDose)
        .put("active", active)
        .put("days", JSONArray(days.sorted().map { it.value }))
        .put("kind", if (isSupplement) "supplement" else "medicine")

    companion object {
        fun fromJson(o: JSONObject): Medication {
            val t = o.optJSONArray("times") ?: JSONArray()
            // Saved before weekday schedules existed: no "days" key, i.e. every day.
            val d = o.optJSONArray("days") ?: JSONArray()
            return Medication(
                id = o.getLong("id"),
                name = o.optString("name"),
                dose = o.optString("dose"),
                times = List(t.length()) { t.getInt(it) }.distinct().sorted(),
                notes = o.optString("notes"),
                stock = o.optInt("stock", -1).takeIf { it >= 0 },
                perDose = o.optInt("perDose", 1).coerceAtLeast(1),
                active = o.optBoolean("active", true),
                days = Schedule.normalizeDays(
                    List(d.length()) { d.optInt(it) }.filter { it in 1..7 }.map { DayOfWeek.of(it) }
                ),
                // Saved before supplements existed: a medicine.
                kind = if (o.optString("kind") == "supplement") Kind.SUPPLEMENT else Kind.MEDICINE,
            )
        }
    }
}
