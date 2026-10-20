package ir.daroyar.app.drugs

import ir.daroyar.app.data.Medication

/**
 * One entry of the built-in drug reference.
 *
 * @property tags interaction groups (e.g. "nsaid", "anticoagulant"); the [id] counts as a tag too.
 * @property tip a few words added to the reminder notification, e.g. "همراه غذا".
 * @property suggestedTimes typical once/twice-daily times (minutes since midnight) when the time of
 *   day matters for this drug; null when it doesn't or depends on the prescription.
 */
data class Drug(
    val id: String,
    val fa: String,
    val en: String,
    val aliases: List<String>,
    val tags: Set<String>,
    val use: String,
    val about: String,
    val timing: String,
    val tip: String?,
    val suggestedTimes: List<Int>?,
    val caution: String?,
) {
    val allTags: Set<String> get() = tags + id
}

enum class Severity { HIGH, MODERATE, TIMING }

/**
 * An interaction between any drug carrying a [left] tag and any drug carrying a [right] tag.
 * TIMING rules are satisfied by spacing the doses: the left drug at least [before] minutes before
 * the right one, and at least [after] minutes after it.
 */
data class Rule(
    val left: Set<String>,
    val right: Set<String>,
    val severity: Severity,
    val text: String,
    val before: Int = 0,
    val after: Int = 0,
)

data class Finding(
    val medA: Medication,
    val medB: Medication,
    val severity: Severity,
    val text: String,
    /** Only for [Severity.TIMING]: whether the current dose times already keep the spacing. */
    val spacingOk: Boolean = false,
) {
    val needsAttention: Boolean
        get() = severity == Severity.HIGH || (severity == Severity.TIMING && !spacingOk)

    fun involves(med: Medication) = medA.id == med.id || medB.id == med.id

    /** Sort order: serious first, satisfied spacing last. */
    val rank: Int
        get() = when {
            severity == Severity.HIGH -> 0
            severity == Severity.TIMING && !spacingOk -> 1
            severity == Severity.MODERATE -> 2
            else -> 3
        }
}
