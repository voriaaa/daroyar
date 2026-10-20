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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Medication
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatMinute

@Composable
fun MedsScreen(
    meds: List<Medication>,
    onEdit: (Medication) -> Unit,
    onToggleActive: (Medication, Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
            MedCard(med, onClick = { onEdit(med) }, onToggleActive = { onToggleActive(med, it) })
        }
    }
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
