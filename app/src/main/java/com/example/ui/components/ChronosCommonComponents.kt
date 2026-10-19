package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CHRONOS_VERSION
import com.example.model.ChronosTranslations
import com.example.model.ScreenType
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

val ChronosCyanNeon = Color(0xFF00E5FF)
val ChronosBlueNeon = Color(0xFF2979FF)
val ChronosGreenNeon = Color(0xFF00E676)
val ChronosOrangeNeon = Color(0xFFFF9100)
val ChronosPurpleNeon = Color(0xFFD500F9)
val ChronosAmberNeon = Color(0xFFFFD54F)
val ChronosRoseNeon = Color(0xFFFF5252)
val ChronosGoldNeon = Color(0xFFFFD700)
val ChronosBgDeep = Color(0xFF0B131E)
val ChronosSurfaceDark = Color(0xFF131D26)
val ChronosCardNavy = Color(0xFF131A2A)

fun getThemeAccentColor(themeColorIndex: Int): Color = when (themeColorIndex) {
    1 -> ChronosBlueNeon
    2 -> ChronosGreenNeon
    3 -> ChronosOrangeNeon
    4 -> ChronosPurpleNeon
    5 -> ChronosRoseNeon
    6 -> ChronosGoldNeon
    else -> ChronosCyanNeon
}

@Composable
fun AnimatedGradientBackground(
    themeColorIndex: Int,
    ecoMode: Boolean = false,
    ambientGlowIntensity: Int = 2
) {
    val palette = remember(themeColorIndex) {
        when (themeColorIndex) {
            1 -> listOf(Color(0xFF2979FF), Color(0xFF00B0FF), Color(0xFF304FFE))
            2 -> listOf(Color(0xFF00E676), Color(0xFF00BFA5), Color(0xFF1DE9B6))
            3 -> listOf(Color(0xFFFF9100), Color(0xFFFF3D00), Color(0xFFFFC400))
            4 -> listOf(Color(0xFFD500F9), Color(0xFF651FFF), Color(0xFFF50057))
            5 -> listOf(Color(0xFFFF5252), Color(0xFFFF1744), Color(0xFFFF8A80))
            6 -> listOf(Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFE57F))
            else -> listOf(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFD500F9))
        }
    }

    val intensityFactor = when (ambientGlowIntensity) {
        0 -> 0f
        1 -> 0.55f
        3 -> 1.55f
        else -> 1.0f
    }

    val phase = if (ecoMode || intensityFactor <= 0f) {
        0.85f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "bg_orb_transition")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb_phase"
        )
        animatedPhase
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxDim = max(w, h)

        // Base deep celestial gradient (#080E17 -> #101B2E -> #070C14)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF080E17),
                    Color(0xFF101B2E),
                    Color(0xFF070C14)
                )
            )
        )

        if (intensityFactor > 0f) {
            // Floating Orb 1
            val x1 = w * (0.3f + 0.2f * sin(phase))
            val y1 = h * (0.2f + 0.15f * cos(phase * 0.8f))
            val r1 = maxDim * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette[0].copy(alpha = (0.15f * intensityFactor).coerceAtMost(0.32f)), Color.Transparent),
                    center = Offset(x1, y1),
                    radius = r1
                ),
                radius = r1,
                center = Offset(x1, y1)
            )

            // Floating Orb 2
            val x2 = w * (0.7f - 0.2f * cos(phase * 0.9f))
            val y2 = h * (0.7f + 0.15f * sin(phase * 0.7f))
            val r2 = maxDim * 0.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette[1].copy(alpha = (0.13f * intensityFactor).coerceAtMost(0.28f)), Color.Transparent),
                    center = Offset(x2, y2),
                    radius = r2
                ),
                radius = r2,
                center = Offset(x2, y2)
            )

            if (!ecoMode) {
                // Floating Orb 3
                val x3 = w * 0.5f
                val y3 = h * (0.5f + 0.2f * sin(phase * 1.1f))
                val r3 = maxDim * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette[2].copy(alpha = (0.11f * intensityFactor).coerceAtMost(0.24f)), Color.Transparent),
                        center = Offset(x3, y3),
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(x3, y3)
                )
            }

            if (ambientGlowIntensity == 3 && !ecoMode) {
                // Ultra Neon Sci-Fi Subtle Grid Overlay
                val step = 48.dp.toPx()
                var gx = 0f
                while (gx < w) {
                    drawLine(
                        color = palette[0].copy(alpha = 0.03f),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 1f
                    )
                    gx += step
                }
            }
        }
    }
}

@Composable
fun ClockFace(
    styleIndex: Int,
    nowMillis: Long,
    is24Hour: Boolean,
    accentColor: Color,
    showSeconds: Boolean = true,
    smoothSweep: Boolean = true,
    onCycleStyle: () -> Unit = {}
) {
    val cal = remember(nowMillis / 100L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val hours24 = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)
    val exactSeconds = if (smoothSweep) seconds + (millis / 1000f) else seconds.toFloat()

    val hoursDisplay = if (is24Hour) {
        "%02d".format(hours24)
    } else {
        val h12 = if (hours24 % 12 == 0) 12 else hours24 % 12
        "%02d".format(h12)
    }
    val minutesDisplay = "%02d".format(minutes)
    val secondsDisplay = "%02d".format(seconds)
    val amPm = if (hours24 >= 12) "PM" else "AM"

    if (styleIndex == 1) {
        // Style 1: Minimalist Digital Glass Card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(275.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                .clickable { onCycleStyle() }
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = hoursDisplay,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 54.sp,
                        color = Color.White
                    )
                    Text(
                        text = ":",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 54.sp,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = minutesDisplay,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 54.sp,
                        color = Color.White
                    )
                    if (showSeconds) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = secondsDisplay,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                if (!is24Hour) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = amPm,
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accentColor
                    )
                }
            }
        }
        return
    }

    if (styleIndex == 3) {
        // Style 3: Cyber Matrix HUD (Tactical Chronometer Telemetry)
        val dayProgress = ((hours24 * 3600 + minutes * 60 + seconds) / 86400f * 100f).toInt()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(305.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(ChronosSurfaceDark.copy(alpha = 0.88f))
                .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .clickable { onCycleStyle() }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CYBER CHRONO HUD",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "SOLAR DAY $dayProgress%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$hoursDisplay:$minutesDisplay",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = Color.White
                    )
                    if (showSeconds) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ":$secondsDisplay.${(millis / 100)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 7.dp)
                        )
                    }
                }

                // 3-Axis Telemetry Bars (Hours / Minutes / Seconds)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("HR", hours24 / 24f, accentColor),
                        Triple("MN", minutes / 60f, ChronosGreenNeon),
                        Triple("SC", exactSeconds / 60f, ChronosAmberNeon)
                    ).forEach { (lbl, ratio, col) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(lbl, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = Color(0xFF9CA3AF), modifier = Modifier.width(20.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(50))
                                        .background(col)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(290.dp)
            .clickable { onCycleStyle() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.42f

            if (styleIndex == 0 || styleIndex == 4) {
                // Style 0: Sci-Fi Orbital Ring & Style 4: Dual Solar Chrono
                val progress = (exactSeconds / 60f).coerceIn(0f, 1f)
                val sweepDeg = progress * 360f
                val angleRad = (progress * 2.0 * PI - PI / 2.0).toFloat()

                // Outer track
                drawCircle(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.45f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )

                if (styleIndex == 4) {
                    // Style 4 Inner 24h Solar Ring
                    val innerSolarR = radius - 20.dp.toPx()
                    val dayProg = ((hours24 * 60 + minutes) / 1440f).coerceIn(0f, 1f)
                    drawCircle(
                        color = ChronosAmberNeon.copy(alpha = 0.16f),
                        radius = innerSolarR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    drawArc(
                        color = ChronosAmberNeon,
                        startAngle = -90f,
                        sweepAngle = dayProg * 360f,
                        useCenter = false,
                        topLeft = Offset(cx - innerSolarR, cy - innerSolarR),
                        size = androidx.compose.ui.geometry.Size(innerSolarR * 2, innerSolarR * 2),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    // Decorative inner thin ring
                    drawCircle(
                        color = accentColor.copy(alpha = 0.22f),
                        radius = radius - 16.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                if (showSeconds) {
                    // Soft glow arc
                    drawArc(
                        color = accentColor.copy(alpha = 0.28f),
                        startAngle = -90f,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Indicator Head Dot
                    val hx = cx + radius * cos(angleRad)
                    val hy = cy + radius * sin(angleRad)
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 9.dp.toPx(),
                        center = Offset(hx, hy),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Micro ticks around ring every 5 seconds
                for (i in 0 until 60 step 5) {
                    val tickAngle = ((i / 60.0) * 2.0 * PI - PI / 2.0).toFloat()
                    val tx1 = cx + (radius - 22.dp.toPx()) * cos(tickAngle)
                    val ty1 = cy + (radius - 22.dp.toPx()) * sin(tickAngle)
                    val tx2 = cx + (radius - 14.dp.toPx()) * cos(tickAngle)
                    val ty2 = cy + (radius - 14.dp.toPx()) * sin(tickAngle)
                    drawLine(
                        color = accentColor.copy(alpha = 0.42f),
                        start = Offset(tx1, ty1),
                        end = Offset(tx2, ty2),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else {
                // Style 2: Precision Analog Sci-Fi Dial
                drawCircle(
                    color = Color(0xFF131D26).copy(alpha = 0.65f),
                    radius = radius,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )

                for (i in 0 until 60) {
                    val a = ((i / 60.0) * 2.0 * PI - PI / 2.0).toFloat()
                    val isMajor = i % 5 == 0
                    val innerR = radius - (if (isMajor) 18.dp.toPx() else 8.dp.toPx())
                    val outerR = radius - 4.dp.toPx()
                    drawLine(
                        color = if (isMajor) accentColor else Color.White.copy(alpha = 0.25f),
                        start = Offset(cx + innerR * cos(a), cy + innerR * sin(a)),
                        end = Offset(cx + outerR * cos(a), cy + outerR * sin(a)),
                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Hour hand
                val hourVal = (hours24 % 12) + minutes / 60f
                val hourAngle = ((hourVal / 12.0) * 2.0 * PI - PI / 2.0).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy),
                    end = Offset(cx + radius * 0.48f * cos(hourAngle), cy + radius * 0.48f * sin(hourAngle)),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Minute hand
                val minVal = minutes + exactSeconds / 60f
                val minAngle = ((minVal / 60.0) * 2.0 * PI - PI / 2.0).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, cy),
                    end = Offset(cx + radius * 0.70f * cos(minAngle), cy + radius * 0.70f * sin(minAngle)),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Second hand
                if (showSeconds) {
                    val secAngle = ((exactSeconds / 60.0) * 2.0 * PI - PI / 2.0).toFloat()
                    drawLine(
                        color = accentColor,
                        start = Offset(cx - 15.dp.toPx() * cos(secAngle), cy - 15.dp.toPx() * sin(secAngle)),
                        end = Offset(cx + radius * 0.85f * cos(secAngle), cy + radius * 0.85f * sin(secAngle)),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = accentColor,
                    radius = 6.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        if (styleIndex == 0 || styleIndex == 4) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (styleIndex == 4) {
                    Text(
                        text = "SOLAR • ${((hours24 * 60 + minutes) / 14.4f).toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosAmberNeon
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$hoursDisplay:$minutesDisplay",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 44.sp,
                        color = Color.White
                    )
                    if (showSeconds) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ":$secondsDisplay",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                if (!is24Hour) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF083344).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChronosTopBar(
    hasActiveAlarm: Boolean,
    onOpenFeatureHub: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProductivity: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Top-Left: Hamburger / Feature Discovery Hub Button
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosCyanNeon.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenFeatureHub() }
                    .testTag("open_feature_hub_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Feature Hub",
                        tint = ChronosCyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (hasActiveAlarm) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(ChronosAmberNeon)
                        .border(2.dp, ChronosBgDeep, CircleShape)
                )
            }
        }

        // 2. Center: Clean CHRONOS branding
        Text(
            text = "CHRONOS",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 22.sp,
            letterSpacing = 6.sp,
            color = ChronosCyanNeon
        )

        // 3. Top-Right: Quick Weather, Productivity & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onNavigateToWeather,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_weather_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Weather Bulletin",
                    tint = ChronosCyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onNavigateToProductivity,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_productivity_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = "Productivity",
                    tint = ChronosCyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToSettings() }
                    .testTag("top_settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFD1D5DB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class NavItemSpec(
    val screen: ScreenType,
    val labelKey: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false
)

@Composable
fun ChronosBottomNavigation(
    currentScreen: ScreenType,
    onSelectScreen: (ScreenType) -> Unit,
    hasActiveAlarm: Boolean,
    appLanguage: String
) {
    val navItems = remember(hasActiveAlarm) {
        listOf(
            NavItemSpec(ScreenType.CLOCK, "clock", Icons.Default.AccessTime),
            NavItemSpec(ScreenType.ALARMS, "alarms", Icons.Default.Alarm, hasActiveAlarm),
            NavItemSpec(ScreenType.TIMERS, "timers", Icons.Default.HourglassEmpty),
            NavItemSpec(ScreenType.STOPWATCH, "stopwatch", Icons.Default.Timer),
            NavItemSpec(ScreenType.WORLD_CLOCK, "world_clock", Icons.Default.Public),
            NavItemSpec(ScreenType.AI_ASSISTANT, "ai_assistant", Icons.Default.AutoAwesome)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isActive = currentScreen == item.screen
                    val label = ChronosTranslations.t(appLanguage, item.labelKey)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isActive) ChronosCyanNeon.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .clickable { onSelectScreen(item.screen) }
                            .padding(vertical = 7.dp, horizontal = 2.dp)
                            .testTag("bottom_nav_${item.screen.id}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label,
                                    tint = if (isActive) ChronosCyanNeon else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                                if (item.hasBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(ChronosCyanNeon)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Color(0xFF67E8F9) else Color(0xFF9CA3AF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HubItemSpec(
    val screen: ScreenType,
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null
)

private data class HubCategorySpec(
    val title: String,
    val items: List<HubItemSpec>
)

@Composable
fun FeatureHubDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSelectScreen: (ScreenType) -> Unit,
    onOpenLifePillar: (String) -> Unit,
    appLanguage: String,
    hasActiveAlarm: Boolean
) {
    val isId = appLanguage == "id"
    val categories = remember(isId, hasActiveAlarm) {
        listOf(
            HubCategorySpec(
                title = if (isId) "Waktu & Kronometri" else "Time & Chronometry",
                items = listOf(
                    HubItemSpec(
                        screen = ScreenType.CLOCK,
                        title = if (isId) "Jam Utama" else "Clock Face",
                        desc = if (isId) "Gaya dial orbital, digital glass, dan analog presisi" else "Orbital ring, digital glass & precision analog dials",
                        icon = Icons.Default.AccessTime,
                        color = ChronosCyanNeon
                    ),
                    HubItemSpec(
                        screen = ScreenType.ALARMS,
                        title = if (isId) "Alarm Cerdas" else "Smart Alarms",
                        desc = if (isId) "Analisis tidur AI, motivasi pagi & dering aurora" else "AI sleep hygiene, wake motivation & aurora waking",
                        icon = Icons.Default.Alarm,
                        color = ChronosAmberNeon,
                        badge = if (hasActiveAlarm) (if (isId) "Aktif" else "Active") else null
                    ),
                    HubItemSpec(
                        screen = ScreenType.TIMERS,
                        title = if (isId) "Pengatur Waktu" else "Timers",
                        desc = if (isId) "Hitung mundur sirkular, Pomodoro & kendali AI" else "Circular countdown, Pomodoro & AI smart controls",
                        icon = Icons.Default.HourglassEmpty,
                        color = ChronosGreenNeon
                    ),
                    HubItemSpec(
                        screen = ScreenType.STOPWATCH,
                        title = if (isId) "Stopwatch Cerdas Pro" else "Smart Stopwatch Pro",
                        desc = if (isId) "Kronograf analitik, Multi-Track 4x & pelatih HIIT" else "Pro chronograph, 4x Multi-Track & HIIT interval coach",
                        icon = Icons.Default.Timer,
                        color = ChronosRoseNeon
                    ),
                    HubItemSpec(
                        screen = ScreenType.WORLD_CLOCK,
                        title = if (isId) "Jam Dunia 3D" else "3D World Clock",
                        desc = if (isId) "Bola dunia 3D interaktif & 40+ kota global" else "Interactive 3D globe & 40+ world time zones",
                        icon = Icons.Default.Public,
                        color = ChronosBlueNeon
                    )
                )
            ),
            HubCategorySpec(
                title = if (isId) "Iklim, Maritim & Lingkungan" else "Climate, Maritime & Environment",
                items = listOf(
                    HubItemSpec(
                        screen = ScreenType.WEATHER,
                        title = if (isId) "Cuaca, Laut & Pasang Surut RI" else "Weather, Sea & Tides",
                        desc = if (isId) "Peringatan dini, angin, awan, ombak & pasut 16 pantai RI" else "Early warnings, wind, clouds, waves & 16 Indonesian coasts",
                        icon = Icons.Default.Cloud,
                        color = ChronosCyanNeon
                    )
                )
            ),
            HubCategorySpec(
                title = if (isId) "Produktivitas & Gaya Hidup" else "Productivity & Life Suite",
                items = listOf(
                    HubItemSpec(
                        screen = ScreenType.PRODUCTIVITY,
                        title = if (isId) "Dashboard Produktivitas" else "Productivity Dashboard",
                        desc = if (isId) "Agenda, to-do list, kebiasaan, catatan & gol harian" else "Agenda, tasks, habit streaks, notes & daily goals",
                        icon = Icons.Default.CheckBox,
                        color = ChronosCyanNeon
                    ),
                    HubItemSpec(
                        screen = ScreenType.CALCULATOR,
                        title = "Kalkulator Smart",
                        desc = if (isId) "Kalkulator pintar untuk matematika, keuangan, konversi, dan perhitungan sehari-hari."
                        else "Smart calculator for math, finance, unit conversions, and daily calculations.",
                        icon = Icons.Default.Calculate,
                        color = ChronosAmberNeon,
                        badge = "SMART"
                    )
                )
            ),
            HubCategorySpec(
                title = if (isId) "Kecerdasan Buatan & Sistem" else "Intelligence & System",
                items = listOf(
                    HubItemSpec(
                        screen = ScreenType.AI_ASSISTANT,
                        title = if (isId) "Asisten CHRONOS AI" else "CHRONOS AI Assistant",
                        desc = if (isId) "Percakapan cerdas & eksekusi perintah sistem" else "Contextual chat & direct system execution actions",
                        icon = Icons.Default.AutoAwesome,
                        color = ChronosPurpleNeon
                    ),
                    HubItemSpec(
                        screen = ScreenType.SETTINGS,
                        title = if (isId) "Pengaturan & Cloud" else "Settings & Cloud Backup",
                        desc = if (isId) "Tema warna, gaya jam, bahasa & pemulihan data" else "Themes, clock styles, language & JSON restore",
                        icon = Icons.Default.Settings,
                        color = Color(0xFFB0BEC5)
                    )
                )
            )
        )
    }

    val lifeModules = remember(isId) {
        listOf(
            Triple(if (isId) "Kesehatan & Ritme Sirkadian" else "Health & Circadian Rhythm", Icons.Default.Favorite, "HEALTH" to ChronosRoseNeon),
            Triple(if (isId) "Kebugaran Adaptif" else "Adaptive Fitness & UV Workout", Icons.Default.FitnessCenter, "ACTIVITY" to ChronosCyanNeon),
            Triple(if (isId) "Nutrisi & Hidrasi" else "Food & Weather Hydration", Icons.Default.Restaurant, "FOOD" to Color(0xFF69F0AE))
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable { onClose() }
            )

            // Drawer Panel
            Surface(
                color = Color(0xFF0E1624),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(330.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChronosSurfaceDark.copy(alpha = 0.6f))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ChronosCyanNeon.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = ChronosCyanNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "FEATURE HUB",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = ChronosCyanNeon
                                    ) {
                                        Text(
                                            text = "V$CHRONOS_VERSION",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isId) "Pusat Eksplorasi Fitur CHRONOS" else "CHRONOS Feature Discovery & Navigation",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    // Feature List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        categories.forEach { cat ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = cat.title.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        color = ChronosCyanNeon.copy(alpha = 0.85f)
                                    )

                                    cat.items.forEach { item ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.03f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    onSelectScreen(item.screen)
                                                    onClose()
                                                }
                                                .testTag("feature_hub_${item.screen.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = item.color.copy(alpha = 0.14f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, item.color.copy(alpha = 0.3f)),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = item.icon,
                                                                contentDescription = null,
                                                                tint = item.color,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = item.title,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            if (item.badge != null) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = ChronosAmberNeon
                                                                ) {
                                                                    Text(
                                                                        text = item.badge,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        color = Color.Black,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = item.desc,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF9CA3AF),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6B7280),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CHRONOS LIFE SUITE ECOSYSTEM Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ChronosAmberNeon.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CHRONOS LIFE SUITE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosAmberNeon
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ChronosAmberNeon.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "ECOSYSTEM",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ChronosAmberNeon,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (isId) "Modul hidup yang bisa dipakai sekarang: tidur, hidrasi, dan gerak."
                                        else "Live modules you can use now: sleep, hydration, and movement.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD1D5DB)
                                    )

                                    lifeModules.forEach { (name, icon, pair) ->
                                        val (pillarId, c) = pair
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.03f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    onOpenLifePillar(pillarId)
                                                    onClose()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = c,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = name,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFE5E7EB)
                                                    )
                                                }
                                                Text(
                                                    text = if (isId) "BUKA" else "OPEN",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ChronosGreenNeon
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChronosBgDeep)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CHRONOS V$CHRONOS_VERSION • Swipe Left/Right for Seamless Navigation",
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
    }
}
