package ir.daroyar.app.drugs

import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.Severity.HIGH
import ir.daroyar.app.drugs.Severity.MODERATE
import ir.daroyar.app.drugs.Severity.TIMING

private const val ASK = "با پزشک یا داروساز مشورت کنید و هیچ دارویی را خودسرانه قطع نکنید."

private fun rule(left: String, right: String, severity: Severity, text: String) =
    Rule(left.split(' ').toSet(), right.split(' ').toSet(), severity, text)

/** [left] must be taken at least [before] minutes before [right] and [after] minutes after it. */
private fun spacing(left: String, right: String, before: Int, after: Int, text: String) =
    Rule(left.split(' ').toSet(), right.split(' ').toSet(), TIMING, text, before, after)

/** Rules are written with space-separated tag lists; a drug matches a side if it has any of them. */
val interactionRules: List<Rule> = listOf(
    // ---- Bleeding ----
    rule(
        "anticoagulant", "nsaid", HIGH,
        "خطر خونریزی (به‌خصوص از معده) زیاد می‌شود. برای درد، استامینوفن معمولاً انتخاب امن‌تری است.",
    ),
    rule(
        "anticoagulant", "antiplatelet", HIGH,
        "دو رقیق‌کنندهٔ خون با هم خطر خونریزی را زیاد می‌کنند. گاهی پزشک عمداً این دو را با هم تجویز می‌کند؛ مطمئن شوید پزشک از هر دو خبر دارد.",
    ),
    rule(
        "anticoagulant", "anticoagulant", HIGH,
        "دو داروی ضدانعقاد معمولاً با هم تجویز نمی‌شوند (خطر خونریزی شدید). فوراً با پزشک چک کنید.",
    ),
    rule(
        "warfarin", "warfarin_potentiator", HIGH,
        "این دارو اثر وارفارین را زیاد می‌کند و خطر خونریزی بالا می‌رود. INR باید زودتر چک شود و شاید دوز وارفارین تغییر کند.",
    ),
    rule(
        "warfarin", "enzyme_inducer", HIGH,
        "این دارو اثر وارفارین را کم می‌کند و خطر لخته بالا می‌رود. INR باید دقیق کنترل شود.",
    ),
    rule(
        "doac", "enzyme_inducer", HIGH,
        "این دارو سطح رقیق‌کنندهٔ خون را پایین می‌آورد و ممکن است دیگر از لخته محافظت نکند. معمولاً با هم تجویز نمی‌شوند.",
    ),
    rule(
        "warfarin", "acetaminophen", MODERATE,
        "مصرف منظم استامینوفن (بیش از ۲ گرم در روز برای چند روز) ممکن است INR را بالا ببرد. مصرف گاه‌به‌گاه مشکلی ندارد.",
    ),
    rule(
        "nsaid", "nsaid", MODERATE,
        "دو مسکن ضدالتهاب با هم اثر بیشتری ندارند ولی خطر زخم معده و آسیب کلیه را زیاد می‌کنند. معمولاً یکی کافی است.",
    ),
    rule(
        "antiplatelet", "nsaid", MODERATE,
        "خطر خونریزی معده بیشتر می‌شود. اگر هر دو لازم است، پزشک ممکن است داروی محافظ معده بدهد.",
    ),
    rule(
        "antiplatelet", "antiplatelet", MODERATE,
        "دو ضدپلاکت با هم (مثلاً بعد از استنت) خطر خونریزی را بیشتر می‌کنند. اغلب عمدی است؛ خودسرانه قطع نکنید.",
    ),
    rule(
        "ssri_bleed", "nsaid anticoagulant antiplatelet", MODERATE,
        "این ضدافسردگی همراه رقیق‌کننده‌های خون یا مسکن‌های ضدالتهاب خطر خونریزی (به‌خصوص معده) را بیشتر می‌کند.",
    ),
    rule(
        "corticosteroid", "nsaid", MODERATE,
        "خطر زخم و خونریزی معده بیشتر می‌شود؛ پزشک ممکن است داروی محافظ معده تجویز کند.",
    ),

    // ---- Potassium, kidneys, blood pressure ----
    rule(
        "ace_inhibitor arb", "potassium_sparing potassium", HIGH,
        "هر دو پتاسیم خون را بالا می‌برند و پتاسیم زیاد برای قلب خطرناک است. آزمایش پتاسیم لازم است.",
    ),
    rule(
        "potassium_sparing", "potassium", HIGH,
        "پتاسیم خون ممکن است به حد خطرناک برسد. مکمل پتاسیم را فقط با نظر پزشک مصرف کنید.",
    ),
    rule(
        "ace_inhibitor", "arb", HIGH,
        "مصرف هم‌زمان این دو گروه داروی فشار معمولاً توصیه نمی‌شود (خطر آسیب کلیه و بالا رفتن پتاسیم).",
    ),
    rule(
        "ace_inhibitor arb potassium_sparing", "raises_potassium", MODERATE,
        "کوتریموکسازول هم می‌تواند پتاسیم را بالا ببرد؛ به‌خصوص در سالمندان و بیماران کلیوی آزمایش پتاسیم لازم است.",
    ),
    rule(
        "ace_inhibitor arb diuretic", "nsaid", MODERATE,
        "مسکن‌های ضدالتهاب اثر داروی فشار را کم می‌کنند و در مصرف طولانی به کلیه آسیب می‌زنند (به‌خصوص در سالمندان). برای درد، استامینوفن بهتر است.",
    ),
    rule(
        "lithium", "nsaid", HIGH,
        "سطح لیتیوم در خون بالا می‌رود و ممکن است مسمومیت ایجاد کند. برای درد، استامینوفن بهتر است؛ آزمایش سطح لیتیوم لازم است.",
    ),
    rule(
        "lithium", "ace_inhibitor arb thiazide loop_diuretic", HIGH,
        "سطح لیتیوم در خون بالا می‌رود و ممکن است مسمومیت ایجاد کند. آزمایش سطح لیتیوم لازم است.",
    ),
    rule(
        "beta_blocker", "beta_blocker", MODERATE,
        "دو بتابلاکر با هم ضربان و فشار را بیش از حد پایین می‌آورند. معمولاً یکی کافی است.",
    ),
    rule(
        "pde5", "nitrate", HIGH,
        "افت شدید و خطرناک فشار خون. هرگز با هم مصرف نشوند.",
    ),
    rule(
        "pde5", "alpha_blocker", MODERATE,
        "افت فشار و سرگیجه هنگام بلند شدن. معمولاً با دوز کم شروع و با فاصله مصرف می‌شوند.",
    ),

    // ---- Heart rhythm ----
    rule(
        "digoxin", "amiodarone", HIGH,
        "سطح دیگوکسین بالا می‌رود (معمولاً دوز دیگوکسین کم می‌شود). علائم مسمومیت: تهوع، تاری دید، کندی ضربان.",
    ),
    rule(
        "digoxin", "loop_diuretic thiazide", MODERATE,
        "ادرارآور پتاسیم را کم می‌کند و پتاسیم پایین خطر مسمومیت با دیگوکسین را بالا می‌برد. آزمایش پتاسیم لازم است.",
    ),
    rule(
        "digoxin", "macrolide", MODERATE,
        "سطح دیگوکسین بالا می‌رود؛ به علائم مسمومیت (تهوع، تاری دید، کندی ضربان) توجه کنید.",
    ),
    rule(
        "qt_prolong", "qt_prolong", MODERATE,
        "هر دو ریتم قلب (فاصلهٔ QT) را طولانی می‌کنند و با هم خطر آریتمی دارند، به‌خصوص در بیماری قلبی یا پتاسیم پایین. پزشک ممکن است نوار قلب بخواهد.",
    ),
    rule(
        "beta_nonselective", "beta_agonist", MODERATE,
        "این بتابلاکر می‌تواند اثر اسپری آسم را کم کند و در آسم تنگی نفس ایجاد کند.",
    ),

    // ---- Brain and nerves ----
    rule(
        "serotonergic_strong", "serotonergic_strong", HIGH,
        "هر دو سروتونین مغز را بالا می‌برند و با هم خطر «سندرم سروتونین» دارند (بی‌قراری، تعریق، لرزش، تب، تپش). بدون نظر پزشک با هم مصرف نشوند.",
    ),
    rule(
        "serotonergic_strong", "serotonergic", MODERATE,
        "هر دو روی سروتونین اثر دارند؛ احتمال کمِ «سندرم سروتونین». به بی‌قراری، تعریق، لرزش و تپش توجه کنید.",
    ),
    rule(
        "opioid", "benzodiazepine", HIGH,
        "هر دو تنفس را سرکوب می‌کنند؛ مصرف هم‌زمان می‌تواند به خواب‌آلودگی شدید و توقف تنفس منجر شود.",
    ),
    rule(
        "opioid", "gabapentinoid", HIGH,
        "خطر خواب‌آلودگی شدید و کندی تنفس. فقط با کمترین دوز و به دستور پزشک.",
    ),
    rule(
        "sedating", "sedating", MODERATE,
        "هر دو خواب‌آورند؛ با هم خواب‌آلودگی، گیجی و خطر زمین خوردن بیشتر می‌شود. رانندگی نکنید و الکل ننوشید.",
    ),
    rule(
        "anticholinergic", "anticholinergic", MODERATE,
        "هر دو اثر آنتی‌کولینرژیک دارند: خشکی دهان، یبوست، احتباس ادرار و گیجی (به‌خصوص در سالمندان).",
    ),
    rule(
        "tizanidine", "cyp1a2_strong", HIGH,
        "سطح تیزانیدین چند برابر می‌شود و افت شدید فشار و خواب‌آلودگی می‌دهد. این ترکیب ممنوع است.",
    ),
    rule(
        "carbamazepine", "cyp3a4_strong", HIGH,
        "سطح کاربامازپین بالا می‌رود (سرگیجه، دوبینی، تهوع).",
    ),
    rule(
        "valproate", "lamotrigine", HIGH,
        "سطح لاموتریژین حدود دو برابر می‌شود و خطر بثورات پوستی شدید بالا می‌رود؛ دوز لاموتریژین باید کم باشد.",
    ),
    rule(
        "levodopa", "metoclopramide", HIGH,
        "متوکلوپرامید اثر لوودوپا را خنثی می‌کند و پارکینسون را بدتر می‌کند. در پارکینسون، دومپریدون جایگزین مناسب‌تری است.",
    ),
    rule(
        "levodopa", "antidopaminergic", MODERATE,
        "این دارو اثر لوودوپا را کم می‌کند و ممکن است علائم پارکینسون را بدتر کند.",
    ),
    rule(
        "cyp2d6_strong", "codeine", MODERATE,
        "اثر ضددرد کدئین کم می‌شود.",
    ),
    rule(
        "cyp2d6_strong", "metoprolol", MODERATE,
        "سطح متوپرولول بالا می‌رود و ممکن است ضربان قلب خیلی کند شود.",
    ),

    // ---- Muscles, cholesterol, gout ----
    rule(
        "statin_3a4_high", "cyp3a4_strong", HIGH,
        "سطح استاتین چند برابر می‌شود و خطر آسیب شدید عضلانی دارد. معمولاً استاتین در دورهٔ مصرف این آنتی‌بیوتیک موقتاً قطع می‌شود؛ با پزشک هماهنگ کنید.",
    ),
    rule(
        "statin_3a4", "cyp3a4_strong", MODERATE,
        "سطح آتورواستاتین بالا می‌رود؛ دوز آن باید محدود شود (معمولاً حداکثر ۲۰ میلی‌گرم) یا موقتاً قطع شود.",
    ),
    rule(
        "statin_3a4_high", "simva_dose_limit", MODERATE,
        "خطر آسیب عضلانی بیشتر می‌شود؛ پزشک معمولاً دوز سیمواستاتین یا لوواستاتین را محدود می‌کند (مثلاً سیمواستاتین حداکثر ۲۰ میلی‌گرم).",
    ),
    rule(
        "fibrate", "statin", HIGH,
        "خطر آسیب شدید عضلانی (رابدومیولیز). این ترکیب معمولاً توصیه نمی‌شود.",
    ),
    rule(
        "statin", "statin", MODERATE,
        "دو داروی کاهندهٔ کلسترول از یک گروه؛ معمولاً یکی کافی است.",
    ),
    rule(
        "colchicine", "cyp3a4_strong", HIGH,
        "سطح کلشی‌سین به حد سمی می‌رسد. این ترکیب خطرناک است؛ فوراً با پزشک مشورت کنید.",
    ),
    rule(
        "colchicine", "statin", MODERATE,
        "گاهی خطر درد و آسیب عضلانی را بیشتر می‌کند؛ درد یا ضعف عضلانی را گزارش کنید.",
    ),
    rule(
        "fluoroquinolone", "corticosteroid", MODERATE,
        "خطر التهاب و پارگی تاندون (به‌خصوص تاندون آشیل در سالمندان) بیشتر می‌شود. درد پاشنه را فوراً گزارش کنید.",
    ),

    // ---- Stomach, diabetes, hormones ----
    rule(
        "clopidogrel", "ppi_cyp2c19", MODERATE,
        "امپرازول و ایزومپرازول اثر کلوپیدوگرل را کم می‌کنند. پنتوپرازول یا فاموتیدین جایگزین بهتری است.",
    ),
    rule(
        "ppi", "ppi", MODERATE,
        "دو داروی معده از یک گروه؛ معمولاً یکی کافی است.",
    ),
    rule(
        "levothyroxine", "ppi", MODERATE,
        "داروهای کاهندهٔ اسید ممکن است جذب لووتیروکسین را کم کنند؛ آزمایش TSH را طبق برنامه انجام دهید.",
    ),
    rule(
        "sulfonylurea", "hypoglycemia_potentiator", MODERATE,
        "خطر افت شدید قند خون بیشتر می‌شود. قند خون را بیشتر کنترل کنید.",
    ),
    rule(
        "sulfonylurea", "sulfonylurea", MODERATE,
        "دو داروی هم‌گروه دیابت با هم خطر افت قند را زیاد می‌کنند؛ معمولاً یکی کافی است.",
    ),
    rule(
        "sulfonylurea insulin", "beta_blocker", MODERATE,
        "بتابلاکر می‌تواند علائم افت قند (تپش و لرزش) را پنهان کند. تعریق همچنان نشانهٔ افت قند است.",
    ),
    rule(
        "enzyme_inducer", "oral_contraceptive", HIGH,
        "اثر قرص ضدبارداری کم می‌شود و ممکن است بارداری رخ دهد. یک روش پیشگیری دیگر هم لازم است.",
    ),
    rule(
        "theophylline", "cyp1a2_strong", HIGH,
        "سطح تئوفیلین بالا می‌رود (تهوع، تپش، تشنج). دوز باید کم شود یا داروی دیگری انتخاب شود.",
    ),
    rule(
        "theophylline", "macrolide", MODERATE,
        "سطح تئوفیلین ممکن است بالا برود؛ به تهوع و تپش توجه کنید.",
    ),
    rule(
        "methotrexate", "cotrimoxazole", HIGH,
        "سمیت متوترکسات (کاهش شدید سلول‌های خونی) زیاد می‌شود. این ترکیب خطرناک است.",
    ),
    rule(
        "methotrexate", "nsaid ppi", MODERATE,
        "سطح متوترکسات ممکن است بالا برود، به‌خصوص با دوزهای بالا. به پزشک بگویید.",
    ),
    rule(
        "isotretinoin", "tetracycline", HIGH,
        "خطر بالا رفتن فشار داخل جمجمه (سردرد شدید، تاری دید). با هم مصرف نشوند.",
    ),

    // ---- More specific pairs ----
    rule(
        "ergot", "cyp3a4_strong", HIGH,
        "سطح ارگوتامین به حد خطرناک می‌رسد و ممکن است خون‌رسانی دست و پا یا مغز قطع شود. با هم مصرف نشوند.",
    ),
    rule(
        "ergot", "triptan", HIGH,
        "هر دو رگ‌ها را جمع می‌کنند؛ تا ۲۴ ساعت از هم فاصله داشته باشند.",
    ),
    rule(
        "fenofibrate", "statin", MODERATE,
        "خطر درد و آسیب عضلانی کمی بیشتر می‌شود؛ درد یا ضعف عضلانی را گزارش کنید.",
    ),
    rule(
        "repaglinide", "fibrate", HIGH,
        "جم‌فیبروزیل سطح رپاگلیناید را چند برابر می‌کند و افت شدید قند می‌دهد. این ترکیب ممنوع است.",
    ),
    rule(
        "ticagrelor", "cyp3a4_strong", HIGH,
        "سطح تیکاگرلور بالا می‌رود و خطر خونریزی زیاد می‌شود. با هم مصرف نشوند.",
    ),
    rule(
        "rate_ccb", "beta_blocker", MODERATE,
        "هر دو ضربان قلب را کند می‌کنند؛ ممکن است ضربان یا فشار بیش از حد پایین بیاید.",
    ),
    rule(
        "rate_ccb", "digoxin", MODERATE,
        "سطح دیگوکسین بالا می‌رود و ضربان کندتر می‌شود؛ به علائم مسمومیت توجه کنید.",
    ),
    rule(
        "ace_inhibitor", "neprilysin", HIGH,
        "با هم خطر ورم شدید صورت و گلو (آنژیوادم) دارند؛ بین قطع یکی و شروع دیگری دست‌کم ۳۶ ساعت فاصله لازم است.",
    ),
    rule(
        "sofosbuvir", "amiodarone", HIGH,
        "خطر کندی شدید و خطرناک ضربان قلب. این ترکیب معمولاً ممنوع است.",
    ),
    rule(
        "opioid_antagonist", "opioid", HIGH,
        "نالترکسون اثر مسکن‌ها و داروهای اپیوئیدی را از بین می‌برد و در مصرف‌کنندهٔ اپیوئید علائم ترک شدید ایجاد می‌کند.",
    ),
    rule(
        "disulfiram", "nitroimidazole metronidazole", HIGH,
        "مصرف هم‌زمان ممکن است گیجی و روان‌پریشی ایجاد کند. با هم مصرف نشوند.",
    ),
    rule(
        "clozapine", "cyp1a2_strong", HIGH,
        "سطح کلوزاپین چند برابر می‌شود (خواب‌آلودگی شدید، تشنج، افت فشار).",
    ),
    rule(
        "tamoxifen", "cyp2d6_strong", HIGH,
        "این دارو اثر تاموکسیفن را در پیشگیری از عود سرطان کم می‌کند؛ ضدافسردگی دیگری (مثل ونلافاکسین) بهتر است.",
    ),
    rule(
        "azathioprine", "allopurinol xanthine_oxidase", HIGH,
        "سطح آزاتیوپرین به حد سمی می‌رسد (کاهش شدید سلول‌های خونی). دوز باید بسیار کم شود یا این ترکیب مصرف نشود.",
    ),
    rule(
        "reduces_contraceptive", "oral_contraceptive", HIGH,
        "اثر قرص ضدبارداری کم می‌شود و ممکن است بارداری رخ دهد. یک روش پیشگیری دیگر هم لازم است.",
    ),
    rule(
        "antifibrinolytic", "oral_contraceptive", MODERATE,
        "هر دو خطر لخته را بالا می‌برند؛ پزشک باید از مصرف هم‌زمان خبر داشته باشد.",
    ),
    rule(
        "seizure_risk", "tramadol", MODERATE,
        "هر دو آستانهٔ تشنج را پایین می‌آورند؛ خطر تشنج بیشتر می‌شود.",
    ),

    // ---- Supplements ----
    rule(
        "warfarin", "vitamin_k", MODERATE,
        "ویتامین K اثر وارفارین را کم می‌کند. مصرف آن باید ثابت باشد و با پزشک هماهنگ شود، چون INR تغییر می‌کند.",
    ),
    rule(
        "warfarin", "coq10 ginseng", MODERATE,
        "این مکمل ممکن است اثر وارفارین را کم کند؛ INR را زودتر چک کنید.",
    ),
    rule(
        "warfarin", "glucosamine cranberry", MODERATE,
        "این مکمل ممکن است INR را بالا ببرد و خطر خونریزی را بیشتر کند.",
    ),
    rule(
        "anticoagulant antiplatelet", "bleeding_supplement", MODERATE,
        "این مکمل (به‌خصوص در دوز بالا) خطر خونریزی را کمی بیشتر می‌کند. به پزشک بگویید.",
    ),
    rule(
        "isotretinoin", "vitamin_a", HIGH,
        "ویتامین A همراه ایزوترتینوئین می‌تواند مسمومیت با ویتامین A ایجاد کند (سردرد شدید، ریزش مو، آسیب کبد). با هم مصرف نشوند.",
    ),

    // ---- Doses that only need spacing in time ----
    spacing(
        "levothyroxine", "polyvalent_cation", before = 240, after = 240,
        text = "کلسیم، آهن، منیزیم، روی و آنتی‌اسید جذب لووتیروکسین را کم می‌کنند. لووتیروکسین صبح ناشتا، و این‌ها دست‌کم ۴ ساعت بعد (مثلاً با ناهار یا شام).",
    ),
    spacing(
        "ciprofloxacin", "polyvalent_cation", before = 120, after = 360,
        text = "این مواد جذب سیپروفلوکساسین را به‌شدت کم می‌کنند. سیپروفلوکساسین را دست‌کم ۲ ساعت قبل یا ۶ ساعت بعد از آن‌ها بخورید.",
    ),
    spacing(
        "levofloxacin ofloxacin", "polyvalent_cation", before = 120, after = 120,
        text = "این مواد جذب این آنتی‌بیوتیک را کم می‌کنند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "tetracycline", "polyvalent_cation", before = 120, after = 180,
        text = "این مواد جذب آنتی‌بیوتیک‌های تتراسایکلین را کم می‌کنند. آنتی‌بیوتیک را ۲ ساعت قبل یا ۳ ساعت بعد از آن‌ها بخورید.",
    ),
    spacing(
        "bisphosphonate", "polyvalent_cation", before = 30, after = 120,
        text = "داروی پوکی استخوان را صبح ناشتا بخورید و کلسیم، آهن یا آنتی‌اسید را دست‌کم ۳۰ دقیقه بعد (بهتر است با ناهار یا شام).",
    ),
    spacing(
        "iron", "calcium antacid", before = 120, after = 120,
        text = "کلسیم و آنتی‌اسید جذب آهن را کم می‌کنند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "levodopa", "iron", before = 120, after = 120,
        text = "آهن جذب لوودوپا را کم می‌کند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "gabapentin", "antacid", before = 120, after = 120,
        text = "آنتی‌اسید جذب گاباپنتین را کم می‌کند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "azithromycin", "antacid", before = 120, after = 120,
        text = "آنتی‌اسید جذب آزیترومایسین را کم می‌کند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "rosuvastatin", "antacid", before = 120, after = 120,
        text = "آنتی‌اسید جذب رزوواستاتین را کم می‌کند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "phenytoin", "antacid", before = 120, after = 120,
        text = "آنتی‌اسید جذب فنی‌توئین را کم می‌کند؛ دست‌کم ۲ ساعت فاصله بدهید.",
    ),
    spacing(
        "probiotic", "antibiotic", before = 120, after = 120,
        text = "آنتی‌بیوتیک باکتری‌های مفید پروبیوتیک را از بین می‌برد؛ پروبیوتیک را دست‌کم ۲ ساعت قبل یا بعد از آنتی‌بیوتیک بخورید.",
    ),
    spacing(
        "aspirin", "ibuprofen", before = 30, after = 480,
        text = "بروفن ممکن است اثر محافظتی آسپرین بر قلب را کم کند. آسپرین را دست‌کم ۳۰ دقیقه قبل یا ۸ ساعت بعد از بروفن بخورید.",
    ),
)

object InteractionChecker {

    private const val DAY = 24 * 60

    /** Every interaction between pairs of [meds], most serious first. */
    fun check(meds: List<Medication>): List<Finding> {
        val matched = meds.map { it to DrugMatcher.match(it.name) }
        val findings = mutableListOf<Finding>()
        for (i in matched.indices) {
            for (j in i + 1 until matched.size) {
                val (a, drugsA) = matched[i]
                val (b, drugsB) = matched[j]
                findings += checkPair(a, drugsA, b, drugsB)
            }
        }
        return findings.sortedBy { it.rank }
    }

    /** Findings that involve [med], checked against [others] (e.g. a medication being edited). */
    fun checkAgainst(med: Medication, others: List<Medication>): List<Finding> {
        val drugs = DrugMatcher.match(med.name)
        if (drugs.isEmpty()) return emptyList()
        return others.filter { it.id != med.id }
            .flatMap { checkPair(med, drugs, it, DrugMatcher.match(it.name)) }
            .sortedBy { it.rank }
    }

    private fun checkPair(a: Medication, drugsA: List<Drug>, b: Medication, drugsB: List<Drug>): List<Finding> {
        if (drugsA.isEmpty() || drugsB.isEmpty()) return emptyList()
        val clinical = mutableListOf<Finding>()
        val timing = mutableListOf<Finding>()

        drugsA.filter { x -> drugsB.any { it.id == x.id } }.forEach { same ->
            clinical += Finding(
                a, b, MODERATE,
                "هر دو شامل «${same.fa}» هستند؛ ممکن است این دارو دو بار مصرف شود. اگر عمدی نیست، $ASK",
            )
        }

        for (x in drugsA) for (y in drugsB) {
            if (x.id == y.id) continue
            for (rule in interactionRules) {
                val forward = matches(rule.left, x) && matches(rule.right, y)
                val backward = matches(rule.left, y) && matches(rule.right, x)
                if (!forward && !backward) continue
                if (rule.severity == TIMING) {
                    // Orientation matters: rule.left is the drug that must come "before".
                    val ok = if (forward) {
                        spacingOk(a.times, b.times, rule.before, rule.after)
                    } else {
                        spacingOk(b.times, a.times, rule.before, rule.after)
                    }
                    timing += Finding(a, b, TIMING, rule.text, spacingOk = ok)
                } else {
                    clinical += Finding(a, b, rule.severity, "${rule.text} $ASK")
                }
            }
        }

        // Keep only the most serious clinical message(s) for the pair; a specific HIGH warning
        // already says more than the generic MODERATE ones it overlaps with.
        val worst = clinical.minOfOrNull { it.severity.ordinal }
        val kept = clinical.filter { it.severity.ordinal == worst }.distinctBy { it.text }
        return kept + timing.distinctBy { it.text }
    }

    private fun matches(tags: Set<String>, drug: Drug) = drug.allTags.any { it in tags }

    /**
     * Whether doses of the "left" drug at [leftTimes] keep at least [before] minutes until the next
     * dose of the "right" drug and at least [after] minutes since the previous one, every day.
     */
    fun spacingOk(leftTimes: List<Int>, rightTimes: List<Int>, before: Int, after: Int): Boolean =
        leftTimes.all { l ->
            rightTimes.all { r ->
                val untilRight = Math.floorMod(r - l, DAY)
                val sinceRight = Math.floorMod(l - r, DAY)
                untilRight >= before && sinceRight >= after
            }
        }
}
