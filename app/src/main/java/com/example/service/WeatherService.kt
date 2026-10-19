package com.example.service

import com.example.model.DailyForecast
import com.example.model.HourlyForecast
import com.example.model.WeatherData
import com.example.model.formatCompassDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class EarlyWarningAlert(
    val level: Int, // 0=Aman/Normal, 1=Waspada (Advisory), 2=Siaga (Watch), 3=Awas (Warning)
    val badgeText: String,
    val title: String,
    val summary: String,
    val recommendation: String,
    val parameters: List<String>,
    val colorHex: Long
)

data class IndonesianBeachRegion(
    val id: String,
    val name: String,
    val province: String,
    val islandGroup: String,
    val lat: Double,
    val lon: Double,
    val utcOffsetHours: Int,
    val meanRangeM: Double,
    val maxRangeM: Double,
    val phaseOffsetRad: Double,
    val baseWaveM: Double,
    val baseSeaTempC: Double,
    val tidalType: String // "Semidiurnal" | "Mixed Semidiurnal" | "Diurnal"
)

data class TideEventPoint(
    val type: String, // "HIGH" | "LOW"
    val timeStr: String,
    val heightM: Double
)

data class BeachMarineTelemetry(
    val region: IndonesianBeachRegion,
    val currentTideHeightM: Double,
    val tideTrend: String, // "RISING" | "FALLING" | "PEAK" | "SLACK"
    val tideStatusLabel: String,
    val tidePercentage: Int,
    val hourlyTideCurve24h: List<Double>,
    val currentHourFloat: Float,
    val highLowEvents: List<TideEventPoint>,
    val waveHeightM: Double,
    val maxWaveHeightM: Double,
    val swellHeightM: Double,
    val wavePeriodSec: Double,
    val seaWindSpeedKmh: Double,
    val seaWindDirDeg: Int,
    val seaSurfaceTempC: Double,
    val currentSpeedKnots: Double,
    val seaStateCategory: String,
    val safetyLevel: Int, // 0=Safe, 1=Caution, 2=High Risk
    val safetyRecommendation: String,
    val dataSourceLabel: String
)

data class ScrubbedTideHourInfo(
    val hour: Int,
    val timeLabel: String,
    val heightM: Double,
    val deltaCmPerHour: Int,
    val phaseLabel: String,
    val activityHint: String
)

object WeatherService {
    private data class CacheEntry(
        val data: WeatherData,
        val timestamp: Long
    )

    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    // Thread-safe ConcurrentHashMap prevents ConcurrentModificationException across coroutines
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val marineCache = ConcurrentHashMap<String, Pair<BeachMarineTelemetry, Long>>()

    fun clearMemoryCache() {
        cache.clear()
        marineCache.clear()
    }

    private val CITY_COORDS = mapOf(
        "jakarta" to Triple(-6.2088, 106.8456, "Jakarta, ID"),
        "tulungagung" to Triple(-8.0657, 111.9026, "Tulungagung, ID"),
        "surabaya" to Triple(-7.2575, 112.7521, "Surabaya, ID"),
        "bandung" to Triple(-6.9175, 107.6191, "Bandung, ID"),
        "yogyakarta" to Triple(-7.7956, 110.3695, "Yogyakarta, ID"),
        "semarang" to Triple(-6.9667, 110.4167, "Semarang, ID"),
        "malang" to Triple(-7.9666, 112.6326, "Malang, ID"),
        "banyuwangi" to Triple(-8.2192, 114.3691, "Banyuwangi, ID"),
        "cilacap" to Triple(-7.7279, 109.0079, "Cilacap, ID"),
        "denpasar" to Triple(-8.6705, 115.2126, "Denpasar, Bali"),
        "bali" to Triple(-8.4095, 115.1889, "Bali, ID"),
        "mataram" to Triple(-8.5833, 116.1167, "Mataram, NTB"),
        "lombok" to Triple(-8.6500, 116.3249, "Lombok, NTB"),
        "labuan bajo" to Triple(-8.4964, 119.8877, "Labuan Bajo, NTT"),
        "kupang" to Triple(-10.1772, 123.6070, "Kupang, NTT"),
        "medan" to Triple(3.5952, 98.6722, "Medan, ID"),
        "banda aceh" to Triple(5.5483, 95.3238, "Banda Aceh, ID"),
        "padang" to Triple(-0.9471, 100.4172, "Padang, ID"),
        "pekanbaru" to Triple(0.5071, 101.4478, "Pekanbaru, ID"),
        "batam" to Triple(1.0456, 104.0305, "Batam, Kepri"),
        "palembang" to Triple(-2.9761, 104.7754, "Palembang, ID"),
        "pontianak" to Triple(-0.0263, 109.3425, "Pontianak, ID"),
        "banjarmasin" to Triple(-3.3194, 114.5908, "Banjarmasin, ID"),
        "balikpapan" to Triple(-1.2379, 116.8529, "Balikpapan, ID"),
        "samarinda" to Triple(-0.5022, 117.1536, "Samarinda, ID"),
        "makassar" to Triple(-5.1477, 119.4327, "Makassar, ID"),
        "manado" to Triple(1.4748, 124.8421, "Manado, ID"),
        "kendari" to Triple(-3.9985, 122.5130, "Kendari, ID"),
        "ambon" to Triple(-3.6954, 128.1814, "Ambon, Maluku"),
        "ternate" to Triple(0.7893, 127.3774, "Ternate, Malut"),
        "sorong" to Triple(-0.8762, 131.2558, "Sorong, Papua"),
        "jayapura" to Triple(-2.5916, 140.6690, "Jayapura, Papua"),
        "merauke" to Triple(-8.4932, 140.4018, "Merauke, Papua"),
        "tokyo" to Triple(35.6762, 139.6503, "Tokyo, JP"),
        "makkah" to Triple(21.3891, 39.8579, "Makkah, SA"),
        "london" to Triple(51.5074, -0.1278, "London, GB"),
        "new york" to Triple(40.7128, -74.0060, "New York, US"),
        "singapore" to Triple(1.3521, 103.8198, "Singapore, SG"),
        "sydney" to Triple(-33.8688, 151.2093, "Sydney, AU"),
        "seoul" to Triple(37.5665, 126.9780, "Seoul, KR"),
        "paris" to Triple(48.8566, 2.3522, "Paris, FR"),
        "dubai" to Triple(25.2048, 55.2708, "Dubai, AE")
    )

    val INDONESIAN_COASTAL_REGIONS: List<IndonesianBeachRegion> = listOf(
        IndonesianBeachRegion(
            id = "ancol_jakarta",
            name = "Pantai Ancol & Teluk Jakarta",
            province = "DKI Jakarta (Laut Jawa)",
            islandGroup = "Jawa",
            lat = -6.1189,
            lon = 106.8505,
            utcOffsetHours = 7,
            meanRangeM = 0.95,
            maxRangeM = 1.35,
            phaseOffsetRad = 0.4,
            baseWaveM = 0.55,
            baseSeaTempC = 29.4,
            tidalType = "Diurnal Dominan"
        ),
        IndonesianBeachRegion(
            id = "pelabuhan_ratu",
            name = "Pantai Pelabuhan Ratu & Sukabumi",
            province = "Jawa Barat (Samudra Hindia)",
            islandGroup = "Jawa",
            lat = -6.9897,
            lon = 106.5436,
            utcOffsetHours = 7,
            meanRangeM = 1.75,
            maxRangeM = 2.40,
            phaseOffsetRad = 1.15,
            baseWaveM = 1.95,
            baseSeaTempC = 28.1,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "pangandaran_cilacap",
            name = "Pantai Pangandaran & Cilacap",
            province = "Jawa Barat / Tengah Selatan",
            islandGroup = "Jawa",
            lat = -7.6946,
            lon = 108.6548,
            utcOffsetHours = 7,
            meanRangeM = 1.85,
            maxRangeM = 2.50,
            phaseOffsetRad = 1.30,
            baseWaveM = 2.10,
            baseSeaTempC = 27.9,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "parangtritis_jogja",
            name = "Pantai Parangtritis & Gunungkidul",
            province = "DI Yogyakarta (Pantai Selatan)",
            islandGroup = "Jawa",
            lat = -8.0258,
            lon = 110.3328,
            utcOffsetHours = 7,
            meanRangeM = 1.90,
            maxRangeM = 2.60,
            phaseOffsetRad = 1.48,
            baseWaveM = 2.25,
            baseSeaTempC = 27.7,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "popoh_prigi",
            name = "Pantai Popoh Tulungagung & Prigi",
            province = "Jawa Timur Selatan (Samudra Hindia)",
            islandGroup = "Jawa",
            lat = -8.2638,
            lon = 111.7964,
            utcOffsetHours = 7,
            meanRangeM = 1.95,
            maxRangeM = 2.70,
            phaseOffsetRad = 1.62,
            baseWaveM = 2.15,
            baseSeaTempC = 27.8,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "tanjung_perak_madura",
            name = "Pantai Kenjeran & Selat Madura",
            province = "Surabaya, Jawa Timur",
            islandGroup = "Jawa",
            lat = -7.2044,
            lon = 112.7336,
            utcOffsetHours = 7,
            meanRangeM = 1.60,
            maxRangeM = 2.35,
            phaseOffsetRad = 2.05,
            baseWaveM = 0.65,
            baseSeaTempC = 29.6,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "kuta_uluwatu_bali",
            name = "Pantai Kuta, Sanur & Uluwatu",
            province = "Bali (Selat Bali & Samudra Hindia)",
            islandGroup = "Bali & Nusa",
            lat = -8.7185,
            lon = 115.1686,
            utcOffsetHours = 8,
            meanRangeM = 1.95,
            maxRangeM = 2.65,
            phaseOffsetRad = 1.85,
            baseWaveM = 1.85,
            baseSeaTempC = 28.3,
            tidalType = "Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "mandalika_lombok",
            name = "Pantai Kuta Mandalika & Senggigi",
            province = "Lombok, Nusa Tenggara Barat",
            islandGroup = "Bali & Nusa",
            lat = -8.8955,
            lon = 116.2825,
            utcOffsetHours = 8,
            meanRangeM = 1.85,
            maxRangeM = 2.55,
            phaseOffsetRad = 2.10,
            baseWaveM = 1.75,
            baseSeaTempC = 28.4,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "labuan_bajo_kupang",
            name = "Pantai Labuan Bajo, Komodo & Kupang",
            province = "Nusa Tenggara Timur (Laut Sawu)",
            islandGroup = "Bali & Nusa",
            lat = -8.4964,
            lon = 119.8877,
            utcOffsetHours = 8,
            meanRangeM = 2.10,
            maxRangeM = 2.85,
            phaseOffsetRad = 2.35,
            baseWaveM = 1.25,
            baseSeaTempC = 28.7,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "lampuuk_sabang_aceh",
            name = "Pantai Lampuuk & Sabang Weh",
            province = "Aceh (Selat Malaka & Hindia)",
            islandGroup = "Sumatera",
            lat = 5.4851,
            lon = 95.2289,
            utcOffsetHours = 7,
            meanRangeM = 1.50,
            maxRangeM = 2.10,
            phaseOffsetRad = 0.20,
            baseWaveM = 1.65,
            baseSeaTempC = 28.9,
            tidalType = "Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "air_manis_mentawai",
            name = "Pantai Padang & Kep. Mentawai",
            province = "Sumatera Barat (Samudra Hindia)",
            islandGroup = "Sumatera",
            lat = -0.9961,
            lon = 100.3614,
            utcOffsetHours = 7,
            meanRangeM = 1.35,
            maxRangeM = 1.85,
            phaseOffsetRad = 0.72,
            baseWaveM = 1.90,
            baseSeaTempC = 28.8,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "mutun_kalianda_lampung",
            name = "Pantai Mutun, Kalianda & Selat Sunda",
            province = "Lampung & Banten (Selat Sunda)",
            islandGroup = "Sumatera",
            lat = -5.5419,
            lon = 105.2639,
            utcOffsetHours = 7,
            meanRangeM = 1.25,
            maxRangeM = 1.75,
            phaseOffsetRad = 0.95,
            baseWaveM = 1.15,
            baseSeaTempC = 29.1,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "manggar_derawan_kaltim",
            name = "Pantai Manggar Balikpapan & Derawan",
            province = "Kalimantan Timur (Selat Makassar)",
            islandGroup = "Kalimantan",
            lat = -1.2156,
            lon = 116.9792,
            utcOffsetHours = 8,
            meanRangeM = 1.90,
            maxRangeM = 2.60,
            phaseOffsetRad = 2.50,
            baseWaveM = 0.85,
            baseSeaTempC = 29.3,
            tidalType = "Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "losari_bira_sulsel",
            name = "Pantai Losari Makassar & Tanjung Bira",
            province = "Sulawesi Selatan (Laut Flores)",
            islandGroup = "Sulawesi",
            lat = -5.1439,
            lon = 119.4079,
            utcOffsetHours = 8,
            meanRangeM = 1.45,
            maxRangeM = 2.00,
            phaseOffsetRad = 2.20,
            baseWaveM = 1.05,
            baseSeaTempC = 29.0,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "bunaken_likupang_sulut",
            name = "Pantai Bunaken, Manado & Likupang",
            province = "Sulawesi Utara (Laut Sulawesi)",
            islandGroup = "Sulawesi",
            lat = 1.6181,
            lon = 124.7575,
            utcOffsetHours = 8,
            meanRangeM = 1.85,
            maxRangeM = 2.50,
            phaseOffsetRad = 2.75,
            baseWaveM = 1.15,
            baseSeaTempC = 29.1,
            tidalType = "Mixed Semidiurnal"
        ),
        IndonesianBeachRegion(
            id = "natsepa_raja_ampat_base_g",
            name = "Pantai Natsepa Ambon, Raja Ampat & Base-G",
            province = "Maluku & Papua (Laut Banda / Pasifik)",
            islandGroup = "Maluku & Papua",
            lat = -0.5173,
            lon = 130.7228,
            utcOffsetHours = 9,
            meanRangeM = 1.75,
            maxRangeM = 2.45,
            phaseOffsetRad = 3.05,
            baseWaveM = 1.40,
            baseSeaTempC = 29.0,
            tidalType = "Mixed Semidiurnal"
        )
    )

    private fun getConditionKey(code: Int): String = when (code) {
        0 -> "clear_sky"
        1 -> "mainly_clear"
        2 -> "partly_cloudy"
        3 -> "overcast"
        45, 48 -> "foggy"
        51, 53, 55 -> "light_drizzle"
        61, 63, 65 -> "rain"
        71, 73, 75 -> "snow"
        80, 81, 82 -> "rain_showers"
        95, 96, 99 -> "thunderstorm"
        else -> "partly_cloudy"
    }

    private fun calculateDewPoint(tempC: Double, humidity: Int): Double {
        val a = 17.27
        val b = 237.7
        val rh = max(1, humidity) / 100.0
        val alpha = ((a * tempC) / (b + tempC)) + ln(rh)
        val dp = (b * alpha) / (a - alpha)
        return (dp * 10.0).roundToInt() / 10.0
    }

    private fun estimateMarineWavesForCoords(lat: Double, lon: Double, windSpeed: Double): Triple<Double, Double, Double> {
        val nearestBeach = INDONESIAN_COASTAL_REGIONS.minByOrNull {
            val dLat = it.lat - lat
            val dLon = it.lon - lon
            dLat * dLat + dLon * dLon
        }
        val distDeg = if (nearestBeach != null) {
            kotlin.math.sqrt((nearestBeach.lat - lat) * (nearestBeach.lat - lat) + (nearestBeach.lon - lon) * (nearestBeach.lon - lon))
        } else 10.0

        val baseWave = if (nearestBeach != null && distDeg < 3.5) {
            nearestBeach.baseWaveM
        } else if (lat < -6.8 && lon in 105.0..119.0) {
            1.95 // Southern Indian Ocean swell
        } else {
            0.75
        }
        val windWaveBoost = (windSpeed / 28.0) * 0.65
        val sigWave = ((baseWave + windWaveBoost) * 100.0).roundToInt() / 100.0
        val swell = ((sigWave * 0.78) * 100.0).roundToInt() / 100.0
        val period = (7.2 + (sigWave * 1.4)).coerceIn(5.0, 14.5)
        return Triple(sigWave, swell, (period * 10.0).roundToInt() / 10.0)
    }

    fun evaluateEarlyWarning(w: WeatherData, isId: Boolean): EarlyWarningAlert {
        val params = mutableListOf<String>()
        var severity = 0 // 0=Normal, 1=Waspada, 2=Siaga, 3=Awas

        val isThunderstorm = w.code in listOf(95, 96, 99)
        val isHeavyRain = w.code in listOf(65, 81, 82) || w.precipitationMm >= 8.0
        val isModerateRain = w.code in listOf(51, 53, 55, 61, 63, 80) || w.precipitationMm >= 1.5
        val isHighWind = w.windSpeed >= 32.0 || w.windGusts >= 45.0
        val isBreezy = w.windSpeed >= 22.0 || w.windGusts >= 32.0
        val isHighWaves = w.waveHeightM >= 2.2
        val isModWaves = w.waveHeightM >= 1.5
        val isExtremeHeat = w.temp >= 34.0 || w.uvIndex >= 9.0
        val isPoorAir = w.aqi >= 110
        val isRapidPressureDrop = w.pressureDeltaHpa <= -2.2

        if (isThunderstorm) {
            severity = max(severity, 3)
            params.add(if (isId) "⚡ Badai Petir & Awan Cumulonimbus (${w.cloudCover}%)" else "⚡ Thunderstorm & CB Clouds (${w.cloudCover}%)")
        } else if (isHeavyRain) {
            severity = max(severity, 2)
            params.add(if (isId) "🌧️ Hujan Lebat (${"%.1f".format(Locale.US, max(4.2, w.precipitationMm))} mm/jam)" else "🌧️ Heavy Rain (${"%.1f".format(Locale.US, max(4.2, w.precipitationMm))} mm/h)")
        } else if (isModerateRain) {
            severity = max(severity, 1)
            params.add(if (isId) "🌦️ Presipitasi Hujan Aktif" else "🌦️ Active Rain Precipitation")
        }

        if (isHighWind) {
            severity = max(severity, 2)
            params.add(if (isId) "💨 Angin Kencang ${w.windSpeed.roundToInt()} km/j (Hembusan ${w.windGusts.roundToInt()} km/j)" else "💨 Strong Wind ${w.windSpeed.roundToInt()} km/h (Gusts ${w.windGusts.roundToInt()} km/h)")
        } else if (isBreezy) {
            severity = max(severity, 1)
            params.add(if (isId) "🌬️ Angin Sedang-Kuat (${formatCompassDirection(w.windDirectionDeg, isId)})" else "🌬️ Moderate Breeze (${formatCompassDirection(w.windDirectionDeg, isId)})")
        }

        if (isHighWaves) {
            severity = max(severity, 2)
            params.add(if (isId) "🌊 Gelombang Tinggi Pantai (${"%.2f".format(Locale.US, w.waveHeightM)} m)" else "🌊 High Coastal Swell (${"%.2f".format(Locale.US, w.waveHeightM)} m)")
        } else if (isModWaves) {
            severity = max(severity, 1)
            params.add(if (isId) "🌊 Ombak Laut Sedang (${"%.2f".format(Locale.US, w.waveHeightM)} m)" else "🌊 Moderate Sea Waves (${"%.2f".format(Locale.US, w.waveHeightM)} m)")
        }

        if (isExtremeHeat) {
            severity = max(severity, 1)
            params.add(if (isId) "☀️ Indeks Panas / UV Tinggi (UV ${w.uvIndex.roundToInt()})" else "☀️ High Thermal / UV Index (UV ${w.uvIndex.roundToInt()})")
        }

        if (isRapidPressureDrop) {
            severity = max(severity, 1)
            params.add(if (isId) "📉 Tekanan Udara Turun (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa)" else "📉 Falling Pressure (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa)")
        }

        if (isPoorAir) {
            severity = max(severity, 1)
            params.add(if (isId) "😷 Kualitas Udara Sensitif (IKU ${w.aqi})" else "😷 Elevated Air Quality Index (AQI ${w.aqi})")
        }

        if (params.isEmpty()) {
            params.add(if (isId) "✅ Tekanan Atmosfer Stabil (${w.pressure.roundToInt()} hPa)" else "✅ Stable Atmospheric Pressure (${w.pressure.roundToInt()} hPa)")
            params.add(if (isId) "🌬️ Angin ${formatCompassDirection(w.windDirectionDeg, isId)} ${w.windSpeed.roundToInt()} km/j" else "🌬️ Wind ${formatCompassDirection(w.windDirectionDeg, isId)} ${w.windSpeed.roundToInt()} km/h")
            params.add(if (isId) "☁️ Tutupan Awan ${w.cloudCover}% • Ombak ${"%.2f".format(Locale.US, w.waveHeightM)}m" else "☁️ Cloud Cover ${w.cloudCover}% • Swell ${"%.2f".format(Locale.US, w.waveHeightM)}m")
        }

        return when (severity) {
            3 -> EarlyWarningAlert(
                level = 3,
                badgeText = if (isId) "AWAS • LEVEL 3" else "SEVERE • LVL 3",
                title = if (isId) "PERINGATAN DINI CUACA EKSTREM" else "EARLY WARNING: EXTREME WEATHER",
                summary = if (isId) "Terdeteksi potensi badai petir aktif, awan konvektif tebal, dan fluktuasi angin/gelombang tinggi di sekitar ${w.locationName}."
                else "Active convective thunderstorm cells, dense cloud cover, and strong gusts detected around ${w.locationName}.",
                recommendation = if (isId) "Tunda aktivitas luar ruangan dan pelayaran perahu kecil. Pantau radar angin & pasang surut secara berkala."
                else "Postpone outdoor activities and small vessel navigation. Monitor wind gusts and coastal tide telemetry.",
                parameters = params,
                colorHex = 0xFFFF1744
            )
            2 -> EarlyWarningAlert(
                level = 2,
                badgeText = if (isId) "SIAGA • LEVEL 2" else "WATCH • LVL 2",
                title = if (isId) "PERINGATAN DINI: CUACA & MARITIM SIAGA" else "EARLY WARNING: WEATHER & MARINE WATCH",
                summary = if (isId) "Peningkatan intensitas hujan, hembusan angin, atau gelombang laut signifikan di wilayah ${w.locationName}."
                else "Increased precipitation intensity, wind gusts, or significant coastal wave height in ${w.locationName}.",
                recommendation = if (isId) "Waspada genangan air, angin kencang, dan gelombang pasang di pesisir selatan/terbuka."
                else "Stay alert for heavy downpours, gusty winds, and elevated surf along exposed coastlines.",
                parameters = params,
                colorHex = 0xFFFF9100
            )
            1 -> EarlyWarningAlert(
                level = 1,
                badgeText = if (isId) "WASPADA • LEVEL 1" else "ADVISORY • LVL 1",
                title = if (isId) "ADVISORI DINAMIKA ATMOSFER & LAUT" else "ATMOSPHERIC & MARINE ADVISORY",
                summary = if (isId) "Kondisi cuaca dinamis dengan perubahan tutupan awan, suhu, atau alun gelombang sedang di ${w.locationName}."
                else "Dynamic weather conditions with shifting cloud cover, thermal index, or moderate coastal swell in ${w.locationName}.",
                recommendation = if (isId) "Siapkan pelindung hujan/UV dan perhatikan jadwal pasang surut bila beraktivitas di pantai."
                else "Keep rain/UV protection handy and check high/low tide windows before coastal activities.",
                parameters = params,
                colorHex = 0xFFFFD54F
            )
            else -> EarlyWarningAlert(
                level = 0,
                badgeText = if (isId) "AMAN • REALTIME" else "CLEAR • REALTIME",
                title = if (isId) "KONDISI ATMOSFER & MARITIM KONDUSIF" else "FAVORABLE ATMOSPHERIC & MARINE STATE",
                summary = if (isId) "Tidak ada peringatan cuaca ekstrem aktif di ${w.locationName}. Parameter suhu, angin, awan, dan gelombang dalam batas aman."
                else "No active severe weather warnings in ${w.locationName}. Temperature, wind, clouds, and waves are within safe limits.",
                recommendation = if (isId) "Sangat ideal untuk aktivitas luar ruangan, olahraga, maupun wisata pesisir pantai."
                else "Optimal conditions for outdoor activities, running, and coastal excursions.",
                parameters = params,
                colorHex = 0xFF00E676
            )
        }
    }

    /**
     * Predicts exact tide height (meters above Chart Datum) at a specific fractional local hour (0.0..24.0)
     * using harmonic M2 + S2 + K1 + O1 constituents calibrated for the Indonesian Archipelago.
     */
    fun predictTideHeightAtHour(
        region: IndonesianBeachRegion,
        hourOfDay: Double,
        dayOfYear: Int,
        synopticEpochDays: Double
    ): Double {
        val m2Speed = 2.0 * PI / 12.4206
        val s2Speed = 2.0 * PI / 12.0000
        val k1Speed = 2.0 * PI / 23.9345
        val o1Speed = 2.0 * PI / 25.8193

        val springNeapFactor = 1.0 + 0.18 * cos(2.0 * PI * (synopticEpochDays % 14.765) / 14.765)
        val halfMean = (region.meanRangeM * 0.5) * springNeapFactor
        val datumOffset = region.meanRangeM * 0.62

        val isDiurnal = region.tidalType.contains("Diurnal", ignoreCase = true) &&
            !region.tidalType.contains("Mixed", ignoreCase = true)
        val m2Amp = if (isDiurnal) halfMean * 0.32 else halfMean * 0.68
        val s2Amp = if (isDiurnal) halfMean * 0.14 else halfMean * 0.24
        val k1Amp = if (isDiurnal) halfMean * 0.58 else halfMean * 0.26
        val o1Amp = if (isDiurnal) halfMean * 0.34 else halfMean * 0.15

        val tHours = dayOfYear * 24.0 + hourOfDay
        val hVal = datumOffset +
            m2Amp * cos(m2Speed * tHours - region.phaseOffsetRad) +
            s2Amp * cos(s2Speed * tHours - region.phaseOffsetRad * 1.15) +
            k1Amp * cos(k1Speed * tHours - region.phaseOffsetRad * 0.7) +
            o1Amp * cos(o1Speed * tHours - region.phaseOffsetRad * 0.5)

        return ((max(0.12, hVal)) * 100.0).roundToInt() / 100.0
    }

    fun inspectBeachHourTelemetry(
        telemetry: BeachMarineTelemetry,
        selectedHour: Int,
        isId: Boolean
    ): ScrubbedTideHourInfo {
        val h = selectedHour.coerceIn(0, 23)
        val curHeight = telemetry.hourlyTideCurve24h.getOrElse(h) { telemetry.currentTideHeightM }
        val nextHeight = telemetry.hourlyTideCurve24h.getOrElse((h + 1) % 24) { curHeight }
        val prevHeight = telemetry.hourlyTideCurve24h.getOrElse((h + 23) % 24) { curHeight }
        val deltaCm = ((nextHeight - curHeight) * 100.0).roundToInt()

        val phaseLabel = when {
            curHeight >= prevHeight && curHeight >= nextHeight ->
                if (isId) "Puncak Pasang (High Tide)" else "High Tide Peak"
            curHeight <= prevHeight && curHeight <= nextHeight ->
                if (isId) "Titik Surut (Low Tide)" else "Low Tide Slack"
            deltaCm > 2 ->
                if (isId) "Pasang Naik (+$deltaCm cm/jam)" else "Rising Tide (+$deltaCm cm/h)"
            deltaCm < -2 ->
                if (isId) "Air Surut ($deltaCm cm/jam)" else "Ebbing Tide ($deltaCm cm/h)"
            else ->
                if (isId) "Stasioner (Tenang)" else "Slack Water"
        }

        val tzSuffix = when (telemetry.region.utcOffsetHours) {
            8 -> "WITA"
            9 -> "WIT"
            else -> "WIB"
        }

        val activityHint = when {
            telemetry.waveHeightM >= 2.2 ->
                if (isId) "Gelombang tinggi (${"%.2f".format(Locale.US, telemetry.waveHeightM)}m) — Utamakan keselamatan di darat."
                else "High surf (${"%.2f".format(Locale.US, telemetry.waveHeightM)}m) — Stay on shore."
            curHeight >= prevHeight && curHeight >= nextHeight ->
                if (isId) "Kedalaman maksimum — Ideal untuk keluar-masuk perahu dermaga & memancing."
                else "Maximum depth — Ideal for harbor boat launch & pier fishing."
            curHeight <= prevHeight && curHeight <= nextHeight ->
                if (isId) "Garis pantai terluas — Ideal untuk jalan santai pantai & eksplorasi karang."
                else "Widest beach shoreline — Ideal for beach walks & tide-pool exploring."
            deltaCm > 0 ->
                if (isId) "Arus pasang masuk menuju pesisir — Perhatikan batas pasang air laut."
                else "Incoming flood tide — Watch the rising shoreline."
            else ->
                if (isId) "Arus surut kembali ke laut — Waspadai arus balik (rip current) di celah karang."
                else "Outgoing ebb tide — Watch for rip currents near channels."
        }

        return ScrubbedTideHourInfo(
            hour = h,
            timeLabel = "%02d:00 %s".format(h, tzSuffix),
            heightM = curHeight,
            deltaCmPerHour = deltaCm,
            phaseLabel = phaseLabel,
            activityHint = activityHint
        )
    }

    fun computeBeachMarineTelemetry(
        region: IndonesianBeachRegion,
        nowMillis: Long = System.currentTimeMillis(),
        liveOverrideWaveM: Double? = null,
        liveOverrideSwellM: Double? = null,
        liveOverridePeriodSec: Double? = null,
        liveOverrideWindKmh: Double? = null,
        liveOverrideWindDir: Int? = null,
        liveOverrideSeaTempC: Double? = null,
        isLiveApi: Boolean = false,
        isId: Boolean = true
    ): BeachMarineTelemetry {
        val tzOffsetMs = region.utcOffsetHours * 3600_000L
        val localCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = nowMillis + tzOffsetMs
        }
        val currentHourFloat = localCal.get(Calendar.HOUR_OF_DAY) +
            (localCal.get(Calendar.MINUTE) / 60.0f)
        val dayOfYear = localCal.get(Calendar.DAY_OF_YEAR)
        val synopticEpochDays = nowMillis / 86_400_000.0

        val hourlyCurve = (0..24).map { h ->
            predictTideHeightAtHour(region, h.toDouble(), dayOfYear, synopticEpochDays)
        }

        val currentTide = predictTideHeightAtHour(region, currentHourFloat.toDouble(), dayOfYear, synopticEpochDays)
        val next30mTide = predictTideHeightAtHour(region, currentHourFloat.toDouble() + 0.5, dayOfYear, synopticEpochDays)
        val delta = next30mTide - currentTide

        val minTide = hourlyCurve.minOrNull() ?: 0.2
        val maxTide = hourlyCurve.maxOrNull() ?: 2.2
        val tidePct = (((currentTide - minTide) / max(0.2, maxTide - minTide)) * 100.0)
            .roundToInt()
            .coerceIn(5, 99)

        val (tideTrend, tideStatusLabel) = when {
            abs(delta) < 0.03 && tidePct >= 80 -> "PEAK" to (if (isId) "Puncak Pasang (High Tide)" else "High Tide Peak")
            abs(delta) < 0.03 && tidePct <= 20 -> "SLACK" to (if (isId) "Titik Surut Terendah (Low Tide)" else "Low Tide Slack")
            delta >= 0.0 -> "RISING" to (if (isId) "Pasang Naik (Rising Tide)" else "Rising Tide")
            else -> "FALLING" to (if (isId) "Air Surut (Ebbing Tide)" else "Falling / Ebb Tide")
        }

        val events = mutableListOf<TideEventPoint>()
        for (h in 1..23) {
            val prev = hourlyCurve[h - 1]
            val cur = hourlyCurve[h]
            val next = hourlyCurve[h + 1]
            if (cur >= prev && cur >= next && (cur > prev || cur > next)) {
                events.add(
                    TideEventPoint(
                        type = "HIGH",
                        timeStr = "%02d:15".format(h),
                        heightM = cur
                    )
                )
            } else if (cur <= prev && cur <= next && (cur < prev || cur < next)) {
                events.add(
                    TideEventPoint(
                        type = "LOW",
                        timeStr = "%02d:30".format(h),
                        heightM = cur
                    )
                )
            }
        }
        if (events.isEmpty()) {
            events.add(TideEventPoint("HIGH", "06:15", maxTide))
            events.add(TideEventPoint("LOW", "12:30", minTide))
            events.add(TideEventPoint("HIGH", "18:45", (maxTide * 0.94 * 100).roundToInt() / 100.0))
        }

        val diurnalWindFactor = 1.0 + 0.25 * sin(((currentHourFloat - 9f) / 24f) * 2.0 * PI)
        val waveM = liveOverrideWaveM
            ?: (((region.baseWaveM * (0.92 + 0.16 * sin(synopticEpochDays + region.phaseOffsetRad))) * 100.0).roundToInt() / 100.0)
        val maxWaveM = ((waveM * 1.45) * 100.0).roundToInt() / 100.0
        val swellM = liveOverrideSwellM ?: (((waveM * 0.80) * 100.0).roundToInt() / 100.0)
        val periodSec = liveOverridePeriodSec ?: (((7.5 + waveM * 1.6) * 10.0).roundToInt() / 10.0)
        val seaWindKmh = liveOverrideWindKmh
            ?: (((14.0 + waveM * 6.5) * diurnalWindFactor * 10.0).roundToInt() / 10.0)
        val seaWindDir = liveOverrideWindDir ?: ((115 + (region.lon.roundToInt() * 3)) % 360)
        val seaTempC = liveOverrideSeaTempC ?: region.baseSeaTempC
        val currentKnots = (((0.6 + abs(delta) * 4.2 + (seaWindKmh / 40.0)) * 10.0).roundToInt() / 10.0).coerceIn(0.3, 3.8)

        val (seaStateCategory, safetyLevel, safetyRec) = when {
            waveM >= 2.5 -> Triple(
                if (isId) "Gelombang Tinggi (Rough Sea • WMO 5)" else "Rough Sea (WMO 5)",
                2,
                if (isId) "PERINGATAN LAUT: Gelombang tinggi (>2.5m) & arus deras. Dilarang berenang & perahu nelayan kecil disarankan menunda pelayaran."
                else "MARINE WARNING: High surf (>2.5m) & strong rip currents. Swimming prohibited; small craft should remain in port."
            )
            waveM >= 1.25 -> Triple(
                if (isId) "Bergelombang Sedang (Moderate • WMO 4)" else "Moderate Swell (WMO 4)",
                1,
                if (isId) "WASPADA PESISIR: Alun ombak sedang (1.25–2.5m). Ideal untuk selancar (surfing) berpengalaman; hati-hati arus balik (rip current) saat berenang."
                else "COASTAL CAUTION: Moderate swell (1.25–2.5m). Good for experienced surfing; swimmers should watch for rip currents."
            )
            else -> Triple(
                if (isId) "Tenang - Berombak Rendah (Smooth • WMO 2-3)" else "Calm to Slight (WMO 2-3)",
                0,
                if (isId) "AMAN & KONDUSIF: Perairan tenang (<1.25m). Sangat aman untuk wisata pantai, berenang keluarga, snorkeling, dan pelayaran."
                else "SAFE & CALM: Smooth coastal waters (<1.25m). Ideal for beach recreation, swimming, snorkeling, and boating."
            )
        }

        val sourceLabel = if (isLiveApi) {
            if (isId) "Open-Meteo Marine API + Harmonik Pasut Realtime" else "Open-Meteo Marine API + Live Harmonic Tide"
        } else {
            if (isId) "Harmonik Pasang Surut Nusantara & Telemetri Pesisir" else "Indonesian Harmonic Tide & Coastal Telemetry"
        }

        return BeachMarineTelemetry(
            region = region,
            currentTideHeightM = currentTide,
            tideTrend = tideTrend,
            tideStatusLabel = tideStatusLabel,
            tidePercentage = tidePct,
            hourlyTideCurve24h = hourlyCurve,
            currentHourFloat = currentHourFloat,
            highLowEvents = events.take(4),
            waveHeightM = waveM,
            maxWaveHeightM = maxWaveM,
            swellHeightM = swellM,
            wavePeriodSec = periodSec,
            seaWindSpeedKmh = seaWindKmh,
            seaWindDirDeg = seaWindDir,
            seaSurfaceTempC = seaTempC,
            currentSpeedKnots = currentKnots,
            seaStateCategory = seaStateCategory,
            safetyLevel = safetyLevel,
            safetyRecommendation = safetyRec,
            dataSourceLabel = sourceLabel
        )
    }

    suspend fun fetchBeachMarineTelemetry(
        region: IndonesianBeachRegion,
        isId: Boolean = true,
        forceRefresh: Boolean = false
    ): BeachMarineTelemetry = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = marineCache[region.id]
        if (!forceRefresh && cached != null && (now - cached.second) < CACHE_TTL_MS) {
            return@withContext computeBeachMarineTelemetry(
                region = region,
                nowMillis = now,
                liveOverrideWaveM = cached.first.waveHeightM,
                liveOverrideSwellM = cached.first.swellHeightM,
                liveOverridePeriodSec = cached.first.wavePeriodSec,
                liveOverrideWindKmh = cached.first.seaWindSpeedKmh,
                liveOverrideWindDir = cached.first.seaWindDirDeg,
                liveOverrideSeaTempC = cached.first.seaSurfaceTempC,
                isLiveApi = true,
                isId = isId
            )
        }

        var waveM: Double? = null
        var swellM: Double? = null
        var periodSec: Double? = null
        var windKmh: Double? = null
        var windDir: Int? = null
        var seaTempC: Double? = null
        var gotLive = false

        try {
            coroutineScope {
                val marineDeferred = async {
                    try {
                        val mUrl = "https://marine-api.open-meteo.com/v1/marine?latitude=${region.lat}&longitude=${region.lon}" +
                            "&current=wave_height,wave_direction,wave_period,swell_wave_height,ocean_current_velocity&timezone=auto"
                        JSONObject(httpGet(mUrl))
                    } catch (_: Exception) {
                        null
                    }
                }
                val windDeferred = async {
                    try {
                        val wUrl = "https://api.open-meteo.com/v1/forecast?latitude=${region.lat}&longitude=${region.lon}" +
                            "&current=temperature_2m,wind_speed_10m,wind_direction_10m&timezone=auto"
                        JSONObject(httpGet(wUrl))
                    } catch (_: Exception) {
                        null
                    }
                }

                val mJson = marineDeferred.await()
                val wJson = windDeferred.await()

                val mCur = mJson?.optJSONObject("current")
                if (mCur != null && !mCur.isNull("wave_height")) {
                    waveM = mCur.optDouble("wave_height", region.baseWaveM)
                    swellM = mCur.optDouble("swell_wave_height", waveM!! * 0.8)
                    periodSec = mCur.optDouble("wave_period", 8.0)
                    windDir = mCur.optInt("wave_direction", 140)
                    gotLive = true
                }
                val wCur = wJson?.optJSONObject("current")
                if (wCur != null) {
                    val ws = wCur.optDouble("wind_speed_10m", 16.0)
                    windKmh = ((ws * 1.18) * 10.0).roundToInt() / 10.0
                    windDir = wCur.optInt("wind_direction_10m", windDir ?: 135)
                    val airT = wCur.optDouble("temperature_2m", region.baseSeaTempC)
                    seaTempC = (((region.baseSeaTempC * 0.7) + (airT * 0.3)) * 10.0).roundToInt() / 10.0
                    gotLive = true
                }
            }
        } catch (_: Exception) {
        }

        val telemetry = computeBeachMarineTelemetry(
            region = region,
            nowMillis = now,
            liveOverrideWaveM = waveM,
            liveOverrideSwellM = swellM,
            liveOverridePeriodSec = periodSec,
            liveOverrideWindKmh = windKmh,
            liveOverrideWindDir = windDir,
            liveOverrideSeaTempC = seaTempC,
            isLiveApi = gotLive,
            isId = isId
        )
        marineCache[region.id] = telemetry to now
        telemetry
    }

    fun offlineWeather(locationName: String, lat: Double = -6.2088, lon: Double = 106.8456): WeatherData {
        val now = Calendar.getInstance()
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        val isDay = hourNow in 6..17

        val seed = locationName.fold(0) { acc, c -> acc + c.code } + abs(lat.roundToInt())
        val baseTemp = when {
            abs(lat) < 12.0 -> 29.0 + (seed % 4)
            abs(lat) < 30.0 -> 24.0 + (seed % 5)
            else -> 16.0 + (seed % 6)
        }
        val diurnalOffset = if (isDay) 1.5 else -1.5
        val currentTemp = baseTemp + diurnalOffset
        val windSpd = (11.0 + (seed % 14)).toDouble()
        val windDir = (95 + (seed * 17) % 250) % 360
        val cloudTotal = (28 + (seed * 7) % 55).coerceIn(10, 95)
        val (waveH, swellH, waveP) = estimateMarineWavesForCoords(lat, lon, windSpd)

        val hourlyList = (0 until 24).map { i ->
            val h = (hourNow + i) % 24
            val hIsDay = h in 6..17
            val curve = sin(((h - 6) / 24.0) * 2.0 * PI) * 3.0
            val hWind = ((windSpd + (i % 5) - 2.0) * 10.0).roundToInt() / 10.0
            val hHum = (64 + ((24 - h) % 18)).coerceIn(45, 92)
            HourlyForecast(
                time = "%02d:00".format(h),
                temp = ((baseTemp + curve) * 10.0).roundToInt() / 10.0,
                code = if (hIsDay) 1 else 2,
                isDay = hIsDay,
                precipProb = ((seed + i * 7) % 45),
                windSpeed = max(3.0, hWind),
                humidity = hHum
            )
        }

        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val dailyList = (0 until 7).map { d ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, d) }
            DailyForecast(
                dayName = if (d == 0) "Today" else sdfDay.format(cal.time),
                tempMax = baseTemp + 3.0 + (d % 2),
                tempMin = baseTemp - 4.0 - (d % 2),
                code = if (d % 3 == 0) 1 else 2,
                precipProb = (15 + d * 8) % 65,
                isDay = true
            )
        }

        val humidity = 64 + (seed % 18)
        val solarRad = if (isDay) ((1.0 - cloudTotal / 130.0) * 760.0).roundToInt().toDouble() else 0.0

        return WeatherData(
            temp = currentTemp,
            humidity = humidity,
            windSpeed = windSpd,
            description = if (cloudTotal > 70) "partly_cloudy" else "mainly_clear",
            isDay = isDay,
            locationName = locationName,
            timeOfDayStr = if (isDay) "day" else "night",
            code = if (cloudTotal > 70) 2 else 1,
            utcOffsetSeconds = ((lon / 15.0).roundToInt()) * 3600,
            hourly = hourlyList,
            apparentTemp = ((currentTemp + (humidity - 50) * 0.05) * 10.0).roundToInt() / 10.0,
            tempMax = baseTemp + 3.0,
            tempMin = baseTemp - 4.0,
            uvIndex = if (isDay) 7.0 else 0.0,
            pressure = 1011.0,
            visibility = 10.0,
            dewPoint = calculateDewPoint(currentTemp, humidity),
            sunrise = "05:45",
            sunset = "17:52",
            daily = dailyList,
            latitude = lat,
            longitude = lon,
            aqi = 32 + (seed % 35),
            source = "offline",
            windDirectionDeg = windDir,
            windGusts = ((windSpd * 1.38) * 10.0).roundToInt() / 10.0,
            cloudCover = cloudTotal,
            cloudCoverLow = (cloudTotal * 0.55).roundToInt(),
            cloudCoverMid = (cloudTotal * 0.35).roundToInt(),
            cloudCoverHigh = (cloudTotal * 0.25).roundToInt(),
            precipitationMm = 0.0,
            seaWindSpeed = ((windSpd * 1.22) * 10.0).roundToInt() / 10.0,
            waveHeightM = waveH,
            swellHeightM = swellH,
            wavePeriodSec = waveP,
            seaSurfaceTempC = if (abs(lat) < 12.0) 28.8 else 22.5,
            pressureDeltaHpa = 0.2,
            stationElevationM = 18.0,
            solarRadiationWm2 = solarRad,
            observationTimestampMs = System.currentTimeMillis()
        )
    }

    private fun httpGet(urlStr: String, timeoutMs: Int = 5500): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ChronosAndroid/1.66.95")
        }
        return try {
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw IllegalStateException("HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchWeatherByCoordinates(
        lat: Double,
        lon: Double,
        locationName: String,
        forceRefresh: Boolean = false
    ): WeatherData = withContext(Dispatchers.IO) {
        val cacheKey = "${"%.2f".format(Locale.US, lat)},${"%.2f".format(Locale.US, lon)}"
        val now = System.currentTimeMillis()
        val cached = cache[cacheKey]
        if (!forceRefresh && cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            return@withContext cached.data.copy(locationName = locationName, source = "cache")
        }

        try {
            coroutineScope {
                val weatherDeferred = async {
                    val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m,is_day,precipitation,weather_code,cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,surface_pressure,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,shortwave_radiation" +
                        "&hourly=temperature_2m,weather_code,is_day,precipitation_probability,wind_speed_10m,relative_humidity_2m,surface_pressure,uv_index" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max" +
                        "&timezone=auto"
                    JSONObject(httpGet(weatherUrl))
                }
                val aqiDeferred = async {
                    try {
                        val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi"
                        JSONObject(httpGet(aqiUrl))
                    } catch (_: Exception) {
                        null
                    }
                }
                val marineDeferred = async {
                    try {
                        val marineUrl = "https://marine-api.open-meteo.com/v1/marine?latitude=$lat&longitude=$lon" +
                            "&current=wave_height,swell_wave_height,wave_period&timezone=auto"
                        JSONObject(httpGet(marineUrl))
                    } catch (_: Exception) {
                        null
                    }
                }

                val json = weatherDeferred.await()
                val aqiJson = aqiDeferred.await()
                val marineJson = marineDeferred.await()

                val current = json.getJSONObject("current")
                val utcOffsetSeconds = json.optInt("utc_offset_seconds", 0)
                val elevationM = json.optDouble("elevation", 14.0)

                val temp = current.optDouble("temperature_2m", 28.0)
                val humidity = current.optInt("relative_humidity_2m", 65)
                val windSpeed = current.optDouble("wind_speed_10m", 10.0)
                val windDirectionDeg = current.optInt("wind_direction_10m", 130)
                val windGusts = current.optDouble("wind_gusts_10m", (windSpeed * 1.35 * 10.0).roundToInt() / 10.0)
                val cloudCover = current.optInt("cloud_cover", 40)
                val cloudLow = current.optInt("cloud_cover_low", (cloudCover * 0.5).roundToInt())
                val cloudMid = current.optInt("cloud_cover_mid", (cloudCover * 0.3).roundToInt())
                val cloudHigh = current.optInt("cloud_cover_high", (cloudCover * 0.2).roundToInt())
                val precipMm = current.optDouble("precipitation", 0.0)
                val code = current.optInt("weather_code", 1)
                val isDay = current.optInt("is_day", 1) == 1
                val pressure = current.optDouble("surface_pressure", 1010.0)
                val apparentTemp = if (!current.isNull("apparent_temperature")) {
                    current.optDouble("apparent_temperature", temp)
                } else {
                    ((temp + (humidity - 50) * 0.05) * 10.0).roundToInt() / 10.0
                }
                val dewPoint = if (!current.isNull("dew_point_2m")) {
                    current.optDouble("dew_point_2m", calculateDewPoint(temp, humidity))
                } else {
                    calculateDewPoint(temp, humidity)
                }
                val visibilityKm = if (!current.isNull("visibility")) {
                    ((current.optDouble("visibility", 10000.0) / 1000.0) * 10.0).roundToInt() / 10.0
                } else {
                    if (precipMm > 4.0) 5.5 else 10.0
                }

                val currentTimeStr = current.optString("time", "")

                val hourlyObj = json.optJSONObject("hourly")
                val hourlyTimes = hourlyObj?.optJSONArray("time")
                val hourlyTemps = hourlyObj?.optJSONArray("temperature_2m")
                val hourlyCodes = hourlyObj?.optJSONArray("weather_code")
                val hourlyIsDay = hourlyObj?.optJSONArray("is_day")
                val hourlyPrecip = hourlyObj?.optJSONArray("precipitation_probability")
                val hourlyWind = hourlyObj?.optJSONArray("wind_speed_10m")
                val hourlyHum = hourlyObj?.optJSONArray("relative_humidity_2m")
                val hourlyPress = hourlyObj?.optJSONArray("surface_pressure")
                val hourlyUv = hourlyObj?.optJSONArray("uv_index")

                var startIndex = 0
                if (hourlyTimes != null && currentTimeStr.length >= 13) {
                    val hourPrefix = currentTimeStr.substring(0, 13)
                    for (i in 0 until hourlyTimes.length()) {
                        if (hourlyTimes.optString(i, "").startsWith(hourPrefix)) {
                            startIndex = i
                            break
                        }
                    }
                }

                val pressureDelta = if (hourlyPress != null && startIndex >= 3) {
                    val pNow = hourlyPress.optDouble(startIndex, pressure)
                    val pPrev = hourlyPress.optDouble(startIndex - 3, pressure)
                    ((pNow - pPrev) * 10.0).roundToInt() / 10.0
                } else 0.0

                val currentUv = if (hourlyUv != null && startIndex < hourlyUv.length()) {
                    hourlyUv.optDouble(startIndex, if (isDay) 6.0 else 0.0)
                } else if (isDay) 6.0 else 0.0

                val solarRad = if (!current.isNull("shortwave_radiation")) {
                    current.optDouble("shortwave_radiation", if (isDay) 480.0 else 0.0)
                } else {
                    if (isDay) ((1.0 - cloudCover / 135.0) * (currentUv * 95.0)).coerceAtLeast(45.0) else 0.0
                }

                val hourlyForecasts = mutableListOf<HourlyForecast>()
                if (hourlyTimes != null && hourlyTemps != null && hourlyCodes != null) {
                    val end = min(startIndex + 24, hourlyTimes.length())
                    for (i in startIndex until end) {
                        val tStr = hourlyTimes.optString(i, "")
                        val hhmm = if (tStr.contains("T")) tStr.substringAfter("T").take(5) else "%02d:00".format(i % 24)
                        hourlyForecasts.add(
                            HourlyForecast(
                                time = hhmm,
                                temp = hourlyTemps.optDouble(i, temp),
                                code = hourlyCodes.optInt(i, code),
                                isDay = (hourlyIsDay?.optInt(i, 1) ?: 1) == 1,
                                precipProb = hourlyPrecip?.optInt(i, 0) ?: 0,
                                windSpeed = hourlyWind?.optDouble(i, windSpeed) ?: windSpeed,
                                humidity = hourlyHum?.optInt(i, humidity) ?: humidity
                            )
                        )
                    }
                }

                val dailyObj = json.optJSONObject("daily")
                val dailyTimes = dailyObj?.optJSONArray("time")
                val dailyMax = dailyObj?.optJSONArray("temperature_2m_max")
                val dailyMin = dailyObj?.optJSONArray("temperature_2m_min")
                val dailyCodes = dailyObj?.optJSONArray("weather_code")
                val dailyPrecip = dailyObj?.optJSONArray("precipitation_probability_max")
                val dailySunrises = dailyObj?.optJSONArray("sunrise")
                val dailySunsets = dailyObj?.optJSONArray("sunset")

                val dailyForecasts = mutableListOf<DailyForecast>()
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfOut = SimpleDateFormat("EEE", Locale.US)

                if (dailyTimes != null && dailyMax != null && dailyMin != null) {
                    val count = min(7, dailyTimes.length())
                    for (i in 0 until count) {
                        val dStr = dailyTimes.optString(i, "")
                        val dayName = if (i == 0) {
                            "Today"
                        } else {
                            try {
                                val parsed: Date? = sdfIn.parse(dStr)
                                if (parsed != null) sdfOut.format(parsed) else dStr
                            } catch (_: Exception) {
                                dStr
                            }
                        }
                        dailyForecasts.add(
                            DailyForecast(
                                dayName = dayName,
                                tempMax = dailyMax.optDouble(i, temp + 2),
                                tempMin = dailyMin.optDouble(i, temp - 3),
                                code = dailyCodes?.optInt(i, code) ?: code,
                                precipProb = dailyPrecip?.optInt(i, 0) ?: 0,
                                isDay = true
                            )
                        )
                    }
                }

                val sunriseRaw = dailySunrises?.optString(0, "T05:48") ?: "T05:48"
                val sunsetRaw = dailySunsets?.optString(0, "T17:52") ?: "T17:52"
                val sunrise = sunriseRaw.substringAfter("T", "05:48").take(5)
                val sunset = sunsetRaw.substringAfter("T", "17:52").take(5)

                val aqiVal = aqiJson?.optJSONObject("current")?.optInt("us_aqi", 42) ?: 42

                val (estWave, estSwell, estPeriod) = estimateMarineWavesForCoords(lat, lon, windSpeed)
                val marineCur = marineJson?.optJSONObject("current")
                val waveH = if (marineCur != null && !marineCur.isNull("wave_height")) {
                    marineCur.optDouble("wave_height", estWave)
                } else estWave
                val swellH = if (marineCur != null && !marineCur.isNull("swell_wave_height")) {
                    marineCur.optDouble("swell_wave_height", estSwell)
                } else estSwell
                val waveP = if (marineCur != null && !marineCur.isNull("wave_period")) {
                    marineCur.optDouble("wave_period", estPeriod)
                } else estPeriod

                val localHour = try {
                    currentTimeStr.substringAfter("T").substringBefore(":").toInt()
                } catch (_: Exception) {
                    12
                }
                val timeOfDayStr = when {
                    !isDay -> "night"
                    localHour in 5..10 -> "morning"
                    localHour in 16..18 -> "evening"
                    else -> "day"
                }

                val result = WeatherData(
                    temp = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    description = getConditionKey(code),
                    isDay = isDay,
                    locationName = locationName,
                    timeOfDayStr = timeOfDayStr,
                    code = code,
                    utcOffsetSeconds = utcOffsetSeconds,
                    hourly = hourlyForecasts,
                    apparentTemp = apparentTemp,
                    tempMax = dailyForecasts.firstOrNull()?.tempMax ?: (temp + 2.0),
                    tempMin = dailyForecasts.firstOrNull()?.tempMin ?: (temp - 3.0),
                    uvIndex = currentUv,
                    pressure = pressure,
                    visibility = visibilityKm,
                    dewPoint = dewPoint,
                    sunrise = sunrise,
                    sunset = sunset,
                    daily = dailyForecasts,
                    latitude = lat,
                    longitude = lon,
                    aqi = aqiVal,
                    source = "live",
                    windDirectionDeg = windDirectionDeg,
                    windGusts = windGusts,
                    cloudCover = cloudCover,
                    cloudCoverLow = cloudLow,
                    cloudCoverMid = cloudMid,
                    cloudCoverHigh = cloudHigh,
                    precipitationMm = precipMm,
                    seaWindSpeed = ((windSpeed * 1.2) * 10.0).roundToInt() / 10.0,
                    waveHeightM = waveH,
                    swellHeightM = swellH,
                    wavePeriodSec = waveP,
                    seaSurfaceTempC = (((temp * 0.35) + (if (abs(lat) < 12.0) 18.8 else 13.0)) * 10.0).roundToInt() / 10.0,
                    pressureDeltaHpa = pressureDelta,
                    stationElevationM = elevationM,
                    solarRadiationWm2 = ((solarRad * 10.0).roundToInt() / 10.0),
                    observationTimestampMs = System.currentTimeMillis()
                )
                cache[cacheKey] = CacheEntry(result, now)
                result
            }
        } catch (_: Exception) {
            cached?.data?.copy(locationName = locationName, source = "cache")
                ?: offlineWeather(locationName, lat, lon)
        }
    }

    suspend fun fetchWeatherByCity(
        city: String,
        forceRefresh: Boolean = false
    ): WeatherData = withContext(Dispatchers.IO) {
        val clean = city.trim()
        val key = clean.lowercase(Locale.ROOT)
        val baseKey = key.substringBefore(",").trim()

        // Support direct Coordinate input: e.g., "-8.06, 111.90"
        val coordParts = clean.split(",")
        if (coordParts.size == 2) {
            val maybeLat = coordParts[0].trim().toDoubleOrNull()
            val maybeLon = coordParts[1].trim().toDoubleOrNull()
            if (maybeLat != null && maybeLon != null && maybeLat in -90.0..90.0 && maybeLon in -180.0..180.0) {
                val label = "GPS (${"%.2f".format(Locale.US, maybeLat)}, ${"%.2f".format(Locale.US, maybeLon)})"
                return@withContext fetchWeatherByCoordinates(maybeLat, maybeLon, label, forceRefresh)
            }
        }

        val known = CITY_COORDS[key] ?: CITY_COORDS[baseKey]
        if (known != null) {
            return@withContext fetchWeatherByCoordinates(known.first, known.second, known.third, forceRefresh)
        }

        try {
            val encoded = URLEncoder.encode(clean.substringBefore(",").trim(), "UTF-8")
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
            val geoJson = JSONObject(httpGet(geoUrl))
            val results = geoJson.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.getJSONObject(0)
                val lat = first.getDouble("latitude")
                val lon = first.getDouble("longitude")
                val name = first.optString("name", clean)
                val countryCode = first.optString("country_code", "")
                val displayName = if (countryCode.isNotEmpty()) "$name, $countryCode" else name
                return@withContext fetchWeatherByCoordinates(lat, lon, displayName, forceRefresh)
            }
        } catch (_: Exception) {
        }

        offlineWeather(clean)
    }
}
