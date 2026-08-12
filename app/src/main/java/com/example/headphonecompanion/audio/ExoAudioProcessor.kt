package com.example.headphonecompanion.audio

import com.example.headphonecompanion.dsp.ParametricEq
import com.google.android.exoplayer2.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ExoAudioProcessor: an ExoPlayer AudioProcessor that applies a chain of biquad peaking filters
 * per channel, or optionally routes blocks through the native FFT convolver when an IR is supplied.
 * This is a proof-of-concept and supports 16-bit PCM (ENCODING_PCM_16BIT) and
 * common sample rates (44.1k, 48k). It is NOT optimized for production; consider NDK porting
 * for low-latency and efficient multi-channel processing.
 */
class ExoAudioProcessor : AudioProcessor {
    private var sampleRateHz: Int = 44100
    private var channelCount: Int = 2
    private var inputEncoding: Int = AudioProcessor.ENCODING_PCM_16BIT

    private var pendingOutputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputBuffer: ByteBuffer = EMPTY_BUFFER

    // One chain per channel: list of biquads per channel
    private var filters: Array<Array<Biquad>> = arrayOf()

    // Optional FIR IR per channel (same IR for each channel by default)
    private var irPerChannel: Array<FloatArray?> = arrayOf()

    // Toggle: if true and IR is present, use native convolver for processing
    private var useNativeFIR: Boolean = false

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
        if (irPerChannel.isEmpty() || irPerChannel.size != channelCount) {
            irPerChannel = Array(channelCount) { null }
        }
        return false
    }

    override fun isActive(): Boolean {
        // active if any filter is installed or an IR is set and native FIR enabled
        return !filters.all { it.isEmpty() } || (useNativeFIR && irPerChannel.any { it != null })
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

        // If native FIR is requested and we have at least one IR, route through native convolver per channel
        if (useNativeFIR && irPerChannel.any { it != null }) {
            // Convert interleaved PCM16 to per-channel float vectors
            val totalSamples = inputBuffer.remaining() / 2 // shorts
            val frames = totalSamples / channelCount
            val channelInputs = Array(channelCount) { FloatArray(frames) }
            for (f in 0 until frames) {
                for (ch in 0 until channelCount) {
                    val s = inputBuffer.short.toInt()
                    channelInputs[ch][f] = s / 32768.0f
                }
            }

            // For each channel, convolve block with IR (if present) using native convolver
            val processedChannels = Array(channelCount) { FloatArray(frames) }
            for (ch in 0 until channelCount) {
                val ir = irPerChannel[ch] ?: irPerChannel.firstOrNull { it != null }
                if (ir != null) {
                    val conv = NativeConvolver.convolve(channelInputs[ch], ir)
                    if (conv != null && conv.isNotEmpty()) {
                        // conv length = frames + irLen - 1. For streaming, use the first 'frames' samples (simple, non-overlap approach)
                        val len = minOf(frames, conv.size)
                        for (i in 0 until len) processedChannels[ch][i] = conv[i]
                        // if conv shorter than frames, zero-fill remainder
                        for (i in len until frames) processedChannels[ch][i] = 0f
                    } else {
                        // fallback: passthrough
                        processedChannels[ch] = channelInputs[ch]
                    }
                } else {
                    // no IR for this channel: passthrough or apply biquads
                    var arr = channelInputs[ch]
                    // apply biquad chain if present
                    val chain = if (ch < filters.size) filters[ch] else arrayOf<Biquad>()
                    if (chain.isNotEmpty()) {
                        val out = FloatArray(frames)
                        for (i in 0 until frames) {
                            var sample = arr[i]
                            for (bq in chain) sample = bq.process(sample)
                            out[i] = sample
                        }
                        processedChannels[ch] = out
                    } else {
                        processedChannels[ch] = arr
                    }
                }
            }

            // Write interleaved processedChannels to pendingOutputBuffer as PCM16
            for (i in 0 until frames) {
                for (ch in 0 until channelCount) {
                    val outf = processedChannels[ch][i].coerceIn(-1f, 1f)
                    val outShort = (outf * 32767f).toInt().toShort()
                    pendingOutputBuffer.putShort(outShort)
                }
            }

            pendingOutputBuffer.flip()
            inputBuffer.position(inputBuffer.limit())
            return
        }

        // Default: apply biquad chains per channel in streaming fashion
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

    // Public API to set an IR for FIR convolution. Pass null to clear.
    fun setFIRForAllChannels(ir: FloatArray?) {
        if (ir == null) {
            irPerChannel = Array(channelCount) { null }
        } else {
            irPerChannel = Array(channelCount) { ir }
        }
    }

    fun enableNativeFIR(enable: Boolean) {
        useNativeFIR = enable
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0)
    }
}
