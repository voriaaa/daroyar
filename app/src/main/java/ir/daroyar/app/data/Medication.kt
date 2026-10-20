package ir.daroyar.app.data

import ir.daroyar.app.reminder.Schedule
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** Medicines and supplements (vitamins, minerals…) are reminded alike; the kind only changes wording and grouping. */
enum class Kind(val noun: String) { MEDICINE("دارو"), SUPPLEMENT("مکمل") }

/** What the medicine looks like; it picks the medication's icon. */
enum class Form(val label: String, val key: String, private val words: List<String>) {
    TABLET("قرص", "tablet", listOf("قرص", "tablet", "tab")),
    CAPSULE("کپسول", "capsule", listOf("کپسول", "پرل", "سافت ژل", "capsule", "cap", "softgel")),
    SYRUP("شربت", "syrup", listOf("شربت", "سوسپانسیون", "الگزیر", "syrup", "suspension")),
    DROPS("قطره", "drops", listOf("قطره", "drop", "drops")),
    INJECTION("آمپول", "injection", listOf("آمپول", "تزریق", "ویال", "انسولین", "سرنگ", "قلم", "injection", "inj", "insulin")),
    SPRAY("اسپری", "spray", listOf("اسپری", "افشانه", "اینهیلر", "spray", "inhaler")),
    CREAM("پماد", "cream", listOf("پماد", "کرم", "ژل", "لوسیون", "cream", "ointment", "gel", "lotion"));

    companion object {
        fun fromKey(key: String?): Form = entries.firstOrNull { it.key == key } ?: TABLET

        /** The form a name such as "شربت دیفن هیدرامین" or "Ventolin inhaler" spells out, if any. */
        fun guess(name: String): Form? {
            val words = name.lowercase().split(Regex("[\\s\\u200c\\d.,،/()+-]+")).filter { it.isNotEmpty() }
            val text = words.joinToString(" ")
            return entries.firstOrNull { form ->
                form.words.any { w -> if (' ' in w) w in text else w in words }
            }
        }
    }
}

/**
 * A medicine or supplement taken at fixed times, every day or on some days of the week, optionally
 * only within a date range.
 *
 * @property times dose times as minutes since midnight, sorted and distinct.
 * @property days weekdays the medicine is taken on; empty means every day (never all seven).
 * @property range dates the medicine is limited to (one-off or every month); null means no limit.
 * @property form how it looks (tablet, syrup…), for its icon.
 * @property color index of its colour in the app's palette, so the user can tell medicines apart.
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
    val range: DateRange? = null,
    val form: Form = Form.TABLET,
    val color: Int = 0,
) {
    val isSupplement: Boolean get() = kind == Kind.SUPPLEMENT

    /** "دارو" or "مکمل", for labels such as "حذف دارو". */
    val noun: String get() = kind.noun

    val everyDay: Boolean get() = days.isEmpty()

    fun isDueOn(date: LocalDate): Boolean = Schedule.isDue(days, date) && (range == null || date in range)

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
        .apply { range?.let { put("range", it.toJson()) } }
        .put("form", form.key)
        .put("color", color)

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
                // Saved before date ranges existed: no limit.
                range = DateRange.fromJson(o.optJSONObject("range")),
                // Saved before icons existed: a tablet in the first colour.
                form = Form.fromKey(o.optString("form")),
                color = o.optInt("color", 0).coerceAtLeast(0),
            )
        }
    }
}
