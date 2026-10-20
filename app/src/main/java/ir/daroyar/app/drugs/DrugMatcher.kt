package ir.daroyar.app.drugs

/**
 * Finds the drugs in a free-text medication name such as "قرص متفورمین ۵۰۰" or "Zoloft 50mg".
 *
 * Names are compared after [normalize]: Arabic letter variants become Persian, Persian digits become
 * Latin (so "ب۱۲" and "B12" match but "ب۶" doesn't), and everything else becomes a space. Doses
 * and units don't get in the way because names are found as substrings of the text. Longer names win over shorter ones
 * they contain ("اس‌سیتالوپرام" is escitalopram, not citalopram), and very short names
 * ("آهن", "روی") must stand as a whole word to avoid matching inside other words.
 *
 * Names with no detailed entry fall back to [DrugIndex]; those must match whole words, because the
 * index holds thousands of brand names, some of them ordinary words.
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
                mapped.isDigit() -> sb.append('0' + Character.getNumericValue(mapped))
                else -> sb.append(' ')
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }

    private fun compact(normalized: String) = normalized.replace(" ", "")

    /** Index names (normalized, spaces removed) to their drugs; filled once [DrugIndex] has loaded. */
    @Volatile
    private var indexKeys: Map<String, List<Drug>> = emptyMap()

    private const val MAX_INDEX_WORDS = 6

    internal fun onIndexLoaded() {
        indexKeys = DrugIndex.drugs
            .flatMap { drug -> (listOf(drug.fa, drug.en) + drug.aliases).map { compact(normalize(it)) to drug } }
            .filter { it.first.length >= 3 }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, drugs) -> drugs.distinct() }
    }

    /**
     * Drugs contained in [name], in the order they appear; empty when none is recognized.
     * Detailed entries win; otherwise the longest run of whole words that is an index name.
     */
    fun match(name: String): List<Drug> {
        val detailed = matchDetailed(name)
        val index = indexKeys
        if (detailed.isNotEmpty() || index.isEmpty()) return detailed
        val tokens = normalize(name).split(' ').filter { it.isNotEmpty() }
        for (len in minOf(MAX_INDEX_WORDS, tokens.size) downTo 1) {
            for (i in 0..tokens.size - len) {
                index[tokens.subList(i, i + len).joinToString("")]?.let { return listOf(it.first()) }
            }
        }
        return emptyList()
    }

    /** Only the detailed reference ([DrugDatabase]), never the name index. */
    fun matchDetailed(name: String): List<Drug> {
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

    private class Keys(val drug: Drug, val fa: String, val all: List<String>)

    private val detailedKeys: List<Keys> by lazy {
        DrugDatabase.drugs.map { drug ->
            Keys(drug, compact(normalize(drug.fa)), (listOf(drug.fa, drug.en) + drug.aliases).map { compact(normalize(it)) })
        }
    }

    /**
     * Names to offer while the user is still typing [query], best first: detailed entries whose
     * Persian name starts with it, then any of their names (English, brand) starting with it, then
     * names containing it, then index names by prefix (and, from three letters on, by substring).
     */
    fun suggest(query: String, limit: Int = 8): List<Drug> {
        val q = compact(normalize(query))
        if (q.isEmpty()) return emptyList()
        val result = LinkedHashSet<Drug>()
        fun offer(drugs: Sequence<Drug>) {
            for (d in drugs) {
                if (result.size >= limit) return
                result += d
            }
        }
        offer(detailedKeys.asSequence().filter { it.fa.startsWith(q) }.sortedBy { it.fa.length }.map { it.drug })
        offer(detailedKeys.asSequence().filter { k -> k.all.any { it.startsWith(q) } }.map { it.drug })
        if (q.length >= 2) offer(detailedKeys.asSequence().filter { k -> k.all.any { q in it } }.map { it.drug })
        val index = indexKeys
        offer(index.asSequence().filter { (key, _) -> key.startsWith(q) }.sortedBy { it.key.length }.map { it.value.first() })
        if (q.length >= 3) offer(index.asSequence().filter { (key, _) -> q in key }.map { it.value.first() })
        return result.toList()
    }
}
