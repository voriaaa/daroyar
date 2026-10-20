package ir.daroyar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.daroyar.app.util.JalaliDate
import ir.daroyar.app.util.PERSIAN_WEEK
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatShortDate
import ir.daroyar.app.util.persianName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A small Jalali month calendar for picking a date range: the first tap picks the first day, the
 * second the last day; tapping again starts over.
 */
@Composable
fun DateRangePickerDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var month by remember {
        mutableStateOf(JalaliDate.from(initialStart ?: today).let { JalaliDate(it.year, it.month, 1) })
    }

    fun tap(day: LocalDate) {
        val s = start
        if (s == null || end != null || day.isBefore(s)) {
            start = day
            end = null
        } else {
            end = day
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بازهٔ تاریخ مصرف", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                val s = start
                val e = end
                Text(
                    when {
                        s == null -> "روز شروع را بزنید."
                        e == null -> "از ${formatShortDate(s, today)}؛ حالا روز پایان را بزنید."
                        else -> "از ${formatShortDate(s, today)} تا ${formatShortDate(e, today)} " +
                            "(${(ChronoUnit.DAYS.between(s, e) + 1).toInt().fa()} روز)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                MonthGrid(month, today, s, e, onMonth = { month = it }, onTap = ::tap)
            }
        },
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = { start?.let { s -> onConfirm(s, end ?: s) } },
            ) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
private fun MonthGrid(
    month: JalaliDate,
    today: LocalDate,
    start: LocalDate?,
    end: LocalDate?,
    onMonth: (JalaliDate) -> Unit,
    onTap: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // In right-to-left layout the first item sits on the right: previous month on the right, next on the left.
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMonth(month.plusMonths(-1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ماه قبل")
        }
        Text(
            "${month.monthName} ${month.year.fa()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onMonth(month.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ماه بعد")
        }
    }
    Row(Modifier.fillMaxWidth()) {
        PERSIAN_WEEK.forEach { day ->
            Text(
                day.persianName().take(1),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val first = month.toLocalDate()
    val blanks = PERSIAN_WEEK.indexOf(first.dayOfWeek)
    val cells = List<LocalDate?>(blanks) { null } +
        (0 until JalaliDate.monthLength(month.year, month.month)).map { first.plusDays(it.toLong()) }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    val isEnd = date == start || date == end
                    val inside = start != null && end != null && date.isAfter(start) && date.isBefore(end)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(vertical = 2.dp)
                            .background(if (inside) colors.primaryContainer else Color.Transparent)
                            .clickable { onTap(date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isEnd) colors.primary else Color.Transparent)
                                .then(
                                    if (date == today && !isEnd) Modifier.border(1.dp, colors.primary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                JalaliDate.from(date).day.fa(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isEnd) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isEnd -> colors.onPrimary
                                    inside -> colors.onPrimaryContainer
                                    else -> colors.onSurface
                                },
                            )
                        }
                    }
                }
            }
            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
