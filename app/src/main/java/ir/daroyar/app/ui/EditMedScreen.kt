package ir.daroyar.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Kind
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.Drug
import ir.daroyar.app.drugs.DrugDatabase
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.drugs.InteractionChecker
import ir.daroyar.app.drugs.isIndexed
import ir.daroyar.app.reminder.Schedule
import ir.daroyar.app.util.PERSIAN_WEEK
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.latinDigits
import ir.daroyar.app.util.persianName
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMedScreen(
    initial: Medication,
    otherMeds: List<Medication>,
    onSave: (Medication) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var dose by rememberSaveable { mutableStateOf(initial.dose) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var times by rememberSaveable { mutableStateOf(ArrayList(initial.times)) }
    var specificDays by rememberSaveable { mutableStateOf(!initial.everyDay) }
    var days by rememberSaveable { mutableStateOf(HashSet(initial.days)) }
    var trackStock by rememberSaveable { mutableStateOf(initial.stock != null) }
    var stockText by rememberSaveable { mutableStateOf(initial.stock?.toString().orEmpty()) }
    var perDoseText by rememberSaveable { mutableStateOf(initial.perDose.toString()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf(initial.kind) }
    var kindTouched by rememberSaveable { mutableStateOf(false) }
    val noun = kind.noun

    // A new entry gets the drug's best times automatically until the user changes the times themselves.
    var timesTouched by rememberSaveable { mutableStateOf(initial.id != 0L) }
    var autoTimesFor by rememberSaveable { mutableStateOf<String?>(null) }

    fun setTimes(list: List<Int>, byUser: Boolean = true) {
        times = ArrayList(list.distinct().sorted())
        if (byUser) {
            timesTouched = true
            autoTimesFor = null
        }
    }

    fun setDays(set: Set<DayOfWeek>) {
        days = HashSet(set)
    }

    /** Once a week on the chosen day, or on today's weekday if none or several are chosen. */
    fun makeWeekly() {
        specificDays = true
        if (days.size != 1) setDays(setOf(LocalDate.now().dayOfWeek))
    }

    fun pickTime(start: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(context, { _, hour, minute -> onPicked(hour * 60 + minute) }, start / 60, start % 60, true).show()
    }

    val matched = remember(name) { DrugMatcher.match(name) }
    // A new entry follows what the name turns out to be, until the user picks the kind themselves.
    LaunchedEffect(matched) {
        if (initial.id == 0L && !kindTouched && matched.isNotEmpty()) {
            kind = if (matched.all { it.isSupplement }) Kind.SUPPLEMENT else Kind.MEDICINE
        }
        if (!timesTouched) {
            val withTimes = matched.firstOrNull { it.suggestedTimes != null }
            if (withTimes != null) {
                setTimes(withTimes.suggestedTimes.orEmpty(), byUser = false)
                autoTimesFor = withTimes.fa
                if (withTimes.weekly && !specificDays) makeWeekly()
            } else if (autoTimesFor != null) {
                // The name changed to something without a best time: don't keep the previous drug's times.
                setTimes(emptyList(), byUser = false)
                autoTimesFor = null
            }
        }
    }

    fun pickDrug(drug: Drug) {
        name = drug.fa
        namePicked = true
    }
    // Autocomplete: shown while the name field has focus, until a name is picked or typed in full.
    var nameFocused by remember { mutableStateOf(false) }
    var namePicked by rememberSaveable { mutableStateOf(initial.name.isNotBlank()) }
    val suggestions = remember(name) { DrugMatcher.suggest(name) }
    val showSuggestions = nameFocused && !namePicked && suggestions.isNotEmpty() &&
        suggestions.none { it.fa == name.trim() }
    val warnings = remember(name, times, otherMeds) {
        InteractionChecker.checkAgainst(initial.copy(name = name, times = times.toList()), otherMeds.filter { it.active })
            .filter { it.rank < 3 }
    }

    val scheduledDays = if (specificDays) Schedule.normalizeDays(days) else emptySet()
    // No warning while the user is still picking days.
    val tooFrequent = remember(matched, specificDays, days) {
        if (specificDays && days.isEmpty()) emptyList() else matched.filter { it.tooFrequent(scheduledDays) }
    }

    val stock = stockText.latinDigits().trim().toIntOrNull()
    val nameError = showErrors && name.isBlank()
    val timesError = showErrors && times.isEmpty()
    val daysError = showErrors && specificDays && days.isEmpty()
    val stockError = showErrors && trackStock && stock == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initial.id == 0L) (if (kind == Kind.SUPPLEMENT) "مکمل جدید" else "داروی جدید") else "ویرایش $noun",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف $noun")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == Kind.MEDICINE,
                    onClick = { kind = Kind.MEDICINE; kindTouched = true },
                    label = { Text("دارو") },
                    leadingIcon = { Icon(Icons.Outlined.Medication, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = kind == Kind.SUPPLEMENT,
                    onClick = { kind = Kind.SUPPLEMENT; kindTouched = true },
                    label = { Text("مکمل و ویتامین") },
                    leadingIcon = { Icon(Icons.Outlined.Spa, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    namePicked = false
                },
                label = { Text("نام $noun *") },
                placeholder = { Text(if (kind == Kind.SUPPLEMENT) "مثلاً ویتامین د یا امگا ۳" else "مثلاً متفورمین ۵۰۰") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("نام $noun را وارد کنید") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { nameFocused = it.isFocused },
            )
            if (showSuggestions) {
                SuggestionList(suggestions, onPick = { pickDrug(it) })
            }
            if (kind == Kind.SUPPLEMENT && name.isBlank()) {
                Text(
                    "مکمل‌های رایج (بهترین ساعت مصرف خودکار تنظیم می‌شود):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    commonSupplements.forEach { drug ->
                        AssistChip(onClick = { pickDrug(drug) }, label = { Text(drug.fa) })
                    }
                }
            }
            matched.forEach { drug ->
                DrugInfoCard(
                    drug = drug,
                    currentTimes = times,
                    onUseSuggestedTimes = if (matched.size == 1) ({ setTimes(it) }) else null,
                )
            }
            OutlinedTextField(
                value = dose,
                onValueChange = { dose = it },
                label = { Text("مقدار هر نوبت") },
                placeholder = { Text(if (kind == Kind.SUPPLEMENT) "مثلاً ۱ کپسول یا ۱۰۰۰ واحد" else "مثلاً ۱ قرص یا ۵ میلی‌لیتر") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("ساعت‌های مصرف")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Schedule.presets.forEach { (label, preset) ->
                    FilterChip(
                        selected = times == preset,
                        onClick = { setTimes(preset) },
                        label = { Text(label) },
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                times.forEach { minute ->
                    InputChip(
                        selected = false,
                        onClick = { pickTime(minute) { picked -> setTimes(times - minute + picked) } },
                        label = { Text(formatMinute(minute), style = MaterialTheme.typography.titleSmall) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "حذف ساعت",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { setTimes(times - minute) },
                            )
                        },
                    )
                }
            }
            OutlinedButton(onClick = {
                val start = times.lastOrNull()?.let { (it + 4 * 60) % (24 * 60) } ?: (8 * 60)
                pickTime(start) { setTimes(times + it) }
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("افزودن ساعت")
            }
            val autoFor = autoTimesFor
            val asNeeded = matched.firstOrNull()?.takeIf { matched.all { d -> d.suggestedTimes == null } }
            when {
                timesError -> Text(
                    "دست‌کم یک ساعت مصرف انتخاب کنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                autoFor != null -> AutoTimesNote(
                    "⏰ دارویار بهترین ساعت مصرف «$autoFor» را تنظیم کرد. اگر این ساعت برایتان مناسب نیست، روی آن بزنید و عوضش کنید، یا ساعت دیگری اضافه کنید.",
                )
                asNeeded != null && times.isEmpty() -> AutoTimesNote(
                    if (asNeeded.isIndexed) {
                        "برای «${asNeeded.fa}» ساعت پیشنهادی در دارویار ثبت نشده؛ طبق نسخه یا بروشور، ساعت‌های مصرف را خودتان اضافه کنید."
                    } else {
                        "«${asNeeded.fa}» معمولاً در صورت نیاز یا طبق نسخه مصرف می‌شود و ساعت ثابتی ندارد؛ اگر پزشک نوبت ثابت گفته، ساعت‌ها را خودتان اضافه کنید."
                    },
                )
                else -> Text(
                    "برای تغییر هر ساعت روی آن بزنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("روزهای مصرف")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !specificDays, onClick = { specificDays = false }, label = { Text("هر روز") })
                FilterChip(selected = specificDays, onClick = { specificDays = true }, label = { Text("روزهای خاص") })
                FilterChip(
                    selected = specificDays && days.size == 1,
                    onClick = { makeWeekly() },
                    label = { Text("هفته‌ای یک بار") },
                )
            }
            if (specificDays) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PERSIAN_WEEK.forEach { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { setDays(if (day in days) days - day else days + day) },
                            label = { Text(day.persianName()) },
                        )
                    }
                }
                Text(
                    if (days.isEmpty()) "دست‌کم یک روز را انتخاب کنید" else "فقط در روزهای انتخاب‌شده یادآوری می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            tooFrequent.forEach { drug ->
                WeeklyDrugWarning(
                    drug = drug,
                    days = scheduledDays,
                    actionLabel = "هفته‌ای یک بار تنظیم شود",
                    onAction = { makeWeekly() },
                )
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("پیگیری موجودی")
                    Text(
                        "با هر نوبت مصرف کم می‌شود و پیش از تمام شدن خبر می‌دهد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }
            if (trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("تعداد موجود") },
                        isError = stockError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = perDoseText,
                        onValueChange = { perDoseText = it },
                        label = { Text("مصرف هر نوبت") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("توضیحات") },
                placeholder = { Text("مثلاً بعد از غذا، با یک لیوان آب") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (warnings.isNotEmpty()) {
                SectionTitle("هشدار با داروها و مکمل‌های دیگر شما")
                warnings.forEach { FindingCard(it) }
                Disclaimer()
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (name.isBlank() || times.isEmpty() || (specificDays && days.isEmpty()) || (trackStock && stock == null)) {
                        showErrors = true
                    } else {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                dose = dose.trim(),
                                notes = notes.trim(),
                                times = times.distinct().sorted(),
                                days = scheduledDays,
                                kind = kind,
                                stock = if (trackStock) stock?.coerceAtLeast(0) else null,
                                perDose = perDoseText.latinDigits().trim().toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("ذخیره", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف ${initial.name}؟") },
            text = { Text("یادآوری‌های این $noun هم حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("انصراف") }
            },
        )
    }
}

/** Drop-down style list under the name field; one row per drug with its Persian and English name and use. */
@Composable
private fun SuggestionList(drugs: List<Drug>, onPick: (Drug) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            drugs.forEachIndexed { i, drug ->
                if (i > 0) HorizontalDivider(color = colors.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(drug) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (drug.isSupplement) Icons.Outlined.Spa else Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(drug.fa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (drug.en != drug.fa) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    drug.en,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            drug.use,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoTimesNote(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

private val commonSupplements: List<Drug> by lazy { DrugDatabase.drugs.filter { it.isSupplement } }

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
