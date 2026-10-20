package ir.daroyar.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.daroyar.app.drugs.Drug
import ir.daroyar.app.drugs.Finding
import ir.daroyar.app.drugs.Severity
import ir.daroyar.app.util.formatMinute

const val MEDICAL_DISCLAIMER =
    "این اطلاعات خلاصه و عمومی است و جای توصیهٔ پزشک یا داروساز را نمی‌گیرد. دستور پزشکِ شما مقدم است؛ هیچ دارویی را خودسرانه قطع یا کم و زیاد نکنید."

/** Summary of one reference drug: what it's for, when to take it and the main caution. */
@Composable
fun DrugInfoCard(
    drug: Drug,
    currentTimes: List<Int>?,
    onUseSuggestedTimes: ((List<Int>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(8.dp))
                Text(drug.fa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(drug.en, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            InfoSection("برای چه مفید است", drug.use)
            InfoSection("دربارهٔ دارو", drug.about)
            InfoSection("بهترین زمان مصرف", drug.timing)

            val suggested = drug.suggestedTimes
            if (suggested != null && onUseSuggestedTimes != null && suggested != currentTimes) {
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = { onUseSuggestedTimes(suggested) }) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ساعت پیشنهادی: " + suggested.joinToString(" و ") { formatMinute(it) })
                }
            }

            drug.caution?.let {
                Spacer(Modifier.height(12.dp))
                Row {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.tertiary)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Spacer(Modifier.height(10.dp))
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyMedium)
}

private data class FindingStyle(val label: String, val icon: ImageVector, val container: Color, val content: Color)

@Composable
private fun styleOf(finding: Finding): FindingStyle {
    val c = MaterialTheme.colorScheme
    return when {
        finding.severity == Severity.HIGH ->
            FindingStyle("تداخل جدی", Icons.Filled.Error, c.errorContainer, c.onErrorContainer)
        finding.severity == Severity.MODERATE ->
            FindingStyle("احتیاط", Icons.Filled.Warning, c.tertiaryContainer, c.onTertiaryContainer)
        finding.spacingOk ->
            FindingStyle("فاصلهٔ زمانی رعایت شده", Icons.Filled.CheckCircle, c.surfaceContainer, c.onSurface)
        else ->
            FindingStyle("ساعت‌ها را جابه‌جا کنید", Icons.Outlined.Schedule, c.secondaryContainer, c.onSecondaryContainer)
    }
}

/** One interaction between two of the user's medications. */
@Composable
fun FindingCard(finding: Finding, modifier: Modifier = Modifier) {
    val style = styleOf(finding)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = style.container, contentColor = style.content),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(style.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(style.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${finding.medA.name} + ${finding.medB.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(finding.text, style = MaterialTheme.typography.bodyMedium)
            if (finding.severity == Severity.TIMING) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "ساعت‌های فعلی: ${finding.medA.name} ${finding.medA.times.joinToString("، ") { formatMinute(it) }}" +
                        " · ${finding.medB.name} ${finding.medB.times.joinToString("، ") { formatMinute(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun Disclaimer(modifier: Modifier = Modifier) {
    Text(
        MEDICAL_DISCLAIMER,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}
