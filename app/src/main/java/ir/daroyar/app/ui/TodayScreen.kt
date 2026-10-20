package ir.daroyar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.MedStore
import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.DoseStatus
import ir.daroyar.app.reminder.Schedule
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.formatPersianDate
import java.time.LocalDateTime

private data class Dose(val med: Medication, val minute: Int, val status: DoseStatus)

@Composable
fun TodayScreen(
    meds: List<Medication>,
    taken: Set<String>,
    now: LocalDateTime,
    alertCount: Int,
    onToggle: (Medication, Int, Boolean) -> Unit,
    onAdd: () -> Unit,
    onShowAlerts: () -> Unit,
) {
    val today = now.toLocalDate()
    val doses = remember(meds, taken, now) {
        meds.filter { it.active && it.isDueOn(today) }
            .flatMap { med -> med.times.map { med to it } }
            .sortedWith(compareBy({ it.second }, { it.first.name }))
            .map { (med, minute) ->
                val isTaken = MedStore.doseKey(med.id, today, minute) in taken
                Dose(med, minute, Schedule.status(minute, now.toLocalTime(), isTaken))
            }
    }
    val takenCount = doses.count { it.status == DoseStatus.TAKEN }
    val nextUpcoming = doses.firstOrNull { it.status == DoseStatus.UPCOMING }
    val lowStock = meds.filter { it.active && it.lowStock }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SummaryCard(formatPersianDate(today), takenCount, doses.size) }
        if (alertCount > 0) item { AlertsCard(alertCount, onShowAlerts) }
        if (lowStock.isNotEmpty()) item { LowStockCard(lowStock) }
        if (doses.isEmpty()) {
            item {
                EmptyState(
                    title = if (meds.isEmpty()) "هنوز دارویی ثبت نکرده‌اید" else "برای امروز نوبتی نیست",
                    body = if (meds.any { it.active }) {
                        "داروها و مکمل‌های شما امروز نوبتی ندارند. برای مورد تازه، آن را با ساعت و روزهای مصرفش اضافه کنید."
                    } else {
                        "دارو یا مکمل (مثل ویتامین‌ها) و ساعت‌های مصرفش را اضافه کنید تا سر وقت با اعلان یادآوری شود."
                    },
                    action = "افزودن دارو",
                    onAction = onAdd,
                )
            }
        }
        items(doses, key = { "${it.med.id}-${it.minute}" }) { dose ->
            DoseCard(
                dose = dose,
                isNext = dose == nextUpcoming,
                onToggle = { onToggle(dose.med, dose.minute, dose.status != DoseStatus.TAKEN) },
            )
        }
    }
}

@Composable
private fun SummaryCard(date: String, taken: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(date, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (total == 0) {
                Text("روز خوبی داشته باشید 🌿", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    if (taken == total) "همهٔ داروهای امروز مصرف شد ✓" else "${taken.fa()} از ${total.fa()} نوبت مصرف شد",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { taken.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsCard(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${count.fa()} هشدار دربارهٔ داروهای شما", fontWeight = FontWeight.Bold)
                Text("تداخل دارویی یا فاصلهٔ زمانی نامناسب؛ برای دیدن جزئیات بزنید.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LowStockCard(meds: List<Medication>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("موجودی رو به اتمام", fontWeight = FontWeight.Bold)
                meds.forEach {
                    Text("${it.name}: ${(it.stock ?: 0).fa()} عدد باقی مانده", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseCard(dose: Dose, isNext: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (container, badge, badgeColor) = when (dose.status) {
        DoseStatus.TAKEN -> Triple(colors.surfaceContainer, "مصرف شد", colors.primary)
        DoseStatus.LATE -> Triple(colors.tertiaryContainer, "دیر شده", colors.tertiary)
        DoseStatus.UPCOMING -> Triple(
            if (isNext) colors.secondaryContainer else colors.surfaceContainerHigh,
            if (isNext) "نوبت بعدی" else "",
            colors.secondary,
        )
    }
    val faded = dose.status == DoseStatus.TAKEN
    Card(onClick = onToggle, colors = CardDefaults.cardColors(containerColor = container)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatMinute(dose.minute),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (faded) colors.onSurfaceVariant else Color.Unspecified,
                modifier = Modifier.widthIn(min = 64.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dose.med.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (faded) colors.onSurfaceVariant else Color.Unspecified,
                )
                val kindLabel = if (dose.med.isSupplement) "مکمل" else ""
                val detail = listOf(kindLabel, dose.med.dose, dose.med.notes).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(detail.fa(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (badge.isNotEmpty()) {
                    Text(badge, style = MaterialTheme.typography.labelMedium, color = badgeColor, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onToggle) {
                if (dose.status == DoseStatus.TAKEN) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "لغو مصرف", tint = colors.primary, modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "علامت مصرف", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Medication,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAction) { Text(action) }
    }
}
