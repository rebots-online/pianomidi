package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class PianoSynth {

    private companion object {
        const val SAMPLE_RATE = 44100
        const val MAX_VOICES = 16
        const val BUFFER_SIZE = 1024
        const val TAG = "PianoSynth"
    }

    class Voice {
        var active: Boolean = false
        var pitch: Int = -1
        var frequency: Double = 0.0
        var phase: Double = 0.0
        var velocity: Float = 0.0f
        var currentEnvelope: Double = 1.0
        var isReleased: Boolean = false

        // Decay coefficients
        var holdDecayRate: Double = 0.99995  // slowly fades while held
        var releaseDecayRate: Double = 0.992 // rapidly dampens on key release
    }

    private val voices = Array(MAX_VOICES) { Voice() }
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val synthScope = CoroutineScope(Dispatchers.Default)

    // Metronome state inside the sound thread (sample-accurate!)
    @Volatile var isMetronomeEnabled: Boolean = false
    private var metronomeTickSamplesRemaining = 0
    private var isDownbeatTick = false

    // Synthesis Parameters (editable in the detailed settings view)
    @Volatile var attackFactor: Float = 0.0f // Immediate attack is default
    @Volatile var decayFactor: Float = 0.5f  // Decay speed multiplier
    @Volatile var overtoneMix: Float = 0.6f  // Slider for overtone brightness

    // Callback for real-time visualizer mapping
    // Exposes current mixed amplitude, active notes, and beat pulses
    var onAudioFrameCalculated: ((amplitude: Float, activePitches: List<Int>) -> Unit)? = null

    init {
        start()
    }

    fun start() {
        if (synthJob != null) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val finalBufferSize = minBufferSize.coerceAtLeast(BUFFER_SIZE * 2)

            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                finalBufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }

        synthJob = synthScope.launch {
            val shortBuffer = ShortArray(BUFFER_SIZE)
            while (audioTrack != null) {
                // Synthesize a chunk of samples
                var maxVal = 0.0f
                val currentPitches = mutableListOf<Int>()

                for (i in 0 until BUFFER_SIZE) {
                    var sampleSum = 0.0

                    // 1. Process active polyphonic voices
                    for (voice in voices) {
                        if (voice.active) {
                            val f = voice.frequency
                            val p = voice.phase

                            // Rich additive timbre: Fundamental + Harmonics
                            val s1 = sin(p)
                            val s2 = sin(p * 2.0) * 0.45f * overtoneMix
                            val s3 = sin(p * 3.0) * 0.22f * overtoneMix
                            val s4 = sin(p * 4.0) * 0.12f * overtoneMix
                            val s5 = sin(p * 5.0) * 0.06f * overtoneMix

                            val rawWave = s1 + s2 + s3 + s4 + s5
                            val env = voice.currentEnvelope
                            sampleSum += rawWave * env * voice.velocity

                            if (i == 0) {
                                currentPitches.add(voice.pitch)
                            }

                            // Advance phase
                            voice.phase += (2.0 * PI * f) / SAMPLE_RATE
                            if (voice.phase > 2.0 * PI) {
                                voice.phase -= 2.0 * PI
                            }

                            // Dynamic envelope decay coefficients
                            val dynamicHoldDecay = 1.0 - (0.00005 * (1.0 + decayFactor))
                            val dynamicReleaseDecay = 1.0 - (0.008 * (1.0 + decayFactor))

                            // Apply envelope decay
                            if (voice.isReleased) {
                                voice.currentEnvelope *= dynamicReleaseDecay
                                if (voice.currentEnvelope < 0.001) {
                                    voice.active = false
                                }
                            } else {
                                voice.currentEnvelope *= dynamicHoldDecay
                                if (voice.currentEnvelope < 0.0001) {
                                    voice.active = false
                                }
                            }
                        }
                    }

                    // 2. Synthesize metronome click if triggered
                    if (isMetronomeEnabled && metronomeTickSamplesRemaining > 0) {
                        val tickPhase = (800 - metronomeTickSamplesRemaining).toDouble()
                        val freq = if (isDownbeatTick) 1500.0 else 900.0
                        val tickEnv = metronomeTickSamplesRemaining.toDouble() / 800.0
                        val tickVal = sin(2.0 * PI * freq * tickPhase / SAMPLE_RATE) * tickEnv * 0.35
                        sampleSum += tickVal
                        metronomeTickSamplesRemaining--
                    }

                    // Soft clipper to prevent harsh digital clipping
                    sampleSum = sampleSum.coerceIn(-1.0, 1.0)
                    
                    // Convert to 16-bit signed PCM short
                    val shortSample = (sampleSum * 32767.0).toInt().toShort()
                    shortBuffer[i] = shortSample

                    // Track maximum peak amplitude for visualizers
                    val absVal = Math.abs(sampleSum.toFloat())
                    if (absVal > maxVal) {
                        maxVal = absVal
                    }
                }

                // Notify callback for graphics visualization
                onAudioFrameCalculated?.invoke(maxVal, currentPitches)

                // Write block to hardware
                audioTrack?.write(shortBuffer, 0, BUFFER_SIZE)
            }
        }
    }

    /**
     * Trigger note on (key press). Finds an idle voice, calculates the frequency,
     * and initializes the dynamic envelopes.
     */
    fun noteOn(pitch: Int, velocity: Float) {
        val freq = 440.0 * Math.pow(2.0, (pitch - 69).toDouble() / 12.0)
        
        // Find existing voice with same pitch to retrigger or release
        val existingVoice = voices.find { it.active && it.pitch == pitch }
        if (existingVoice != null) {
            existingVoice.phase = 0.0
            existingVoice.velocity = velocity
            existingVoice.currentEnvelope = 1.0
            existingVoice.isReleased = false
            return
        }

        // Find idle voice
        var voiceToUse = voices.find { !it.active }
        
        // If voice limit reached, steal the oldest/quietest voice
        if (voiceToUse == null) {
            voiceToUse = voices.minByOrNull { if (it.active) it.currentEnvelope else Double.MAX_VALUE }
        }

        voiceToUse?.apply {
            this.active = true
            this.pitch = pitch
            this.frequency = freq
            this.phase = 0.0
            this.velocity = velocity
            this.currentEnvelope = 1.0
            this.isReleased = false
        }
    }

    /**
     * Trigger note off (key release). Initiates rapid decay phase.
     */
    fun noteOff(pitch: Int) {
        voices.forEach { voice ->
            if (voice.active && voice.pitch == pitch) {
                voice.isReleased = true
            }
        }
    }

    /**
     * Triggers a metronome click sample in the audio thread buffer
     */
    fun triggerMetronomeTick(isDownbeat: Boolean) {
        isDownbeatTick = isDownbeat
        metronomeTickSamplesRemaining = 800 // ~18ms of woodblock tick sound
    }

    /**
     * All notes off
     */
    fun releaseAll() {
        voices.forEach { it.isReleased = true }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Silently close
        }
        audioTrack = null
    }
}
