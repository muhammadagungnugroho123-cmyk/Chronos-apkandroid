package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Native Android 11+ (API 30+) PCM Synthesizer, Custom Device Music Player, 8-Channel Chime Engine & HD Haptics
 * Zero-Crash Guarded against AudioFlinger exhaustion, missing URI permissions, and headless environment exceptions.
 */
object ChronosAudioSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var soundscapeJob: Job? = null
    private var chimeJob: Job? = null
    private var customMediaPlayer: MediaPlayer? = null
    @Volatile
    private var activeChimeTrack: AudioTrack? = null
    @Volatile
    private var activeSoundscapeTrack: AudioTrack? = null

    @Volatile
    var isPlayingSoundscape: Boolean = false
        private set

    @Volatile
    var isPlayingAlarmChime: Boolean = false
        private set

    @Volatile
    var currentSoundType: String = "NONE"
        private set

    @Volatile
    var volume: Float = 0.75f

    @Volatile
    var hapticsEnabled: Boolean = true

    fun isCustomDeviceUri(soundUri: String): Boolean {
        return soundUri.startsWith("content://", ignoreCase = true) ||
            soundUri.startsWith("file://", ignoreCase = true) ||
            soundUri.startsWith("android.resource://", ignoreCase = true)
    }

    fun queryDisplayName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Throwable) {
        }
        val seg = uri.lastPathSegment ?: "Musik_Kustom.mp3"
        return seg.substringAfterLast('/').substringAfterLast(':').ifBlank { "Musik_Kustom.mp3" }
    }

    fun triggerHaptic(context: Context?, style: Int = 0) {
        if (!hapticsEnabled || context == null) return
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val effectId = when (style) {
                1 -> VibrationEffect.EFFECT_TICK
                2 -> VibrationEffect.EFFECT_HEAVY_CLICK
                3 -> VibrationEffect.EFFECT_DOUBLE_CLICK
                else -> VibrationEffect.EFFECT_CLICK
            }
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } catch (_: Throwable) {
            // Zero-crash guard for devices without vibrator hardware
        }
    }

    fun toggleSoundscape(type: String, onUpdate: () -> Unit = {}) {
        if (isPlayingSoundscape && currentSoundType == type) {
            stopSoundscape()
        } else {
            playSoundscape(type)
        }
        onUpdate()
    }

    fun playSoundscape(type: String) {
        stopSoundscape()
        if (type == "NONE") return
        currentSoundType = type
        isPlayingSoundscape = true

        soundscapeJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                activeSoundscapeTrack = track
                track.play()
                val samples = ShortArray(1024)
                var phase1 = 0.0
                var phase2 = 0.0
                var phase3 = 0.0
                var lastNoise = 0.0

                while (isActive && isPlayingSoundscape) {
                    val vol = volume.coerceIn(0.05f, 1f)
                    for (i in samples.indices) {
                        val rawSample: Double = when (type) {
                            "FOCUS_BINAURAL" -> {
                                phase1 += 2.0 * PI * 200.0 / sampleRate
                                phase2 += 2.0 * PI * 240.0 / sampleRate // 40Hz Gamma/Alpha Focus beat
                                phase3 += 2.0 * PI * 432.0 / sampleRate
                                (sin(phase1) + sin(phase2) + 0.45 * sin(phase3)) * 0.18
                            }
                            "VITALITY_PULSE" -> {
                                phase1 += 2.0 * PI * 528.0 / sampleRate
                                phase2 += 2.0 * PI * 261.63 / sampleRate
                                (sin(phase1) * 0.15 + sin(phase2) * 0.12)
                            }
                            "MORNING_BIRDS" -> {
                                phase1 += 2.0 * PI * 264.0 / sampleRate
                                phase2 += 2.0 * PI * 330.0 / sampleRate
                                phase3 += 2.0 * PI * 396.0 / sampleRate
                                (sin(phase1) + sin(phase2) + sin(phase3)) * 0.12
                            }
                            "OCEAN_WAVES" -> {
                                phase1 += 2.0 * PI * 0.18 / sampleRate
                                val waveEnv = 0.35 + 0.65 * ((sin(phase1) + 1.0) * 0.5)
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastNoise = (lastNoise + 0.035 * white) / 1.035
                                lastNoise * 0.42 * waveEnv
                            }
                            "RAIN", "STREAM", "WIND", "NIGHT" -> {
                                val coeff = if (type == "WIND") 0.02 else 0.05
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastNoise = (lastNoise + coeff * white) / (1.0 + coeff)
                                lastNoise * 0.36
                            }
                            else -> 0.0
                        }
                        val scaled = (rawSample * vol * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        samples[i] = scaled.toShort()
                    }
                    track.write(samples, 0, samples.size)
                }
            } catch (_: Throwable) {
                // Safe fallback in headless/emulator environments
            } finally {
                if (activeSoundscapeTrack === track) {
                    activeSoundscapeTrack = null
                }
                try {
                    track?.stop()
                } catch (_: Throwable) {}
                try {
                    track?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    fun stopSoundscape() {
        isPlayingSoundscape = false
        currentSoundType = "NONE"
        try {
            activeSoundscapeTrack?.pause()
            activeSoundscapeTrack?.flush()
            activeSoundscapeTrack?.stop()
        } catch (_: Throwable) {}
        activeSoundscapeTrack = null
        soundscapeJob?.cancel()
        soundscapeJob = null
    }

    fun ringtoneIndexFromName(name: String): Int = when {
        name.contains("Vitality", ignoreCase = true) -> 0
        name.contains("Cosmos", ignoreCase = true) -> 1
        name.contains("Pulse", ignoreCase = true) -> 2
        name.contains("Zen", ignoreCase = true) -> 3
        name.contains("Cyberpunk", ignoreCase = true) -> 4
        name.contains("Birds", ignoreCase = true) -> 5
        name.contains("Ocean", ignoreCase = true) -> 6
        name.contains("Digital", ignoreCase = true) || name.contains("Beep", ignoreCase = true) -> 7
        else -> 0
    }

    /**
     * Plays either a user-selected custom device music file (MP3/WAV/OGG/FLAC/M4A via content:// URI)
     * with smooth fade-in and looping, OR falls back to the built-in 8-channel PCM synthesizer.
     */
    fun playCustomOrPresetAlarm(
        context: Context?,
        soundUri: String,
        durationSeconds: Int = 45,
        fadeInSeconds: Int = 0
    ): () -> Unit {
        stopChime()
        if (context != null && isCustomDeviceUri(soundUri)) {
            chimeJob = scope.launch {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + (durationSeconds.coerceIn(1, 180) * 1000L)
                var player: MediaPlayer? = null
                try {
                    val uri = Uri.parse(soundUri)
                    player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(context, uri)
                        isLooping = true
                        prepare()
                    }
                    customMediaPlayer = player
                    val initialVol = if (fadeInSeconds > 0) (volume * 0.15f).coerceIn(0.05f, 1f) else volume.coerceIn(0.1f, 1f)
                    player.setVolume(initialVol, initialVol)
                    player.start()

                    while (isActive && System.currentTimeMillis() < endTime) {
                        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                        val fadeFactor = if (fadeInSeconds > 0) {
                            min(1.0, (elapsedSec / fadeInSeconds.toDouble()).coerceAtLeast(0.15))
                        } else 1.0
                        val currentVol = (volume.coerceIn(0.1f, 1f) * fadeFactor.toFloat()).coerceIn(0.05f, 1f)
                        try {
                            player.setVolume(currentVol, currentVol)
                        } catch (_: Throwable) {}
                        delay(200L)
                    }
                } catch (_: Throwable) {
                    // Fallback to synthesized chime if device music file was deleted or permission revoked
                    playSynthesizedChimeInternal(0, durationSeconds, fadeInSeconds)
                } finally {
                    try {
                        player?.stop()
                    } catch (_: Throwable) {}
                    try {
                        player?.release()
                    } catch (_: Throwable) {}
                    if (customMediaPlayer === player) {
                        customMediaPlayer = null
                    }
                }
            }
            return { stopChime() }
        }
        return playAlarmChime(
            ringtoneIndex = ringtoneIndexFromName(soundUri),
            durationSeconds = durationSeconds,
            fadeInSeconds = fadeInSeconds
        )
    }

    fun playAlarmChime(
        ringtoneIndex: Int = 0,
        durationSeconds: Int = 4,
        fadeInSeconds: Int = 0
    ): () -> Unit {
        stopChime()
        chimeJob = scope.launch {
            playSynthesizedChimeInternal(ringtoneIndex, durationSeconds, fadeInSeconds)
        }
        return { stopChime() }
    }

    private suspend fun playSynthesizedChimeInternal(
        ringtoneIndex: Int,
        durationSeconds: Int,
        fadeInSeconds: Int
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds.coerceIn(1, 120) * 1000L)
        val chordFreqs = when (ringtoneIndex.coerceIn(0, 7)) {
            0 -> doubleArrayOf(528.0, 660.0, 792.0, 1056.0) // Vitality Spirit
            1 -> doubleArrayOf(432.0, 540.0, 648.0, 864.0)  // AI Cosmos
            2 -> doubleArrayOf(440.0, 554.37, 659.25, 880.0) // AI Pulse
            3 -> doubleArrayOf(396.0, 495.0, 594.0, 792.0)  // Zen Garden
            4 -> doubleArrayOf(330.0, 440.0, 660.0, 880.0)  // Cyberpunk
            5 -> doubleArrayOf(587.33, 740.0, 880.0, 1174.66) // Morning Birds
            6 -> doubleArrayOf(293.66, 369.99, 440.0, 587.33) // Ocean Waves
            else -> doubleArrayOf(800.0, 1000.0, 800.0, 1200.0) // Digital Beep
        }

        val sampleRate = 22050
        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        var track: AudioTrack? = null
        var toneGen: ToneGenerator? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .build()
                activeChimeTrack = track
                isPlayingAlarmChime = true
                track.play()

                val chunk = ShortArray(1024)
                var phase = 0.0
                var stepIdx = 0
                var samplesInNote = 0
                val noteDurationSamples = sampleRate / 4 // 250ms per arpeggio note

                while (isPlayingAlarmChime && kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive != false && System.currentTimeMillis() < endTime) {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    val fadeFactor = if (fadeInSeconds > 0) {
                        val norm = (elapsedSec / fadeInSeconds.toDouble()).coerceIn(0.0, 1.0)
                        (Math.pow(norm, 1.7) * 0.88 + 0.12).coerceIn(0.12, 1.0)
                    } else 1.0
                    val masterVol = volume.coerceIn(0.1f, 1f) * fadeFactor

                    for (i in chunk.indices) {
                        val freq = chordFreqs[stepIdx % chordFreqs.size]
                        phase += 2.0 * PI * freq / sampleRate
                        val env = 1.0 - (samplesInNote.toDouble() / noteDurationSamples.toDouble()).coerceIn(0.0, 0.85)
                        val wave = (sin(phase) * 0.24 + sin(phase * 2.0) * 0.08 + sin(phase * 3.0) * 0.02) * env * masterVol
                        chunk[i] = (wave * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                        samplesInNote++
                        if (samplesInNote >= noteDurationSamples) {
                            samplesInNote = 0
                            stepIdx++
                        }
                    }
                    track.write(chunk, 0, chunk.size)
                }
            } catch (_: Throwable) {
                // Fallback to a single reused ToneGenerator instance
                try {
                    isPlayingAlarmChime = true
                    toneGen = ToneGenerator(AudioManager.STREAM_ALARM, (volume * 90).toInt().coerceIn(20, 100))
                    while (isPlayingAlarmChime && kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive != false && System.currentTimeMillis() < endTime) {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 320)
                        delay(750L)
                    }
                } catch (_: Throwable) {
                }
            } finally {
                if (activeChimeTrack === track) {
                    activeChimeTrack = null
                }
                isPlayingAlarmChime = false
                try {
                    track?.stop()
                } catch (_: Throwable) {}
                try {
                    track?.release()
                } catch (_: Throwable) {}
                try {
                    toneGen?.release()
                } catch (_: Throwable) {}
            }
    }

    fun playTacticalBeep(freqHz: Double = 880.0, durationMs: Int = 95) {
        scope.launch {
            val sampleRate = 22050
            val totalSamples = (sampleRate * (durationMs.coerceIn(40, 400) / 1000.0)).toInt().coerceAtLeast(256)
            val bufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(totalSamples * 2)
            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                val samples = ShortArray(totalSamples)
                val vol = volume.coerceIn(0.1f, 1f)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    phase += 2.0 * PI * freqHz / sampleRate
                    val env = 1.0 - (i.toDouble() / totalSamples.toDouble()) * 0.75
                    val s = sin(phase) * 0.28 * env * vol
                    samples[i] = (s * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                track.write(samples, 0, samples.size)
                track.play()
                delay(durationMs.toLong() + 40L)
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                } catch (_: Throwable) {}
                try {
                    track?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    fun stopChime() {
        isPlayingAlarmChime = false
        try {
            activeChimeTrack?.pause()
            activeChimeTrack?.flush()
            activeChimeTrack?.stop()
        } catch (_: Throwable) {}
        activeChimeTrack = null
        chimeJob?.cancel()
        chimeJob = null
        try {
            customMediaPlayer?.stop()
        } catch (_: Throwable) {}
        try {
            customMediaPlayer?.release()
        } catch (_: Throwable) {}
        customMediaPlayer = null
    }

    fun releaseAll() {
        stopSoundscape()
        stopChime()
    }
}
