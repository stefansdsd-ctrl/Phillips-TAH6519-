package com.example.util

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

object BluetoothChimeSynthesizer {

    @Suppress("DEPRECATION")
    suspend fun playConnectChime(volume: Float = 0.8f) = withContext(Dispatchers.Default) {
        try {
            val sampleRate = 44100
            // 3 notes: C5 (523.25 Hz, 120ms), E5 (659.25 Hz, 120ms), G5+C6 (783.99 + 1046.50 Hz, 350ms)
            val duration1 = (sampleRate * 0.12).toInt()
            val duration2 = (sampleRate * 0.12).toInt()
            val duration3 = (sampleRate * 0.35).toInt()
            val totalSamples = duration1 + duration2 + duration3

            val generatedSnd = ByteArray(2 * totalSamples)
            var byteIdx = 0

            // Note 1: 523.25 Hz (C5)
            for (i in 0 until duration1) {
                val t = i.toDouble() / sampleRate
                val envelope = sin(Math.PI * i / duration1) // Smooth bell envelope
                val wave = sin(2.0 * Math.PI * 523.25 * t)
                val valShort = (wave * envelope * 0.6 * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                generatedSnd[byteIdx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[byteIdx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            // Note 2: 659.25 Hz (E5)
            for (i in 0 until duration2) {
                val t = i.toDouble() / sampleRate
                val envelope = sin(Math.PI * i / duration2)
                val wave = sin(2.0 * Math.PI * 659.25 * t) + 0.3 * sin(2.0 * Math.PI * 1318.5 * t)
                val valShort = (wave * envelope * 0.5 * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                generatedSnd[byteIdx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[byteIdx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            // Note 3: Dual chord G5 + C6
            for (i in 0 until duration3) {
                val t = i.toDouble() / sampleRate
                val decay = Math.exp(-4.0 * i / duration3) // Exponential decay
                val attack = if (i < 200) i / 200.0 else 1.0
                val envelope = attack * decay
                val wave = 0.5 * sin(2.0 * Math.PI * 783.99 * t) + 0.5 * sin(2.0 * Math.PI * 1046.50 * t) + 0.2 * sin(2.0 * Math.PI * 1567.98 * t)
                val valShort = (wave * envelope * 0.7 * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                generatedSnd[byteIdx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[byteIdx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufferSize, generatedSnd.size),
                AudioTrack.MODE_STATIC
            )

            track.write(generatedSnd, 0, generatedSnd.size)
            track.play()

            delay(650)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    suspend fun playDisconnectChime(volume: Float = 0.7f) = withContext(Dispatchers.Default) {
        try {
            val sampleRate = 44100
            // 2 notes descending: G5 (783.99 Hz, 140ms), C5 (523.25 Hz, 250ms)
            val duration1 = (sampleRate * 0.14).toInt()
            val duration2 = (sampleRate * 0.25).toInt()
            val totalSamples = duration1 + duration2

            val generatedSnd = ByteArray(2 * totalSamples)
            var byteIdx = 0

            for (i in 0 until duration1) {
                val t = i.toDouble() / sampleRate
                val envelope = sin(Math.PI * i / duration1)
                val wave = sin(2.0 * Math.PI * 783.99 * t)
                val valShort = (wave * envelope * 0.5 * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                generatedSnd[byteIdx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[byteIdx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            for (i in 0 until duration2) {
                val t = i.toDouble() / sampleRate
                val decay = Math.exp(-5.0 * i / duration2)
                val wave = sin(2.0 * Math.PI * 523.25 * t)
                val valShort = (wave * decay * 0.6 * 32767 * volume).toInt().coerceIn(-32768, 32767).toShort()
                generatedSnd[byteIdx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[byteIdx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufferSize, generatedSnd.size),
                AudioTrack.MODE_STATIC
            )

            track.write(generatedSnd, 0, generatedSnd.size)
            track.play()

            delay(450)
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
