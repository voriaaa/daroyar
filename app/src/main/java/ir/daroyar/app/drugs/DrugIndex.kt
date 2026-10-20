package ir.daroyar.app.drugs

import android.content.Context
import java.io.InputStream

/**
 * Names of the many drugs that have no detailed entry in [DrugDatabase]: every generic name in
 * the Iranian drug list, medicinal plants and herbal brand products, read from
 * assets/drug_index.tsv (built by tools/update_drug_index.py from the public lists of darooyab.ir).
 *
 * Such a drug is recognized and shown with its drug group and general advice for that group; it
 * has no suggested times and takes no part in interaction checks.
 */
object DrugIndex {

    const val TAG = "indexed"
    const val SOURCE = "فهرست نام داروها از سایت دارویاب (darooyab.ir)"

    @Volatile
    var drugs: List<Drug> = emptyList()
        private set

    private val SUPPLEMENT_GROUPS = listOf("مکمل", "گیاه", "طب سنتی", "پروبیوتیک", "فرآوردهٔ گیاهی")

    fun load(context: Context) {
        if (drugs.isNotEmpty()) return
        context.assets.open("drug_index.tsv").use { load(it) }
    }

    fun load(stream: InputStream) {
        val parsed = stream.bufferedReader().readLines().mapIndexedNotNull { i, line -> parse(i, line) }
        // A name the detailed reference already knows stays with its detailed entry.
        drugs = parsed.filter { DrugMatcher.matchDetailed(it.fa).isEmpty() && DrugMatcher.matchDetailed(it.en).isEmpty() }
        DrugMatcher.onIndexLoaded()
    }

    private fun parse(lineNo: Int, line: String): Drug? {
        if (line.isBlank() || line.startsWith("#")) return null
        val cols = line.split('\t')
        if (cols.size < 4 || cols[1].isBlank()) return null
        val (kind, fa, en, group) = cols
        val aliases = cols.getOrNull(4).orEmpty().split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val advice = adviceFor(kind, group)
        val tags = buildSet {
            add(TAG)
            if (kind != "G" || SUPPLEMENT_GROUPS.any { it in group }) add("supplement")
        }
        return Drug(
            id = "idx$lineNo",
            fa = fa.trim(),
            en = en.trim().ifBlank { fa.trim() },
            aliases = aliases,
            tags = tags,
            use = advice.use,
            about = "گروه: $group",
            timing = advice.timing,
            tip = null,
            suggestedTimes = null,
            caution = advice.caution,
        )
    }

    private class Advice(val use: String, val timing: String, val caution: String?)

    private const val PRESCRIBED = "طبق نسخهٔ پزشک یا بروشور دارو، در ساعت‌های ثابت."
    private const val ASK_PHARMACIST =
        "اطلاعات کامل این دارو در دارویار نیست؛ برای زمان مصرف و تداخل‌ها از پزشک یا داروساز بپرسید."

    /** General advice per drug group, written for DaroYar (not taken from the source site). */
    private val GROUP_ADVICE: List<Pair<String, Advice>> = listOf(
        "مسکن" to Advice(
            "از گروه مسکن‌ها، ضدالتهاب‌ها و تب‌برها (درد، التهاب یا تب).",
            "بیشتر این داروها بهتر است همراه غذا مصرف شوند تا معده کمتر اذیت شود. $PRESCRIBED",
            "بیش از دوز مجاز روزانه مصرف نکنید و دو مسکن هم‌گروه را با هم نخورید.",
        ),
        "ضد کرم" to Advice(
            "از گروه داروهای ضدکرم (انگل‌های روده).",
            "اغلب یک دوز یا دورهٔ کوتاه؛ در کرمک، همهٔ اعضای خانواده با هم درمان شوند.",
            "در بارداری بدون نظر پزشک مصرف نشود.",
        ),
        "ضد باکتری" to Advice(
            "آنتی‌بیوتیک (عفونت‌های باکتریایی).",
            "در فاصله‌های مساوی در طول شبانه‌روز (مثلاً هر ۸ یا ۱۲ ساعت) طبق نسخه.",
            "دوره را کامل کنید، حتی اگر زودتر بهتر شدید. بثورات یا تنگی نفس را فوراً گزارش کنید.",
        ),
        "ضد افسردگی" to Advice(
            "از گروه داروهای ضدافسردگی.",
            "هر روز در ساعت ثابت؛ اگر خواب‌آلود می‌کند شب، اگر بی‌خوابی می‌دهد صبح.",
            "اثر کامل چند هفته طول می‌کشد. ناگهان قطع نشود.",
        ),
        "ضد دیابت" to Advice(
            "از گروه داروهای دیابت (کاهش قند خون).",
            "بیشتر این داروها به وعده‌های غذایی وابسته‌اند؛ طبق نسخه، معمولاً همراه یا کمی قبل از غذا.",
            "علائم افت قند (لرزش، تعریق، گرسنگی) را بشناسید.",
        ),
        "ضد صرع" to Advice(
            "از گروه داروهای ضدتشنج.",
            "در ساعت‌های کاملاً ثابت هر روز؛ نوبتی را جا نیندازید.",
            "ناگهان قطع نشود. خواب‌آلودگی ممکن است.",
        ),
        "ضد قارچ" to Advice(
            "از گروه داروهای ضدقارچ.",
            PRESCRIBED,
            "دوره را کامل کنید.",
        ),
        "ضد نقرس" to Advice(
            "از گروه داروهای نقرس.",
            "معمولاً بعد از غذا با آب فراوان.",
            null,
        ),
        "آنتی هیستامین" to Advice(
            "آنتی‌هیستامین (حساسیت، خارش، آبریزش بینی).",
            "اگر خواب‌آلود می‌کند، شب مصرف کنید.",
            "ممکن است خواب‌آلودگی بدهد؛ با احتیاط رانندگی کنید.",
        ),
        "مالاریا" to Advice(
            "از گروه داروهای ضدمالاریا.",
            "اغلب همراه غذا؛ برای پیشگیری در سفر، طبق برنامهٔ پزشک.",
            null,
        ),
        "میگرن" to Advice(
            "از گروه داروهای میگرن.",
            "داروی حمله در شروع سردرد؛ داروی پیشگیری هر روز در ساعت ثابت.",
            "داروی حمله را بیش از ۱۰ روز در ماه مصرف نکنید.",
        ),
        "موسکارینیک" to Advice(
            "از گروه آنتی‌کولینرژیک‌ها (ضداسپاسم).",
            PRESCRIBED,
            "خشکی دهان و یبوست شایع است؛ در گلوکوم و بزرگی پروستات با احتیاط.",
        ),
        "نئوپلاستیک" to Advice(
            "از گروه داروهای ضدسرطان.",
            "دقیقاً طبق برنامهٔ پزشک معالج؛ برخی معده خالی و برخی همراه غذا.",
            "تب یا علائم عفونت را فوراً گزارش کنید. قرص را خرد نکنید و بعد از لمس دست‌ها را بشویید.",
        ),
        "پروتوزوآ" to Advice(
            "از گروه داروهای ضدانگل تک‌یاخته‌ای.",
            "معمولاً همراه غذا، طبق نسخه.",
            "با بعضی از این داروها الکل ممنوع است.",
        ),
        "ویروس" to Advice(
            "از گروه داروهای ضدویروس.",
            "در ساعت‌های ثابت و با فاصلهٔ مساوی؛ هیچ نوبتی را جا نیندازید.",
            "دوره را کامل کنید.",
        ),
        "خواب آور" to Advice(
            "از گروه آرام‌بخش‌ها، خواب‌آورها یا داروهای ضدروان‌پریشی.",
            "داروی خواب درست قبل از خواب؛ بقیه طبق نسخه در ساعت‌های ثابت.",
            "خواب‌آلودگی؛ رانندگی نکنید و الکل ننوشید. ناگهان قطع نشود.",
        ),
        "خون" to Advice(
            "از گروه داروهای خون (خون‌سازی یا انعقاد).",
            PRESCRIBED,
            "خونریزی یا کبودی غیرعادی را گزارش کنید.",
        ),
        "استخوان" to Advice(
            "از گروه داروهای استخوان.",
            "بسیاری از داروهای پوکی استخوان صبح ناشتا با یک لیوان آب مصرف می‌شوند؛ طبق بروشور.",
            null,
        ),
        "آسم" to Advice(
            "از گروه داروهای آسم و گشادکننده‌های نایژه.",
            "داروهای کنترل‌کننده هر روز صبح و شب؛ اسپری‌های سریع‌اثر فقط هنگام نیاز.",
            "بعد از اسپری‌های کورتون‌دار دهان را بشویید.",
        ),
        "قلبی" to Advice(
            "از گروه داروهای قلب و عروق یا فشار خون.",
            "هر روز در ساعت ثابت؛ ادرارآورها صبح.",
            "ناگهان قطع نشود. سرگیجه هنگام بلند شدن ممکن است.",
        ),
        "آنتی دوت" to Advice(
            "از گروه پادزهرها و داروهای ضدمسمومیت.",
            PRESCRIBED,
            null,
        ),
        "کورتیکواستروئید" to Advice(
            "کورتون (ضدالتهاب و ضدحساسیت قوی).",
            "شکل خوراکی معمولاً صبح همراه صبحانه.",
            "پس از مصرف طولانی ناگهان قطع نشود.",
        ),
        "سرفه" to Advice(
            "از گروه داروهای سرفه، خلط‌آور یا ضداحتقان.",
            "در صورت نیاز؛ داروهای ضداحتقان را عصر و شب نخورید (بی‌خوابی).",
            "در فشار خون بالا با ضداحتقان‌ها احتیاط کنید.",
        ),
        "پوستی" to Advice(
            "از گروه داروهای پوستی.",
            "داروی موضعی را روی پوست تمیز و خشک بمالید، معمولاً یک یا دو بار در روز.",
            null,
        ),
        "دوپامینرژیک" to Advice(
            "از گروه داروهای دوپامینی (پارکینسون، پرولاکتین، پای بی‌قرار).",
            PRESCRIBED,
            "خواب ناگهانی یا سرگیجه را گزارش کنید.",
        ),
        "الکترولیت" to Advice(
            "از گروه الکترولیت‌ها و املاح.",
            "معمولاً همراه غذا و آب کافی.",
            "در بیماری کلیه فقط با نظر پزشک.",
        ),
        "گوارشی" to Advice(
            "از گروه داروهای گوارشی.",
            "داروهای معده اغلب قبل از غذا و ملین‌ها اغلب شب؛ طبق بروشور.",
            null,
        ),
        "هیپوفیز" to Advice(
            "از گروه هورمون‌های هیپوفیز و هیپوتالاموس.",
            PRESCRIBED,
            null,
        ),
        "ایمنی" to Advice(
            "از گروه داروهای سرکوب‌کنندهٔ ایمنی.",
            "در ساعت‌های کاملاً ثابت؛ آزمایش‌های منظم لازم است.",
            "تب یا علائم عفونت را فوراً گزارش کنید.",
        ),
        "عضلات" to Advice(
            "از گروه شل‌کننده‌های عضلانی.",
            "اغلب شب، چون خواب‌آورند؛ طبق نسخه.",
            "خواب‌آلودگی؛ رانندگی نکنید.",
        ),
        "مکمل" to Advice(
            "مکمل غذایی یا ویتامین.",
            "بیشتر مکمل‌ها همراه غذا بهتر جذب می‌شوند؛ ویتامین‌های محلول در چربی همراه غذای چرب.",
            "با هورمون تیروئید و برخی آنتی‌بیوتیک‌ها فاصله بگذارید اگر مواد معدنی دارد.",
        ),
        "پاراسمپاتومیمتیک" to Advice(
            "از گروه داروهای کولینرژیک.",
            PRESCRIBED,
            null,
        ),
        "پروستاگلاندین" to Advice(
            "از گروه پروستاگلاندین‌ها.",
            PRESCRIBED,
            null,
        ),
        "هورمون های جنسی" to Advice(
            "از گروه هورمون‌های جنسی.",
            "هر روز دقیقاً در ساعت ثابت.",
            "درد ساق پا یا قفسهٔ سینه را گزارش کنید.",
        ),
        "اشتها" to Advice(
            "از گروه داروهای اشتها (افزایش یا کاهش).",
            "داروهای محرک صبح؛ اشتهاآورها قبل از غذا.",
            null,
        ),
        "تیروئید" to Advice(
            "از گروه داروهای تیروئید.",
            "هورمون تیروئید صبح ناشتا؛ داروهای ضدتیروئید در ساعت‌های ثابت.",
            null,
        ),
        "زوال عقل" to Advice(
            "از گروه داروهای آلزایمر و فراموشی.",
            "هر روز در ساعت ثابت؛ برخی شب.",
            null,
        ),
        "پارکینسون" to Advice(
            "از گروه داروهای پارکینسون.",
            "ساعت‌های دقیق و ثابت مهم است؛ برخی ۳۰ دقیقه قبل از غذا.",
            "ناگهان قطع نشود.",
        ),
        "گلوکوم" to Advice(
            "از گروه داروهای چشم (گلوکوم یا مردمک).",
            "قطره‌ها را در ساعت ثابت بریزید؛ بین دو قطرهٔ مختلف ۵ دقیقه فاصله.",
            null,
        ),
        "ادراری" to Advice(
            "از گروه داروهای مجاری ادراری.",
            PRESCRIBED,
            null,
        ),
        "گیاه" to Advice(
            "داروی گیاهی یا گیاه دارویی.",
            "طبق بروشور یا توصیهٔ داروساز؛ بیشتر همراه یا بعد از غذا.",
            "گیاهی بودن به معنای بی‌خطر بودن نیست؛ در بارداری، شیردهی و همراه داروهای دیگر با پزشک یا داروساز مشورت کنید.",
        ),
        "طب سنتی" to Advice(
            "فرآوردهٔ طب سنتی.",
            "طبق بروشور یا توصیهٔ داروساز.",
            "همراه داروهای دیگر با پزشک یا داروساز مشورت کنید.",
        ),
        "پروبیوتیک" to Advice(
            "پروبیوتیک (باکتری‌های مفید روده).",
            "معمولاً قبل از صبحانه یا همراه آن؛ با آنتی‌بیوتیک ۲ ساعت فاصله.",
            null,
        ),
    )

    private fun adviceFor(kind: String, group: String): Advice {
        if (kind == "P") return GROUP_ADVICE.first { it.first == "گیاه" }.second
        if (kind == "B") {
            val g = GROUP_ADVICE.first { it.first == "گیاه" }.second
            return Advice("فرآوردهٔ گیاهی (${group.removePrefix("فرآوردهٔ گیاهی: ")}).", g.timing, g.caution)
        }
        val found = GROUP_ADVICE.firstOrNull { (key, _) -> key in group }?.second
        return found?.let { Advice(it.use, it.timing, it.caution ?: ASK_PHARMACIST) }
            ?: Advice("از گروه «$group».", PRESCRIBED, ASK_PHARMACIST)
    }
}

val Drug.isIndexed: Boolean get() = DrugIndex.TAG in tags
