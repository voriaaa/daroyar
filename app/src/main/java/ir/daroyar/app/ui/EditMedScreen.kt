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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.Schedule
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.latinDigits

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMedScreen(
    initial: Medication,
    onSave: (Medication) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var dose by rememberSaveable { mutableStateOf(initial.dose) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    var times by rememberSaveable { mutableStateOf(ArrayList(initial.times)) }
    var trackStock by rememberSaveable { mutableStateOf(initial.stock != null) }
    var stockText by rememberSaveable { mutableStateOf(initial.stock?.toString().orEmpty()) }
    var perDoseText by rememberSaveable { mutableStateOf(initial.perDose.toString()) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun setTimes(list: List<Int>) {
        times = ArrayList(list.distinct().sorted())
    }

    fun pickTime(start: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(context, { _, hour, minute -> onPicked(hour * 60 + minute) }, start / 60, start % 60, true).show()
    }

    val stock = stockText.latinDigits().trim().toIntOrNull()
    val nameError = showErrors && name.isBlank()
    val timesError = showErrors && times.isEmpty()
    val stockError = showErrors && trackStock && stock == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial.id == 0L) "داروی جدید" else "ویرایش دارو", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف دارو")
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام دارو *") },
                placeholder = { Text("مثلاً متفورمین ۵۰۰") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("نام دارو را وارد کنید") }) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dose,
                onValueChange = { dose = it },
                label = { Text("مقدار هر نوبت") },
                placeholder = { Text("مثلاً ۱ قرص یا ۵ میلی‌لیتر") },
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
            Text(
                if (timesError) "دست‌کم یک ساعت مصرف انتخاب کنید" else "برای تغییر هر ساعت روی آن بزنید.",
                style = MaterialTheme.typography.bodySmall,
                color = if (timesError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (name.isBlank() || times.isEmpty() || (trackStock && stock == null)) {
                        showErrors = true
                    } else {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                dose = dose.trim(),
                                notes = notes.trim(),
                                times = times.distinct().sorted(),
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
            text = { Text("یادآوری‌های این دارو هم حذف می‌شود.") },
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
