package ir.daroyar.app.drugs

/**
 * Finds the drugs in a free-text medication name such as "قرص متفورمین ۵۰۰" or "Zoloft 50mg".
 *
 * Names are compared after [normalize]: Arabic letter variants become Persian, and digits, units
 * are kept out because every non-letter becomes a space. Longer names win over shorter ones
 * they contain ("اس‌سیتالوپرام" is escitalopram, not citalopram), and very short names
 * ("آهن", "روی") must stand as a whole word to avoid matching inside other words.
 */
object DrugMatcher {

    private const val SHORT_KEY = 4

    private class Entry(val key: String, val drugs: List<Drug>) {
        /** Short names only match a whole word (or two adjacent words, as in "ال دی"). */
        val short = key.length <= SHORT_KEY
    }

    private class Hit(val start: Int, val end: Int, val entry: Entry)

    private val entries: List<Entry> by lazy {
        val fromDrugs = DrugDatabase.drugs.flatMap { drug ->
            (listOf(drug.fa, drug.en) + drug.aliases).map { compact(normalize(it)) to drug }
        }
        val fromCombos = DrugDatabase.combos.map { (name, ids) ->
            compact(normalize(name)) to ids.map { DrugDatabase.byId.getValue(it) }
        }
        (fromDrugs.map { (key, drug) -> key to listOf(drug) } + fromCombos)
            .filter { it.first.length >= 3 }
            .distinctBy { (key, drugs) -> key + drugs.joinToString { it.id } }
            .map { (key, drugs) -> Entry(key, drugs) }
            .sortedByDescending { it.key.length }
    }

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text.lowercase()) {
            val mapped = when (c) {
                'ي', 'ى', 'ئ' -> 'ی'
                'ك' -> 'ک'
                'ة', 'ۀ' -> 'ه'
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ؤ' -> 'و'
                else -> c
            }
            when {
                // Diacritics (harakat, superscript alef) and tatweel are dropped.
                mapped in 'ً'..'ٟ' || mapped == 'ٰ' || mapped == 'ـ' -> Unit
                mapped.isLetter() -> sb.append(mapped)
                else -> sb.append(' ')
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }

    private fun compact(normalized: String) = normalized.replace(" ", "")

    /** Drugs contained in [name], in the order they appear; empty when none is recognized. */
    fun match(name: String): List<Drug> {
        val norm = normalize(name)
        if (norm.isEmpty()) return emptyList()
        val text = compact(norm)
        val tokens = norm.split(' ')
        val wordKeys = (tokens + tokens.zipWithNext { a, b -> a + b }).toSet()

        val hits = mutableListOf<Hit>()
        for (entry in entries) {
            if (entry.short) {
                // A whole word has no reliable position in the joined text, so it never overlaps.
                if (entry.key in wordKeys) hits += Hit(-1, -1, entry)
            } else {
                var at = text.indexOf(entry.key)
                while (at >= 0) {
                    hits += Hit(at, at + entry.key.length, entry)
                    at = text.indexOf(entry.key, at + 1)
                }
            }
        }

        // Entries are longest-first, so a hit inside an accepted longer one is a false partial match.
        val accepted = mutableListOf<Hit>()
        for (hit in hits) {
            val overlaps = hit.start >= 0 && accepted.any { it.start >= 0 && hit.start < it.end && it.start < hit.end }
            if (!overlaps) accepted += hit
        }
        return accepted.sortedBy { if (it.start < 0) Int.MAX_VALUE else it.start }
            .flatMap { it.entry.drugs }
            .distinct()
    }

    /** Drug names to offer while the user is still typing [query]. */
    fun suggest(query: String, limit: Int = 6): List<Drug> {
        val q = compact(normalize(query))
        if (q.length < 2) return emptyList()
        fun keys(drug: Drug) = (listOf(drug.fa, drug.en) + drug.aliases).map { compact(normalize(it)) }
        val starts = DrugDatabase.drugs.filter { drug -> keys(drug).any { it.startsWith(q) } }
        val contains = DrugDatabase.drugs.filter { drug -> keys(drug).any { q in it } }
        return (starts + contains).distinct().take(limit)
    }
}
