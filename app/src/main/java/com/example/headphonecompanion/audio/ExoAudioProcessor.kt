package com.example.headphonecompanion.audio

import com.example.headphonecompanion.dsp.ParametricEq
import com.google.android.exoplayer2.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ExoAudioProcessor: an ExoPlayer AudioProcessor that applies a chain of biquad peaking filters
 * per channel. This is a proof-of-concept and supports 16-bit PCM (ENCODING_PCM_16BIT) and
 * common sample rates (44.1k, 48k). It is NOT optimized for production; consider NDK porting
 * for low-latency and efficient multi-channel processing.
 *
 * Note: This class depends on ExoPlayer's AudioProcessor interface (com.google.android.exoplayer2.audio.AudioProcessor).
 * media3-exoplayer should provide the dependency. Build may require adjusting versions.
 */
class ExoAudioProcessor : AudioProcessor {
    private var sampleRateHz: Int = 44100
    private var channelCount: Int = 2
    private var inputEncoding: Int = AudioProcessor.ENCODING_PCM_16BIT

    private var pendingOutputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputBuffer: ByteBuffer = EMPTY_BUFFER

    // One chain per channel: list of biquads per channel
    private var filters: Array<Array<Biquad>> = arrayOf()

    override fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int): Boolean {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        this.inputEncoding = encoding
        // Initialize filters for each channel if empty
        if (filters.isEmpty()) {
            filters = Array(channelCount) { arrayOf() }
        } else if (filters.size != channelCount) {
            filters = Array(channelCount) { arrayOf() }
        }
        return false
    }

    override fun isActive(): Boolean {
        return !filters.all { it.isEmpty() } // active if any filter is installed
    }

    override fun queueInput(buffer: ByteBuffer) {
        // Copy input buffer for processing
        if (inputBuffer.capacity() < buffer.remaining()) {
            inputBuffer = ByteBuffer.allocateDirect(buffer.remaining()).order(ByteOrder.nativeOrder())
        } else {
            inputBuffer.clear()
        }
        inputBuffer.put(buffer)
        inputBuffer.flip()

        // Prepare output buffer
        if (pendingOutputBuffer.capacity() < inputBuffer.remaining()) {
            pendingOutputBuffer = ByteBuffer.allocateDirect(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        } else {
            pendingOutputBuffer.clear()
        }

        // Only supporting 16-bit PCM for now
        if (inputEncoding != AudioProcessor.ENCODING_PCM_16BIT) {
            // passthrough
            pendingOutputBuffer.put(inputBuffer)
            pendingOutputBuffer.flip()
            inputBuffer.position(inputBuffer.limit())
            return
        }

        // Process samples: interleaved PCM16
        val little = inputBuffer.order() == ByteOrder.LITTLE_ENDIAN
        inputBuffer.order(ByteOrder.nativeOrder())

        while (inputBuffer.remaining() >= 2 * channelCount) {
            val samples = FloatArray(channelCount)
            for (ch in 0 until channelCount) {
                val s = inputBuffer.short.toInt()
                // normalize to -1..1
                samples[ch] = s / 32768.0f
            }
            // apply filters per channel
            for (ch in 0 until channelCount) {
                var out = samples[ch]
                val chain = if (ch < filters.size) filters[ch] else arrayOf<Biquad>()
                for (bq in chain) {
                    out = bq.process(out)
                }
                // clamp
                val clamped = (out.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                pendingOutputBuffer.putShort(clamped)
            }
        }
        pendingOutputBuffer.flip()
        // mark input consumed
        inputBuffer.position(inputBuffer.limit())
    }

    override fun getOutput(): ByteBuffer {
        val out = pendingOutputBuffer
        pendingOutputBuffer = EMPTY_BUFFER
        return out
    }

    override fun flush() {
        pendingOutputBuffer = EMPTY_BUFFER
        inputBuffer = EMPTY_BUFFER
        // reset filter states
        for (chain in filters) {
            for (bq in chain) bq.reset()
        }
    }

    override fun reset() {
        flush()
    }

    override fun isEnded(): Boolean {
        return false
    }

    override fun queueEndOfStream() {
        // no-op
    }

    override fun getMediaFormat() = null

    // Public API to set parametric EQ and build per-channel filter chains
    fun setParametricEq(eq: ParametricEq) {
        // For simplicity we build the same chain for each channel. Production code may vary.
        val chains = Array(channelCount) { index ->
            eq.bands.map { band ->
                Biquad.peaking(sampleRateHz.toFloat(), band.centerHz, band.q, band.gainDb)
            }.toTypedArray()
        }
        filters = chains
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0)
    }
}
