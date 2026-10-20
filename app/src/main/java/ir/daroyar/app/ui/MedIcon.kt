package ir.daroyar.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.daroyar.app.data.Form
import ir.daroyar.app.data.Medication

/** Colours a medication can be given, as ARGB; mid tones that read on light and dark backgrounds. */
val MED_COLORS: List<Long> = listOf(
    0xFF14A394, // teal
    0xFF3B82F6, // blue
    0xFF8B5CF6, // violet
    0xFFEC4899, // pink
    0xFFEF4444, // red
    0xFFF59E0B, // amber
    0xFF65A30D, // green
    0xFF64748B, // slate
)

fun medColor(index: Int): Color = Color(MED_COLORS[Math.floorMod(index, MED_COLORS.size)])

/** The first colour none of [meds] uses, so each new medication looks different from the others. */
fun nextFreeColor(meds: List<Medication>): Int {
    val used = meds.map { Math.floorMod(it.color, MED_COLORS.size) }.toSet()
    return MED_COLORS.indices.firstOrNull { it !in used } ?: (meds.size % MED_COLORS.size)
}

/** A medication's picture: its [form] drawn in its [color], on a soft disc of the same colour. */
@Composable
fun MedIcon(form: Form, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.64f)) { drawForm(form, color) }
    }
}

@Composable
fun MedIcon(med: Medication, modifier: Modifier = Modifier, size: Dp = 44.dp) =
    MedIcon(med.form, medColor(med.color), modifier, size)

/** Draws [form] on a 24 × 24 grid scaled to the canvas. */
private fun DrawScope.drawForm(form: Form, color: Color) {
    val u = size.minDimension / 24f
    val light = lerp(color, Color.White, 0.62f)
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun s(w: Float, h: Float) = Size(w * u, h * u)
    fun r(v: Float) = CornerRadius(v * u, v * u)

    when (form) {
        Form.TABLET -> {
            drawCircle(color, radius = 9.5f * u, center = o(12f, 12f))
            rotate(-35f, pivot = o(12f, 12f)) {
                drawLine(light, o(6.5f, 12f), o(17.5f, 12f), strokeWidth = 1.6f * u, cap = StrokeCap.Round)
            }
            drawCircle(Color.White.copy(alpha = 0.35f), radius = 2f * u, center = o(8.5f, 7.5f))
        }
        Form.CAPSULE -> rotate(-45f, pivot = o(12f, 12f)) {
            val left = 1.5f
            val top = 7.5f
            clipRect(left = 0f, top = 0f, right = 12f * u, bottom = size.height) {
                drawRoundRect(color, o(left, top), s(21f, 9f), r(4.5f))
            }
            clipRect(left = 12f * u, top = 0f, right = size.width, bottom = size.height) {
                drawRoundRect(light, o(left, top), s(21f, 9f), r(4.5f))
            }
            drawRoundRect(Color.White.copy(alpha = 0.35f), o(4f, 9f), s(6f, 2f), r(1f))
        }
        Form.SYRUP -> {
            drawRoundRect(color, o(8.5f, 1f), s(7f, 3.5f), r(1f))
            drawRect(light, o(9.5f, 4.5f), s(5f, 2.5f))
            drawRoundRect(light, o(5f, 7f), s(14f, 16f), r(3.5f))
            clipRect(top = 13f * u) {
                drawRoundRect(color, o(5f, 7f), s(14f, 16f), r(3.5f))
            }
            drawRoundRect(Color.White.copy(alpha = 0.45f), o(7f, 15f), s(2f, 5f), r(1f))
        }
        Form.DROPS -> {
            val drop = Path().apply {
                moveTo(12f * u, 1.5f * u)
                cubicTo(12f * u, 1.5f * u, 20f * u, 10f * u, 20f * u, 15f * u)
                cubicTo(20f * u, 19.5f * u, 16.4f * u, 22.5f * u, 12f * u, 22.5f * u)
                cubicTo(7.6f * u, 22.5f * u, 4f * u, 19.5f * u, 4f * u, 15f * u)
                cubicTo(4f * u, 10f * u, 12f * u, 1.5f * u, 12f * u, 1.5f * u)
                close()
            }
            drawPath(drop, color)
            drawCircle(Color.White.copy(alpha = 0.4f), radius = 2f * u, center = o(8.5f, 15.5f))
        }
        Form.INJECTION -> rotate(45f, pivot = o(12f, 12f)) {
            // Plunger, flange, barrel with its dose, needle.
            drawLine(color, o(12f, 0.5f), o(12f, 5f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawLine(color, o(9f, 0.8f), o(15f, 0.8f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawLine(color, o(7.5f, 5f), o(16.5f, 5f), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawRoundRect(light, o(8.5f, 5.5f), s(7f, 12f), r(1.5f))
            clipRect(top = 11f * u) {
                drawRoundRect(color, o(8.5f, 5.5f), s(7f, 12f), r(1.5f))
            }
            drawRoundRect(color, o(8.5f, 5.5f), s(7f, 12f), r(1.5f), style = Stroke(1.2f * u))
            drawLine(color, o(12f, 17.5f), o(12f, 23.5f), strokeWidth = 1.2f * u, cap = StrokeCap.Round)
        }
        Form.SPRAY -> {
            // An inhaler: canister on top, body, mouthpiece to the side.
            drawRoundRect(light, o(10f, 1f), s(7f, 10f), r(2f))
            drawRoundRect(color, o(8f, 8f), s(11f, 12f), r(3f))
            drawRoundRect(color, o(3f, 15f), s(9f, 7f), r(2.5f))
            drawRoundRect(light, o(3.5f, 17f), s(2f, 3f), r(1f))
        }
        Form.CREAM -> {
            // A tube standing on its cap.
            drawRoundRect(color, o(5f, 1f), s(14f, 3f), r(1f))
            val body = Path().apply {
                moveTo(5.5f * u, 4f * u)
                lineTo(18.5f * u, 4f * u)
                lineTo(15.5f * u, 17f * u)
                lineTo(8.5f * u, 17f * u)
                close()
            }
            drawPath(body, color)
            drawRect(light, o(7.5f, 8f), s(9f, 3.5f))
            drawRect(light, o(10f, 17f), s(4f, 2f))
            drawRoundRect(color, o(8.5f, 19f), s(7f, 4f), r(1.2f))
        }
    }
}
