package com.example.stepsdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

private val Green = Color(0xFF6CC700)
private val Blue = Color(0xFF1FB8F0)
private val Purple = Color(0xFFA24BFF)
private val CardBg = Color(0xFF1C1C1E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App() } }
    }
}

private fun fmt(n: Int) = String.format(Locale.US, "%,d", n).replace(',', '\u00A0')

@Composable
fun App() {
    var steps by rememberSaveable { mutableIntStateOf(9433) }
    val hours = remember {
        mutableStateListOf<Int>().apply {
            val init = IntArray(24)
            init[0] = 30; init[7] = 100; init[8] = 450; init[9] = 80; init[10] = 100
            init[11] = 200; init[13] = 650; init[14] = 80; init[15] = 1900; init[16] = 350
            addAll(init.toList())
        }
    }
    var auto by remember { mutableStateOf(false) }
    var perSecond by remember { mutableFloatStateOf(5f) }
    var showDialog by remember { mutableStateOf(false) }

    fun addSteps(n: Int) {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        steps += n
        hours[h] = hours[h] + n
    }
    fun setSteps(v: Int) {
        if (v >= steps) addSteps(v - steps)
        else {
            val k = if (steps == 0) 0f else v.toFloat() / steps
            for (i in hours.indices) hours[i] = (hours[i] * k).toInt()
            steps = v
        }
    }

    LaunchedEffect(auto) {
        while (auto) {
            delay(1000)
            addSteps(perSecond.roundToInt())
        }
    }

    val minutes = (steps / 103.7f).toInt()
    val active = (steps * 0.0506f).toInt()
    val total = 1154 + active
    val km = steps * 0.8f / 1000f
    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0

    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()
            .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основная карточка
        Column(
            Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(32.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeartRings(steps / 6000f, minutes / 90f, active / 500f)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Шаги", fmt(steps), "", "/6 000", Green, Modifier.weight(1f)) { showDialog = true }
                StatBox("Время активности", minutes.toString(), "мин.", "/90", Blue, Modifier.weight(1f))
                StatBox("Потраченные калории", active.toString(), "ккал", "/500", Purple, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            InfoRow("Всего сожжено калорий", "${fmt(total)} ккал")
            Spacer(Modifier.height(8.dp))
            InfoRow("Расстояние, пройденное в ходе активностей", String.format(Locale.US, "%.2f", km).replace('.', ',') + " км")
        }

        // График шагов
        Column(Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(32.dp)).padding(20.dp)) {
            Text("Шаги", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Период максимальной активности: %02d:00–%02d:00".format(peak, (peak + 1) % 24),
                color = Color(0xFFB0B0B0), fontSize = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val maxV = (hours.maxOrNull() ?: 1).coerceAtLeast(1)
                val baseY = size.height - 24.dp.toPx()
                hours.forEachIndexed { i, v ->
                    if (v > 0) {
                        val x = size.width * i / 24f
                        val h = (baseY - 4.dp.toPx()) * v / maxV
                        drawRoundRect(
                            Green, Offset(x, baseY - h), Size(6.dp.toPx(), h.coerceAtLeast(6.dp.toPx())),
                            androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                        )
                    }
                }
                drawLine(Color(0xFF555555), Offset(0f, baseY + 2.dp.toPx()), Offset(size.width, baseY + 2.dp.toPx()), 2f)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0", "6", "12", "18", "(ч)").forEach { Text(it, color = Color(0xFFB0B0B0), fontSize = 12.sp) }
            }
        }

        // Управление
        Column(
            Modifier.fillMaxWidth().background(CardBg, RoundedCornerShape(32.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Управление", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 500, 1000).forEach { n ->
                    Button({ addSteps(n) }, Modifier.weight(1f)) { Text("+$n") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ showDialog = true }, Modifier.weight(1f)) { Text("Задать") }
                OutlinedButton({ setSteps(0) }, Modifier.weight(1f)) { Text("Сброс") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Авто-ходьба", Modifier.weight(1f))
                Switch(auto, { auto = it })
            }
            Text("Скорость: ${perSecond.roundToInt()} шаг/сек", color = Color(0xFFB0B0B0))
            Slider(perSecond, { perSecond = it }, valueRange = 1f..50f)
        }
    }

    if (showDialog) {
        var text by remember { mutableStateOf(steps.toString()) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Количество шагов") },
            text = {
                OutlinedTextField(
                    text, { text = it.filter(Char::isDigit).take(6) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton({ text.toIntOrNull()?.let { setSteps(it) }; showDialog = false }) { Text("OK") }
            },
            dismissButton = { TextButton({ showDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun StatBox(
    title: String, value: String, unit: String, goal: String, color: Color,
    modifier: Modifier, onClick: (() -> Unit)? = null
) {
    val m = if (onClick != null) modifier.clickableNoRipple(onClick) else modifier
    Column(
        m.height(200.dp).border(BorderStroke(1.dp, Color(0xFF555555)), RoundedCornerShape(20.dp)).padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = color, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(unit, color = Color(0xFFB0B0B0), fontSize = 13.sp, modifier = Modifier.height(20.dp))
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = Color(0xFF555555), modifier = Modifier.padding(horizontal = 8.dp))
        Text(goal, color = Color(0xFFB0B0B0), fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = Color(0xFFB0B0B0), fontSize = 16.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun HeartRings(stepsP: Float, timeP: Float, calP: Float) {
    Canvas(Modifier.size(260.dp, 230.dp)) {
        val u = size.width / 36f
        val cx = size.width / 2
        val cy = size.height * 0.46f
        val sw = 4.2f * u
        val rings = listOf(
            Triple(stepsP, Green, 1.0f),
            Triple(timeP, Blue, 0.80f),
            Triple(calP, Purple, 0.60f)
        )
        rings.forEach { (p, color, k) ->
            val path = heartPath(cx, cy, u * k)
            drawPath(path, color.copy(alpha = 0.22f), style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val pm = PathMeasure().apply { setPath(path, false) }
            val seg = Path()
            val len = pm.length * p.coerceIn(0f, 1f)
            if (len > 0f) {
                pm.getSegment(0f, len, seg, true)
                drawPath(seg, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private fun heartPath(cx: Float, cy: Float, s: Float): Path {
    val path = Path()
    val n = 240
    for (i in 0..n) {
        val t = (2 * PI * i / n).toFloat()
        val x = 16 * sin(t).pow(3)
        val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
        val px = cx + x * s
        val py = cy - y * s
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
