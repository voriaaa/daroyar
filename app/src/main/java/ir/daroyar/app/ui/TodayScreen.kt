package ir.daroyar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.MedStore
import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.DoseStatus
import ir.daroyar.app.reminder.Schedule
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.formatPersianDate
import java.time.LocalDateTime
import kotlin.random.Random

private data class Dose(val med: Medication, val minute: Int, val status: DoseStatus)

/** Parts of the day the doses are grouped under, by the hour they start at. */
private enum class DayPart(val label: String, val icon: ImageVector, val startHour: Int) {
    LATE_NIGHT("نیمه‌شب", Icons.Outlined.NightsStay, 0),
    MORNING("صبح", Icons.Outlined.WbSunny, 4),
    NOON("ظهر", Icons.Outlined.LightMode, 12),
    EVENING("عصر", Icons.Outlined.WbTwilight, 16),
    NIGHT("شب", Icons.Outlined.NightsStay, 20);

    companion object {
        fun of(minuteOfDay: Int): DayPart = entries.last { minuteOfDay / 60 >= it.startHour }
    }
}

/** Gap between list items; the timeline line is drawn across it so it stays unbroken. */
private val ITEM_GAP = 10.dp

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
    val lateCount = doses.count { it.status == DoseStatus.LATE }
    val nextUpcoming = doses.firstOrNull { it.status == DoseStatus.UPCOMING }
    val lowStock = meds.filter { it.active && it.lowStock }
    val nowMinute = now.hour * 60 + now.minute

    val haptics = LocalHapticFeedback.current
    val toggle = { dose: Dose ->
        val markTaken = dose.status != DoseStatus.TAKEN
        if (markTaken) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onToggle(dose.med, dose.minute, markTaken)
    }

    // Confetti only when the last dose of the day is ticked off here, not on opening a finished day.
    val allDone = doses.isNotEmpty() && takenCount == doses.size
    var wasDone by remember { mutableStateOf<Boolean?>(null) }
    var celebration by remember { mutableStateOf(0) }
    LaunchedEffect(allDone) {
        if (wasDone == false && allDone) celebration++
        wasDone = allDone
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
        ) {
            item(key = "hero") { HeroCard(formatPersianDate(today), takenCount, lateCount, doses.size) }
            if (nextUpcoming != null) {
                item(key = "next") {
                    NextDoseCard(nextUpcoming, minutesLeft = nextUpcoming.minute - nowMinute, onTake = { toggle(nextUpcoming) })
                }
            }
            if (alertCount > 0) item(key = "alerts") { AlertsCard(alertCount, onShowAlerts) }
            if (lowStock.isNotEmpty()) item(key = "stock") { LowStockCard(lowStock) }
            if (doses.isEmpty()) {
                item(key = "empty") {
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
            doses.groupBy { DayPart.of(it.minute) }.forEach { (part, partDoses) ->
                item(key = "part-${part.name}") {
                    DayPartHeader(part, partDoses.count { it.status == DoseStatus.TAKEN }, partDoses.size, Modifier.animateItem())
                }
                items(partDoses, key = { "${it.med.id}-${it.minute}" }) { dose ->
                    TimelineRow(
                        dose = dose,
                        isNext = dose == nextUpcoming,
                        isFirst = dose == partDoses.first(),
                        isLast = dose == partDoses.last(),
                        onToggle = { toggle(dose) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        if (celebration > 0) Confetti(trigger = celebration)
    }
}

/** The brand's teal fading into deep blue; the same in light and dark theme, always with white text. */
private val HERO_GRADIENT = Brush.linearGradient(listOf(Color(0xFF14A394), Color(0xFF0E6E86)))

@Composable
private fun HeroCard(date: String, taken: Int, late: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val onHero = Color.White
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else taken.toFloat() / total,
        animationSpec = tween(700),
        label = "progress",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(HERO_GRADIENT)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, style = MaterialTheme.typography.titleSmall, color = onHero.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        total == 0 -> "روز خوبی داشته باشید 🌿"
                        taken == total -> "آفرین! همهٔ نوبت‌های امروز مصرف شد 🎉"
                        taken == 0 -> "${total.fa()} نوبت برای امروز"
                        else -> "${(total - taken).fa()} نوبت دیگر مانده"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                )
                if (late > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${late.fa()} نوبت دیر شده",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onTertiaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.tertiaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (total > 0) {
                Spacer(Modifier.width(16.dp))
                ProgressRing(progress, taken, total, onHero)
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, taken: Int, total: Int, color: Color) {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${taken.fa()}/${total.fa()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text("نوبت", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.85f))
        }
    }
}

/** "۴۰ دقیقهٔ دیگر", "۲ ساعت و ۱۵ دقیقهٔ دیگر". */
private fun countdown(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        minutes <= 0 -> "همین حالا"
        h == 0 -> "${m.fa()} دقیقهٔ دیگر"
        m == 0 -> "${h.fa()} ساعت دیگر"
        else -> "${h.fa()} ساعت و ${m.fa()} دقیقهٔ دیگر"
    }
}

@Composable
private fun NextDoseCard(dose: Dose, minutesLeft: Int, onTake: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer, contentColor = colors.onSecondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MedIcon(dose.med, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("نوبت بعدی", style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                    Text(
                        dose.med.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (dose.med.dose.isNotBlank()) {
                        Text(dose.med.dose.fa(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatMinute(dose.minute), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(countdown(minutesLeft), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onTake, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("الان مصرف کردم")
            }
        }
    }
}

@Composable
private fun DayPartHeader(part: DayPart, taken: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(part.icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(part.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            if (taken == total) "✓ کامل" else "${taken.fa()} از ${total.fa()}",
            style = MaterialTheme.typography.labelLarge,
            color = if (taken == total) colors.primary else colors.onSurfaceVariant,
        )
    }
}

/** One dose: a dot on the day's timeline, and its card. */
@Composable
private fun TimelineRow(
    dose: Dose,
    isNext: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dotColor = when (dose.status) {
        DoseStatus.TAKEN -> colors.primary
        DoseStatus.LATE -> colors.tertiary
        DoseStatus.UPCOMING -> if (isNext) colors.secondary else colors.outline
    }
    val gap = with(LocalDensity.current) { ITEM_GAP.toPx() }
    Row(modifier.height(IntrinsicSize.Min)) {
        Canvas(
            Modifier
                .width(28.dp)
                .fillMaxHeight()
        ) {
            val x = size.width / 2
            val dotY = 34.dp.toPx()
            val line = colors.outlineVariant
            val w = 2.dp.toPx()
            // The line reaches into the gaps between items, so the rail looks continuous.
            if (!isFirst) drawLine(line, Offset(x, -gap), Offset(x, dotY), strokeWidth = w)
            if (!isLast) drawLine(line, Offset(x, dotY), Offset(x, size.height + gap), strokeWidth = w)
            val r = (if (isNext) 8 else 6).dp.toPx()
            if (dose.status == DoseStatus.UPCOMING) {
                drawCircle(colors.surface, r, Offset(x, dotY))
                drawCircle(dotColor, r, Offset(x, dotY), style = Stroke(2.dp.toPx()))
            } else {
                drawCircle(dotColor, r, Offset(x, dotY))
            }
        }
        Spacer(Modifier.width(6.dp))
        DoseCard(dose, isNext, onToggle, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseCard(dose: Dose, isNext: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val taken = dose.status == DoseStatus.TAKEN
    val container by animateColorAsState(
        when (dose.status) {
            DoseStatus.TAKEN -> colors.surfaceContainer
            DoseStatus.LATE -> colors.tertiaryContainer
            DoseStatus.UPCOMING -> if (isNext) colors.secondaryContainer else colors.surfaceContainerHigh
        },
        label = "container",
    )
    val (badge, badgeColor) = when (dose.status) {
        DoseStatus.TAKEN -> "مصرف شد" to colors.primary
        DoseStatus.LATE -> "دیر شده" to colors.tertiary
        DoseStatus.UPCOMING -> (if (isNext) "نوبت بعدی" else "") to colors.secondary
    }
    val textColor = if (taken) colors.onSurfaceVariant else colors.onSurface
    Card(onClick = onToggle, modifier = modifier, colors = CardDefaults.cardColors(containerColor = container)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedIcon(dose.med, Modifier.scale(if (taken) 0.9f else 1f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatMinute(dose.minute),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (taken) colors.onSurfaceVariant else colors.primary,
                    )
                    if (badge.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(badge, style = MaterialTheme.typography.labelMedium, color = badgeColor, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    dose.med.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val kindLabel = if (dose.med.isSupplement) "مکمل" else ""
                val detail = listOf(kindLabel, dose.med.dose, dose.med.notes).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(
                        detail.fa(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CheckButton(taken, onToggle)
        }
    }
}

/** Round tick that pops in with a little bounce when the dose is marked taken. */
@Composable
private fun CheckButton(checked: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "check",
    )
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .border(2.dp, if (checked) colors.primary else colors.outline, CircleShape),
        )
        Box(
            Modifier
                .size(34.dp)
                .scale(fill)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = if (checked) "لغو مصرف" else "علامت مصرف",
                tint = colors.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val color: Color, val w: Float, val h: Float)

/** A burst of confetti over the screen; a new [trigger] value starts a new burst. Doesn't take touches. */
@Composable
private fun Confetti(trigger: Int) {
    val t = remember(trigger) { Animatable(0f) }
    val pieces = remember(trigger) {
        val random = Random(trigger)
        List(90) {
            Piece(
                x = random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 0.6f,
                vy = -(0.6f + random.nextFloat() * 0.7f),
                spin = (random.nextFloat() - 0.5f) * 900f,
                color = medColor(random.nextInt(MED_COLORS.size)),
                w = 6f + random.nextFloat() * 6f,
                h = 10f + random.nextFloat() * 8f,
            )
        }
    }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val time = t.value
        val alpha = if (time > 0.75f) (1f - time) / 0.25f else 1f
        pieces.forEach { p ->
            // Thrown up from the bottom edge, then falling back under gravity.
            val x = (p.x + p.vx * time) * size.width
            val y = size.height * (1f + p.vy * time * 2.2f + 2.4f * time * time)
            val w = p.w * density / 2
            val h = p.h * density / 2
            rotate(p.spin * time, pivot = Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h))
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

@Composable
fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Medication, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAction) { Text(action) }
    }
}
