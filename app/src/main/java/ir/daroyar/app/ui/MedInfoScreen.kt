package ir.daroyar.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.drugs.InteractionChecker
import ir.daroyar.app.drugs.isIndexed
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatDays
import ir.daroyar.app.util.formatMinute

/** What the app knows about one of the user's medications, and how it interacts with the others. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedInfoScreen(
    med: Medication,
    allMeds: List<Medication>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onApplyTimes: (List<Int>) -> Unit,
) {
    val drugs = remember(med.name) { DrugMatcher.match(med.name) }
    val findings = remember(med, allMeds) {
        InteractionChecker.checkAgainst(med, allMeds.filter { it.active })
    }
    var proposedTimes by remember { mutableStateOf<List<Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(med.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "ویرایش")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("برنامهٔ شما", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    if (med.dose.isNotBlank()) Text(med.dose.fa(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        med.times.joinToString("  ·  ") { formatMinute(it) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!med.everyDay) Text(formatDays(med.days), style = MaterialTheme.typography.bodyLarge)
                    med.stock?.let { Text("موجودی: ${it.fa()} عدد", style = MaterialTheme.typography.bodyMedium) }
                    if (med.notes.isNotBlank()) Text(med.notes, style = MaterialTheme.typography.bodyMedium)
                    if (!med.active) Text("یادآوری متوقف شده", style = MaterialTheme.typography.labelMedium)
                }
            }

            drugs.filter { it.tooFrequent(med.days) }.forEach { drug ->
                WeeklyDrugWarning(drug = drug, days = med.days, actionLabel = "ویرایش برنامه", onAction = onEdit)
            }

            if (drugs.isEmpty()) {
                Text(
                    "این ${med.noun} در بانک دارویار پیدا نشد. اگر نام عمومی آن (مثلاً «متفورمین» یا «ویتامین د» به‌جای نام تجاری) را بنویسید، احتمال شناسایی بیشتر است. برای اطلاعات و تداخل‌ها از داروساز بپرسید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            drugs.forEach { drug ->
                DrugInfoCard(
                    drug = drug,
                    currentTimes = med.times,
                    // Only a single-drug medication can take that drug's suggested times as-is.
                    onUseSuggestedTimes = if (drugs.size == 1) ({ proposedTimes = it }) else null,
                )
            }

            if (drugs.isNotEmpty()) {
                Text(
                    "تداخل با داروهای دیگر شما",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (findings.isEmpty()) {
                    Text(
                        if (drugs.all { it.isIndexed }) {
                            "تداخل‌های این ${med.noun} در دارویار بررسی نمی‌شود؛ دربارهٔ مصرف هم‌زمان با داروهای دیگرتان از داروساز بپرسید."
                        } else {
                            "تداخل مهمی با داروهای ثبت‌شدهٔ شما پیدا نشد."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                findings.forEach { FindingCard(it) }
            }

            Disclaimer()
        }
    }

    proposedTimes?.let { times ->
        AlertDialog(
            onDismissRequest = { proposedTimes = null },
            title = { Text("تغییر ساعت مصرف") },
            text = {
                Text(
                    "ساعت مصرف به ${times.joinToString(" و ") { formatMinute(it) }} تغییر کند؟ اگر پزشک ساعت دیگری گفته، همان را نگه دارید.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onApplyTimes(times)
                    proposedTimes = null
                }) { Text("تغییر بده") }
            },
            dismissButton = {
                TextButton(onClick = { proposedTimes = null }) { Text("انصراف") }
            },
        )
    }
}
