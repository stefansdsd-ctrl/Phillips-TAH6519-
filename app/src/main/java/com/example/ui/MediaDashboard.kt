package com.example.ui
import androidx.compose.foundation.BorderStroke

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*

@Composable
fun FullScreenMediaDashboard(viewModel: HeadphoneViewModel, settings: HeadphoneSettings) {
    val isPlaying by viewModel.mediaIsPlaying.collectAsStateWithLifecycle()
    val trackProgressSecs by viewModel.mediaProgress.collectAsStateWithLifecycle()
    val totalDurationSecs by viewModel.mediaDuration.collectAsStateWithLifecycle()
    val trackName by viewModel.mediaTrackName.collectAsStateWithLifecycle()
    val trackArtist by viewModel.mediaTrackArtist.collectAsStateWithLifecycle()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()
    
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "album_scale"
    )

    val progressFraction = if (totalDurationSecs > 0) trackProgressSecs.toFloat() / totalDurationSecs.toFloat() else 0f

    val formatTime = { secs: Int ->
        val m = secs / 60
        val s = secs % 60
        String.format("%d:%02d", m, s)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkPanel)
                .border(1.dp, DarkBorder, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.album_cover_synth_1783687331450),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(if (isPlaying) 1.0f else 0.8f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = trackName,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = trackArtist,
                color = HighlightSky,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progressFraction,
                onValueChange = { newVal ->
                    val newSecs = (newVal * totalDurationSecs).toInt()
                    viewModel.seekMedia(newSecs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = HighlightSky,
                    inactiveTrackColor = DarkBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(trackProgressSecs), color = TextMuted, fontSize = 12.sp)
                Text(text = formatTime(totalDurationSecs), color = TextMuted, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.playPreviousTrack() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary)
                    .clickable { viewModel.toggleMediaPlayer() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            IconButton(
                onClick = { viewModel.playNextTrack() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(imageVector = Icons.Default.FastForward, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Playlist Section
        Text(
            text = "App-Only Playlist",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.playlist.forEachIndexed { index, track ->
                val isSelected = index == currentTrackIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTrack(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) HighlightSky.copy(alpha = 0.15f) else DarkCard
                    ),
                    border = BorderStroke(1.dp, if (isSelected) HighlightSky.copy(alpha = 0.5f) else DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) HighlightSky else DarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected && isPlaying) {
                                Icon(Icons.Default.Pause, contentDescription = "Playing", tint = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = if (isSelected) Color.White else TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isSelected) HighlightSky else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = track.artist,
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (track.isOffline) {
                            Icon(Icons.Default.DownloadDone, contentDescription = "Offline Available", tint = StatusSuccess, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.CloudQueue, contentDescription = "Stream", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleSurround(!settings.surroundSoundEnabled) },
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (settings.surroundSoundEnabled) HighlightSky.copy(alpha = 0.5f) else DarkBorder)
        ) {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.spatial_audio_banner_1783687350013),
                    contentDescription = "Spatial Audio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (settings.surroundSoundEnabled) 1.0f else 0.4f)
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBg.copy(alpha = 0.95f))))
                )
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("3D Spatial Audio", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Immersive surround sound", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.surroundSoundEnabled,
                        onCheckedChange = { viewModel.toggleSurround(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighlightSky,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        )
                    )
                }
            }
        }
    }
}
