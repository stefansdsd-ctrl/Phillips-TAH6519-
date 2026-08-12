package com.example.headphonecompanion.dsp

import com.example.headphonecompanion.dsp.EqBand
import com.example.headphonecompanion.dsp.ParametricEq

/**
 * Maps simple hearing test results (frequency -> heard bool) to a ParametricEq.
 * This is a naive mapping: frequencies the user did NOT hear get small boosts.
 * This is NOT a clinical hearing compensation. Use only as a baseline POC.
 */
object AudiogramToEq {
    fun map(results: Map<Float, Boolean>): ParametricEq {
        val bands = results.map { (freq, heard) ->
            val gain = if (heard) 0f else 6f // boost up to +6dB where not heard
            EqBand(freq, gain, 1f)
        }
        return ParametricEq(bands)
    }
}
