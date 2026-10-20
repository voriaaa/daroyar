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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.drugs.Finding
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatMinute

@Composable
fun MedsScreen(
    meds: List<Medication>,
    findings: List<Finding>,
    onOpen: (Medication) -> Unit,
    onToggleActive: (Medication, Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    val unknown = remember(meds) { meds.filter { it.active && DrugMatcher.match(it.name).isEmpty() } }
    var showSpacingOk by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (meds.count { it.active } >= 2) {
            item { SectionHeader("تداخل‌های دارویی") }
            val important = findings.filter { it.rank < 3 }
            val satisfied = findings.filter { it.rank == 3 }
            if (important.isEmpty()) {
                item {
                    Text(
                        "تداخل مهمی بین داروهای فعال شما پیدا نشد.",
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
                        "این داروها در بانک دارویار نیستند و بررسی نشدند: " + unknown.joinToString("، ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Disclaimer() }
            item { SectionHeader("داروهای من") }
        }
        if (meds.isEmpty()) {
            item {
                EmptyState(
                    title = "فهرست داروها خالی است",
                    body = "برای هر دارو نام، مقدار و ساعت‌های مصرف را وارد کنید.",
                    action = "افزودن اولین دارو",
                    onAction = onAdd,
                )
            }
        }
        items(meds.sortedBy { it.name }, key = { it.id }) { med ->
            MedCard(med, onClick = { onOpen(med) }, onToggleActive = { onToggleActive(med, it) })
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
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
                    med.times.joinToString("  ·  ") { formatMinute(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                )
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
