package ir.daroyar.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A medicine taken every day at fixed times.
 *
 * @property times dose times as minutes since midnight, sorted and distinct.
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
) {
    /** Fewer than three days of doses left. */
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

    companion object {
        fun fromJson(o: JSONObject): Medication {
            val t = o.optJSONArray("times") ?: JSONArray()
            return Medication(
                id = o.getLong("id"),
                name = o.optString("name"),
                dose = o.optString("dose"),
                times = List(t.length()) { t.getInt(it) }.distinct().sorted(),
                notes = o.optString("notes"),
                stock = o.optInt("stock", -1).takeIf { it >= 0 },
                perDose = o.optInt("perDose", 1).coerceAtLeast(1),
                active = o.optBoolean("active", true),
            )
        }
    }
}
