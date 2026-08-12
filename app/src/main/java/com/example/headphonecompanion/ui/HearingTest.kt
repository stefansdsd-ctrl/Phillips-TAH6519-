package com.example.headphonecompanion.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.headphonecompanion.dsp.ParametricEq
import com.example.headphonecompanion.dsp.EqBand
import com.example.headphonecompanion.dsp.AudiogramToEq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Very simple hearing test POC. Plays tones at fixed frequencies and asks the user
 * if they hear them. Not a clinical test. Produces a basic ParametricEq using AudiogramToEq.
 */
@Composable
fun HearingTestScreen(onDone: (ParametricEq) -> Unit, modifier: Modifier = Modifier) {
    val freqs = listOf(125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f)
    var index by remember { mutableStateOf(0) }
    var results by remember { mutableStateOf(mutableMapOf<Float, Boolean>()) }
    var playing by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Hearing Test (POC)")
        Text("Frequency: ${freqs[index].toInt()} Hz", modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = {
            if (!playing) {
                playing = true
                // play tone asynchronously
                playTone(freqs[index]) {
                    playing = false
                }
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(if (playing) "Playing..." else "Play Tone")
        }

        Button(onClick = {
            results[freqs[index]] = true
            if (index < freqs.size - 1) index += 1 else {
                // finish
                val eq = AudiogramToEq.map(results)
                onDone(eq)
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("I heard it")
        }

        Button(onClick = {
            results[freqs[index]] = false
            if (index < freqs.size - 1) index += 1 else {
                val eq = AudiogramToEq.map(results)
                onDone(eq)
            }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("I did not hear it")
        }

        Text("Progress: ${index + 1}/${freqs.size}", modifier = Modifier.padding(top = 12.dp))
    }
}

private fun playTone(freqHz: Float, onComplete: () -> Unit) {
    Thread {
        val sampleRate = 44100
        val durationSec = 1.0
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val max = Short.MAX_VALUE
        for (i in 0 until numSamples) {
            val sample = (max * sin(2.0 * PI * i * freqHz / sampleRate)).toInt().toShort()
            buffer[i] = sample
        }
        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STATIC)
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep((durationSec * 1000L) + 50)
        track.stop()
        track.release()
        onComplete()
    }.start()
}
