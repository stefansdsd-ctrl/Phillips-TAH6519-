package com.example.headphonecompanion.audio

import android.media.audiofx.Equalizer
import android.util.Log
import kotlin.math.abs

/**
 * EqualizerManager: wraps Android's media Equalizer and applies parametric EQ settings.
 * Note: Equalizer operates on a specific audio session (audioSessionId). For ExoPlayer,
 * use player.audioSessionId. Using 0 may apply to global output on some devices but is not
 * guaranteed. Test on target device and pass the correct audioSessionId from the player.
 */
class EqualizerManager(private val audioSessionId: Int) {
    private var equalizer: Equalizer? = null

    fun init() {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId)
            equalizer?.enabled = true
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to init Equalizer", e)
        }
    }

    fun applyParametricEq(paramEq: com.example.headphonecompanion.dsp.ParametricEq) {
        val eq = equalizer ?: run {
            init()
            equalizer ?: return
        }

        val numBands = eq.numberOfBands.toInt()
        if (numBands <= 0) return

        val range = eq.bandLevelRange // short array [min, max] in millibels
        val minLevel = range[0].toInt()
        val maxLevel = range[1].toInt()

        val bandFreqs = IntArray(numBands)
        for (i in 0 until numBands) {
            bandFreqs[i] = eq.getCenterFreq(i.toShort()) // in milliHz
        }

        // Map each parametric band to the nearest hardware EQ band and set level
        for (band in paramEq.bands) {
            // target in milliHz
            val target = (band.centerHz * 1000f).toLong()
            var closestIdx = 0
            var closestDiff = Long.MAX_VALUE
            for (i in bandFreqs.indices) {
                val diff = abs(bandFreqs[i].toLong() - target)
                if (diff < closestDiff) {
                    closestDiff = diff
                    closestIdx = i
                }
            }

            // Android Equalizer band levels are in millibels (1 dB = 100 mB)
            val levelMb = (band.gainDb * 100f).toInt()
            val clamped = levelMb.coerceIn(minLevel, maxLevel)
            try {
                eq.setBandLevel(closestIdx.toShort(), clamped.toShort())
            } catch (e: Exception) {
                Log.w("EqualizerManager", "Failed to set band level: $closestIdx -> $clamped", e)
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            // ignore
        }
        equalizer = null
    }
}
