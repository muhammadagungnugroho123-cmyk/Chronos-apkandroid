package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class AlarmThemeSpec(
    val id: String,
    val nameId: String,
    val nameEn: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val descriptionId: String,
    val descriptionEn: String,
    val recommendedChime: String
)

object AlarmBackgroundThemes {
    val THEMES = listOf(
        AlarmThemeSpec(
            id = "COSMIC_DAWN",
            nameId = "Fajar Kosmik",
            nameEn = "Cosmic Dawn",
            emoji = "🌅",
            primaryColor = Color(0xFFFF9100),
            secondaryColor = Color(0xFFFF3D00),
            descriptionId = "Gradasi fajar hangat dengan berkas sinar matahari terbit, partikel keemasan, dan horizon fajar bersinar",
            descriptionEn = "Warm dawn gradient with radiant sunbeams, rising golden photoluminescent motes, and luminous earth horizon",
            recommendedChime = "Morning Birds"
        ),
        AlarmThemeSpec(
            id = "RAINY_HORIZON",
            nameId = "Hujan Ambien",
            nameEn = "Rainy Horizon",
            emoji = "🌧️",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF2979FF),
            descriptionId = "Garis tetesan hujan diagonal dinamis, riak air di horizon bawah, dan kabut sejuk malam hari",
            descriptionEn = "Dynamic diagonal raindrops, water ripple rings on the bottom horizon, and cool atmospheric blue mist",
            recommendedChime = "Ocean Waves"
        ),
        AlarmThemeSpec(
            id = "CIRCADIAN_STARS",
            nameId = "Bintang Sirkadian",
            nameEn = "Circadian Starlight",
            emoji = "🌌",
            primaryColor = Color(0xFFD500F9),
            secondaryColor = Color(0xFF651FFF),
            descriptionId = "Kubah bintang malam berkerlip alami, bulan sabit bercahaya aura sirkadian lembut, dan gelombang REM",
            descriptionEn = "Deep starry night with gentle twinkling stars, soft circadian lunar aura, and harmonic REM sleep wave",
            recommendedChime = "Zen Garden"
        ),
        AlarmThemeSpec(
            id = "FOREST_MIST",
            nameId = "Hutan Fajar",
            nameEn = "Forest Mist",
            emoji = "🌲",
            primaryColor = Color(0xFF00E676),
            secondaryColor = Color(0xFF00BFA5),
            descriptionId = "Siluet pegunungan pinus, kabut berembun fajar, dan bintik cahaya kunang-kunang melayang",
            descriptionEn = "Mountain pine canopy silhouette, floating morning mist layers, and drifting bioluminescent fireflies",
            recommendedChime = "Vitality Spirit"
        ),
        AlarmThemeSpec(
            id = "CYBER_AURORA",
            nameId = "Cyber Aurora",
            nameEn = "Cyber Aurora",
            emoji = "⚡",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFD500F9),
            descriptionId = "Tirai aurora borealis neon berdenyut dinamis, gelombang magnetik CHRONOS, dan grid futuristik",
            descriptionEn = "Undulating neon cyan & violet aurora curtains with magnetic pulse waves and perspective cyber grid",
            recommendedChime = "Cyberpunk"
        )
    )

    fun getTheme(id: String): AlarmThemeSpec {
        return THEMES.find { it.id.equals(id, ignoreCase = true) } ?: THEMES.first()
    }
}

private class StarData(val xPct: Float, val yPct: Float, val radius: Float, val speed: Float, val phaseOffset: Float)
private class RaindropData(val xPct: Float, val initialYPct: Float, val length: Float, val speed: Float, val alpha: Float)
private class FireflyData(val xPct: Float, val yPct: Float, val radius: Float, val floatSpeed: Float, val phase: Float)

/**
 * Realistic procedural Canvas background for Smart Alarms, featuring smooth animated motion,
 * dynamic atmospheric lighting, natural particle flows, and adaptive sunrise intensity ramp.
 */
@Composable
fun AlarmRealisticBackgroundCanvas(
    themeId: String,
    sunriseIntensity: Float = 0.5f, // 0.0 to 1.0 (brightens as audio fades in or during dawn)
    isEcoMode: Boolean = false,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val theme = remember(themeId) { AlarmBackgroundThemes.getTheme(themeId) }

    // In Eco Mode, use a static phase to consume 0% continuous GPU/CPU
    val phase: Float = if (isEcoMode) {
        0.85f
    } else {
        val transition = rememberInfiniteTransition(label = "alarm_theme_transition")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "theme_phase"
        )
        animatedPhase
    }

    // Stable procedural particles seeded per theme
    val stars = remember {
        val rng = Random(42)
        List(48) {
            StarData(
                xPct = rng.nextFloat(),
                yPct = rng.nextFloat() * 0.75f,
                radius = rng.nextFloat() * 2.2f + 0.8f,
                speed = rng.nextFloat() * 2.5f + 1.2f,
                phaseOffset = rng.nextFloat() * (2f * PI).toFloat()
            )
        }
    }

    val raindrops = remember {
        val rng = Random(1337)
        List(40) {
            RaindropData(
                xPct = rng.nextFloat(),
                initialYPct = rng.nextFloat(),
                length = rng.nextFloat() * 26f + 14f,
                speed = rng.nextFloat() * 1.8f + 1.2f,
                alpha = rng.nextFloat() * 0.45f + 0.25f
            )
        }
    }

    val fireflies = remember {
        val rng = Random(777)
        List(24) {
            FireflyData(
                xPct = rng.nextFloat(),
                yPct = rng.nextFloat() * 0.65f + 0.25f,
                radius = rng.nextFloat() * 2.8f + 1.2f,
                floatSpeed = rng.nextFloat() * 1.5f + 0.8f,
                phase = rng.nextFloat() * (2f * PI).toFloat()
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (theme.id) {
            "COSMIC_DAWN" -> {
                // Base Sky Gradient: Transitioning gracefully from twilight indigo to warm dawn horizon
                val dawnRamp = sunriseIntensity.coerceIn(0.15f, 1.0f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF030714),
                            Color(0xFF0E1A33),
                            Color(0xFF28183C),
                            Color(0xFF5A1E38).copy(alpha = 0.85f * dawnRamp),
                            Color(0xFFFF5722).copy(alpha = 0.60f * dawnRamp),
                            Color(0xFFFFB300).copy(alpha = 0.75f * dawnRamp)
                        )
                    )
                )

                // Sun Disc & Radiant Radial Corona at Horizon
                val sunCenter = Offset(w * 0.5f, h * 0.78f)
                val coronaRadius = w * (0.60f + 0.35f * dawnRamp)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE082).copy(alpha = (0.75f * dawnRamp).coerceAtMost(0.95f)),
                            Color(0xFFFF9800).copy(alpha = (0.45f * dawnRamp).coerceAtMost(0.70f)),
                            Color(0xFFFF3D00).copy(alpha = (0.22f * dawnRamp).coerceAtMost(0.40f)),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = coronaRadius
                    ),
                    radius = coronaRadius,
                    center = sunCenter
                )

                // Crepuscular Solar Light Shafts (Gentle rotating dawn rays)
                val rays = 12
                for (i in 0 until rays) {
                    val angle = (phase * 0.08f) + (i * (2f * PI.toFloat() / rays))
                    val rayEndX = sunCenter.x + cos(angle) * (w * 1.1f)
                    val rayEndY = sunCenter.y + sin(angle) * (h * 0.9f)
                    if (rayEndY < sunCenter.y + 40f) { // only radiate upwards
                        drawLine(
                            color = Color(0xFFFFD54F).copy(alpha = 0.08f * dawnRamp),
                            start = sunCenter,
                            end = Offset(rayEndX, rayEndY),
                            strokeWidth = 32f + (i % 3) * 16f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Rising golden photoluminescent motes
                for ((idx, mote) in fireflies.take(20).withIndex()) {
                    val floatY = (mote.yPct - ((phase * mote.floatSpeed * 0.05f) % 0.8f))
                    val wrappedY = if (floatY < 0.05f) floatY + 0.85f else floatY
                    val floatX = mote.xPct + sin(phase + mote.phase) * 0.04f
                    val motePos = Offset(w * floatX.coerceIn(0.05f, 0.95f), h * wrappedY)
                    val moteAlpha = (0.35f + 0.45f * sin(phase * 1.5f + idx)) * dawnRamp
                    drawCircle(
                        color = Color(0xFFFFE082).copy(alpha = moteAlpha.coerceIn(0.1f, 0.85f)),
                        radius = mote.radius * (1.2f + 0.3f * dawnRamp),
                        center = motePos
                    )
                }

                // Curved Earth Horizon Line at base
                val horizonPath = Path().apply {
                    moveTo(0f, h * 0.82f)
                    quadraticTo(w * 0.5f, h * 0.77f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = horizonPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF140D24),
                            Color(0xFF07040C)
                        )
                    )
                )
            }

            "RAINY_HORIZON" -> {
                // Deep atmospheric rainy slate gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF070D18),
                            Color(0xFF0F1B2D),
                            Color(0xFF162840),
                            Color(0xFF0B1626)
                        )
                    )
                )

                // Soft rolling atmospheric blue mist
                val mistCenter1 = Offset(w * (0.3f + 0.15f * sin(phase * 0.5f)), h * 0.45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.12f), Color.Transparent),
                        center = mistCenter1,
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = mistCenter1
                )

                // Dynamic diagonal rain streaks
                for (drop in raindrops) {
                    val progressY = (drop.initialYPct + (phase * drop.speed * 0.35f)) % 1.0f
                    val startX = w * drop.xPct
                    val startY = h * progressY
                    val endX = startX - drop.length * 0.45f
                    val endY = startY + drop.length
                    drawLine(
                        color = Color(0xFF67E8F9).copy(alpha = drop.alpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )
                }

                // Water ripples on the bottom horizon
                for (i in 0 until 4) {
                    val ripplePhase = (phase * 1.2f + i * 1.5f) % (2f * PI.toFloat())
                    val rippleRadius = (ripplePhase / (2f * PI.toFloat())) * (w * 0.32f)
                    val rippleAlpha = (1f - (ripplePhase / (2f * PI.toFloat()))).coerceIn(0f, 0.45f)
                    val rx = w * (0.25f + i * 0.22f)
                    val ry = h * (0.85f + (i % 2) * 0.05f)
                    drawOval(
                        color = Color(0xFF00E5FF).copy(alpha = rippleAlpha),
                        topLeft = Offset(rx - rippleRadius, ry - (rippleRadius * 0.3f)),
                        size = Size(rippleRadius * 2f, rippleRadius * 0.6f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            "CIRCADIAN_STARS" -> {
                // Deep velvet space dome
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020510),
                            Color(0xFF090E22),
                            Color(0xFF131438),
                            Color(0xFF050714)
                        )
                    )
                )

                // Twinkling Starfield
                for (star in stars) {
                    val twinkle = (sin(phase * star.speed + star.phaseOffset) + 1f) * 0.5f
                    val alpha = (0.25f + 0.65f * twinkle).coerceIn(0.15f, 0.95f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = star.radius * (0.75f + 0.35f * twinkle),
                        center = Offset(w * star.xPct, h * star.yPct)
                    )
                }

                // Glowing Celestial Crescent Moon with Circadian Breathing Aura
                val moonCenter = Offset(w * 0.76f, h * 0.22f)
                val auraRadius = w * 0.35f
                val auraPulse = 0.15f + 0.08f * sin(phase * 0.8f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD500F9).copy(alpha = auraPulse),
                            Color(0xFF651FFF).copy(alpha = auraPulse * 0.5f),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = auraRadius
                    ),
                    radius = auraRadius,
                    center = moonCenter
                )

                // Crescent Moon Body
                drawCircle(
                    color = Color(0xFFEDE7F6),
                    radius = 26f,
                    center = moonCenter
                )
                // Cutout to form elegant crescent
                drawCircle(
                    color = Color(0xFF090E22),
                    radius = 24f,
                    center = Offset(moonCenter.x - 9f, moonCenter.y - 7f)
                )

                // Smooth Sinusoidal Circadian Sleep-Wake Cycle Wave at base
                val wavePath = Path()
                val waveY = h * 0.82f
                wavePath.moveTo(0f, waveY)
                val points = 36
                val stepX = w / points.toFloat()
                for (p in 0..points) {
                    val curX = p * stepX
                    val angle = phase + (p * 0.25f)
                    val curY = waveY + sin(angle) * 28f + cos(angle * 0.5f) * 14f
                    wavePath.lineTo(curX, curY)
                }
                drawPath(
                    path = wavePath,
                    color = Color(0xFFD500F9).copy(alpha = 0.40f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            "FOREST_MIST" -> {
                // Emerald deep forest gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020E08),
                            Color(0xFF062217),
                            Color(0xFF0C3827),
                            Color(0xFF04140D)
                        )
                    )
                )

                // Soft Emerald Morning Mist Clouds
                val mistCenter = Offset(w * (0.5f + 0.15f * cos(phase * 0.6f)), h * 0.65f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha = 0.14f), Color.Transparent),
                        center = mistCenter,
                        radius = w * 0.70f
                    ),
                    radius = w * 0.70f,
                    center = mistCenter
                )

                // Mountain / Forest Silhouettes at bottom
                val ridgePath = Path().apply {
                    moveTo(0f, h * 0.85f)
                    val trees = 18
                    val tStep = w / trees.toFloat()
                    for (t in 0 until trees) {
                        val peakX = t * tStep + (tStep * 0.5f)
                        val peakY = h * (0.76f + ((t % 4) * 0.025f))
                        val nextX = (t + 1) * tStep
                        lineTo(peakX, peakY)
                        lineTo(nextX, h * 0.85f)
                    }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = ridgePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF092E1F),
                            Color(0xFF020C07)
                        )
                    )
                )

                // Drifting bioluminescent fireflies
                for ((idx, fly) in fireflies.withIndex()) {
                    val fx = (fly.xPct + sin(phase * fly.floatSpeed + fly.phase) * 0.08f).coerceIn(0.04f, 0.96f)
                    val fy = (fly.yPct + cos(phase * fly.floatSpeed * 0.8f + fly.phase) * 0.06f).coerceIn(0.15f, 0.88f)
                    val pulse = (sin(phase * 2.2f + idx) + 1f) * 0.5f
                    val alpha = (0.35f + 0.55f * pulse).coerceIn(0.1f, 0.9f)
                    drawCircle(
                        color = Color(0xFF69F0AE).copy(alpha = alpha),
                        radius = fly.radius * (1f + 0.4f * pulse),
                        center = Offset(w * fx, h * fy)
                    )
                }
            }

            "CYBER_AURORA" -> {
                // Deep obsidian synth background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020612),
                            Color(0xFF081226),
                            Color(0xFF101C38),
                            Color(0xFF050814)
                        )
                    )
                )

                // Undulating Neon Cyan & Violet Aurora Curtains
                val ribbonCount = 3
                for (r in 0 until ribbonCount) {
                    val ribbonPath = Path()
                    val rBaseY = h * (0.32f + r * 0.16f)
                    ribbonPath.moveTo(0f, rBaseY)
                    val steps = 28
                    val sStep = w / steps.toFloat()
                    for (s in 0..steps) {
                        val sx = s * sStep
                        val wave1 = sin(phase * 1.1f + s * 0.35f + r * 1.2f) * (h * 0.12f)
                        val wave2 = cos(phase * 0.7f + s * 0.20f) * (h * 0.06f)
                        ribbonPath.lineTo(sx, rBaseY + wave1 + wave2)
                    }
                    val color = if (r % 2 == 0) ChronosCyanNeon else ChronosPurpleNeon
                    drawPath(
                        path = ribbonPath,
                        color = color.copy(alpha = 0.35f),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Perspective Cyber Grid Lines on floor
                val gridHorizon = h * 0.80f
                drawLine(
                    color = ChronosCyanNeon.copy(alpha = 0.40f),
                    start = Offset(0f, gridHorizon),
                    end = Offset(w, gridHorizon),
                    strokeWidth = 2f
                )
                val lines = 10
                for (l in 0..lines) {
                    val startX = (w / lines.toFloat()) * l
                    val bottomX = w * 0.5f + (startX - w * 0.5f) * 2.2f
                    drawLine(
                        color = ChronosCyanNeon.copy(alpha = 0.16f),
                        start = Offset(startX, gridHorizon),
                        end = Offset(bottomX, h),
                        strokeWidth = 1.5f
                    )
                }
            }
        }
    }
}

/**
 * Compact horizontal selector for Alarm Visual Background Themes.
 */
@Composable
fun AlarmThemeSelectorPills(
    selectedThemeId: String,
    onSelectTheme: (String) -> Unit,
    isId: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AlarmBackgroundThemes.THEMES.forEach { theme ->
            val isSelected = theme.id.equals(selectedThemeId, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) theme.primaryColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) theme.primaryColor else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectTheme(theme.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.emoji,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isId) theme.nameId else theme.nameEn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFFD1D5DB)
                        )
                        Text(
                            text = if (isSelected) (if (isId) "Aktif" else "Selected") else theme.recommendedChime,
                            fontSize = 9.sp,
                            color = if (isSelected) theme.primaryColor else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

