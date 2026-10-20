package ir.daroyar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Kind
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.drugs.Finding
import ir.daroyar.app.drugs.isIndexed
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatDays
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.formatRange
import java.time.LocalDate

@Composable
fun MedsScreen(
    meds: List<Medication>,
    findings: List<Finding>,
    onOpen: (Medication) -> Unit,
    onToggleActive: (Medication, Boolean) -> Unit,
    onAdd: (Kind) -> Unit,
) {
    // Index-only names are recognized but their interactions aren't checked.
    val unknown = remember(meds) { meds.filter { it.active && DrugMatcher.match(it.name).none { d -> !d.isIndexed } } }
    var showSpacingOk by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (meds.count { it.active } >= 2) {
            item { SectionHeader("تداخل‌ها") }
            val important = findings.filter { it.rank < 3 }
            val satisfied = findings.filter { it.rank == 3 }
            if (important.isEmpty()) {
                item {
                    Text(
                        "تداخل مهمی بین داروها و مکمل‌های فعال شما پیدا نشد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(important) { FindingCard(it) }
            if (satisfied.isNotEmpty()) {
                item {
                    TextButton(onClick = { showSpacingOk = !showSpacingOk }) {
                        Text(
                            if (showSpacingOk) "پنهان کردن فاصله‌های رعایت‌شده"
                            else "${satisfied.size.fa()} مورد فاصلهٔ زمانی که رعایت شده",
                        )
                    }
                }
                if (showSpacingOk) items(satisfied) { FindingCard(it) }
            }
            if (unknown.isNotEmpty()) {
                item {
                    Text(
                        "تداخل این موارد در دارویار بررسی نمی‌شود: " + unknown.joinToString("، ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Disclaimer() }
        }
        if (meds.isEmpty()) {
            item {
                EmptyState(
                    title = "فهرست خالی است",
                    body = "داروها و مکمل‌هایی مثل ویتامین‌ها را با ساعت مصرفشان اضافه کنید تا سر وقت یادآوری شوند.",
                    action = "افزودن دارو",
                    onAction = { onAdd(Kind.MEDICINE) },
                )
            }
        }
        val (supplements, medicines) = meds.sortedBy { it.name }.partition { it.isSupplement }
        listOf(Kind.MEDICINE to medicines, Kind.SUPPLEMENT to supplements).forEach { (kind, list) ->
            if (meds.isNotEmpty()) {
                item(key = "header-${kind.name}") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(if (kind == Kind.MEDICINE) "داروها" else "مکمل‌ها و ویتامین‌ها", Modifier.weight(1f))
                        TextButton(onClick = { onAdd(kind) }) { Text("+ افزودن ${kind.noun}") }
                    }
                }
            }
            items(list, key = { it.id }) { med ->
                MedCard(med, onClick = { onOpen(med) }, onToggleActive = { onToggleActive(med, it) })
            }
        }
        if (meds.isNotEmpty() && supplements.isEmpty()) {
            item {
                Text(
                    "ویتامین یا مکمل هم مصرف می‌کنید؟ آن را اضافه کنید تا بهترین ساعت مصرفش پیشنهاد و یادآوری شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedCard(med: Medication, onClick: () -> Unit, onToggleActive: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedIcon(med, Modifier.alpha(if (med.active) 1f else 0.45f), size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    med.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (med.active) colors.onSurface else colors.onSurfaceVariant,
                )
                if (med.dose.isNotBlank()) {
                    Text(med.dose.fa(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                val use = remember(med.name) { DrugMatcher.match(med.name).firstOrNull()?.use }
                if (use != null) {
                    Text(
                        use,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    med.times.joinToString("  ·  ") { formatMinute(it) } +
                        if (med.everyDay) "" else "  ·  ${formatDays(med.days)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                )
                med.range?.let { range ->
                    val ended = range.endedBefore(LocalDate.now())
                    Text(
                        formatRange(range) + if (ended) " (تمام شده)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ended) colors.onSurfaceVariant else colors.primary,
                    )
                }
                val weeklyDrug = remember(med.name, med.days) {
                    DrugMatcher.match(med.name).firstOrNull { it.tooFrequent(med.days) }
                }
                if (weeklyDrug != null) {
                    Text(
                        "⚠️ ${weeklyDrug.fa} معمولاً فقط هفته‌ای یک بار است",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
                med.stock?.let { stock ->
                    Text(
                        "موجودی: ${stock.fa()} عدد" + if (med.lowStock) " (رو به اتمام)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (med.lowStock) colors.tertiary else colors.onSurfaceVariant,
                    )
                }
                if (!med.active) {
                    Text("یادآوری متوقف شده", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = med.active, onCheckedChange = onToggleActive)
        }
    }
}
