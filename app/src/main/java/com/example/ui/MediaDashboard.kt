package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.HeadphoneSettings
import com.example.ui.theme.*
import com.example.PremiumSlider
import com.example.PhilipsPremiumSwitch
import coil.compose.AsyncImage

@Composable
fun FullScreenMediaDashboard(viewModel: HeadphoneViewModel, settings: HeadphoneSettings) {
    val isPlaying by viewModel.mediaIsPlaying.collectAsStateWithLifecycle()
    val trackProgressSecs by viewModel.mediaProgress.collectAsStateWithLifecycle()
    val totalDurationSecs by viewModel.mediaDuration.collectAsStateWithLifecycle()
    val trackName by viewModel.mediaTrackName.collectAsStateWithLifecycle()
    val trackArtist by viewModel.mediaTrackArtist.collectAsStateWithLifecycle()
    val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()
    
    val activeAudioMood by viewModel.activeAudioMood.collectAsStateWithLifecycle()
    val audioMoodVolume by viewModel.audioMoodVolume.collectAsStateWithLifecycle()
    
    // YouTube Music specific states
    val isYoutubeActive by viewModel.isYoutubeActive.collectAsStateWithLifecycle()
    val youtubePlaylistTracks by viewModel.youtubePlaylistTracks.collectAsStateWithLifecycle()
    val youtubePlaylistName by viewModel.youtubePlaylistName.collectAsStateWithLifecycle()
    val youtubeAccountConnected by viewModel.youtubeAccountConnected.collectAsStateWithLifecycle()
    val youtubeAccountName by viewModel.youtubeAccountName.collectAsStateWithLifecycle()
    val lastYoutubePlaylistUrl by viewModel.lastYoutubePlaylistUrl.collectAsStateWithLifecycle()
    val youtubeLastSyncedTime by viewModel.youtubeLastSyncedTime.collectAsStateWithLifecycle()

    val sleepTimerTotalMin by viewModel.sleepTimerTotalMin.collectAsStateWithLifecycle()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()
    val sleepTimerRunning by viewModel.sleepTimerRunning.collectAsStateWithLifecycle()
    val sleepTimerAction by viewModel.sleepTimerAction.collectAsStateWithLifecycle()

    val mediaQueue by viewModel.exoPlayerController.queue.collectAsStateWithLifecycle()
    val mediaQueueIndex by viewModel.exoPlayerController.currentIndex.collectAsStateWithLifecycle()
    val isMediaSessionActive by viewModel.exoPlayerController.isMedia3SessionActive.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("QUEUE") }
    var playlistInputUrl by remember { mutableStateOf("") }
    val isYoutubeImporting by viewModel.isYoutubeImporting.collectAsStateWithLifecycle()
    val youtubeImportMessage by viewModel.youtubeImportMessage.collectAsStateWithLifecycle()

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

    // YouTube Music Stream Sync Animations
    val ytSyncTransition = rememberInfiniteTransition(label = "yt_sync_animation")
    val ytPulseRadius by ytSyncTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yt_pulse_radius"
    )
    val ytPulseAlpha by ytSyncTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yt_pulse_alpha"
    )
    val ytPulseRadius2 by ytSyncTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yt_pulse_radius2"
    )
    val ytPulseAlpha2 by ytSyncTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yt_pulse_alpha2"
    )
    val ytWaveGlow by ytSyncTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yt_wave_glow"
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
        // 1. ALBUM ART COVER (Dynamic: standard cover, YouTube thumbnail, or Atmospheric background with spinning disc!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkPanel)
                .border(1.dp, if (activeAudioMood != "NONE") HighlightSky.copy(alpha = 0.6f) else if (isYoutubeActive) Color(0xFFFF0000).copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (activeAudioMood != "NONE") {
                val moodResId = when (activeAudioMood) {
                    "RAINFOREST" -> R.drawable.img_mood_rainforest_1784335733389
                    "OCEAN" -> R.drawable.img_mood_ocean_1784335744083
                    "CAFE" -> R.drawable.img_mood_cafe_1784335755316
                    "FIREPLACE" -> R.drawable.img_mood_fireplace_1784335766118
                    else -> R.drawable.album_cover_synth_1783687331450
                }
                
                // 1a. The atmospheric background image generated by AI
                Image(
                    painter = painterResource(id = moodResId),
                    contentDescription = "Atmospheric background: $activeAudioMood",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.65f)
                )
                
                // Immersive dark overlay gradient to blend the atmosphere beautifully
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // 1b. Rotating Vinyl / CD of the current track at the center!
                if (isYoutubeActive && youtubePlaylistTracks.isNotEmpty() && currentTrackIndex in youtubePlaylistTracks.indices) {
                    val activeTrack = youtubePlaylistTracks[currentTrackIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(175.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFFF0000).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        YouTubePlayer(
                            youtubeId = activeTrack.youtubeId,
                            isPlaying = isPlaying,
                            progressSecs = trackProgressSecs
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .graphicsLayer {
                                    rotationZ = if (isPlaying) rotationAngle else 0f
                                }
                                .background(Color(0xFF0F1424), CircleShape)
                                .border(6.dp, Color(0xFF070B14), CircleShape)
                                .border(8.dp, HighlightSky.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vinyl Grooves effect (concentric circles)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.9f)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                    .border(5.dp, Color.Transparent, CircleShape)
                                    .border(6.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(15.dp, Color.Transparent, CircleShape)
                                    .border(16.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                            )
                            
                            // Album Artwork at the center of the Vinyl
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(CircleShape)
                                    .background(DarkBg)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.album_cover_synth_1783687331450),
                                    contentDescription = "Track Art Center",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // Vinyl spindle hole at center
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFF070B14), CircleShape)
                                    .border(1.5.dp, HighlightSky.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }
                
                // Floating indicator showing active atmosphere
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .border(1.dp, HighlightSky.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Animated pulsing audio visualizer bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(10.dp)
                        ) {
                            val pulseTransition = rememberInfiniteTransition(label = "pulse")
                            for (b in 1..4) {
                                val heightVal by pulseTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 250 + b * 100, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "bar_height"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight(if (isPlaying) heightVal else 0.2f)
                                        .background(HighlightSky, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                        Text(
                            text = when(activeAudioMood) {
                                "RAINFOREST" -> "Regenwoud"
                                "OCEAN" -> "Oceaan"
                                "CAFE" -> "Café"
                                "FIREPLACE" -> "Haardvuur"
                                else -> ""
                            },
                            color = HighlightSky,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // YouTube Music / Media3 ExoPlayer Session Floating Pill Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (isYoutubeActive) Color(0xFFFF0000).copy(alpha = 0.6f) else HighlightSky.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isYoutubeActive) Color(0xFFFF0000) else HighlightSky, CircleShape)
                            )
                            Text(
                                text = if (isYoutubeActive) "YT MUSIC STREAM" else "STUDIO PLAYER",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0D182E).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighlightSky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MEDIA3 EXOPLAYER SESSION",
                            color = HighlightSky,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else if (isYoutubeActive && youtubePlaylistTracks.isNotEmpty() && currentTrackIndex in youtubePlaylistTracks.indices) {
                val activeTrack = youtubePlaylistTracks[currentTrackIndex]
                AsyncImage(
                    model = "https://img.youtube.com/vi/${activeTrack.youtubeId}/hqdefault.jpg",
                    contentDescription = "YouTube Track Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (isPlaying) 1.0f else 0.8f)
                )
                // YouTube Music Premium Floating Pill Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFFF0000), CircleShape)
                        )
                        Text(
                            text = "YT MUSIC ACTIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.album_cover_synth_1783687331450),
                    contentDescription = "Standard Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (isPlaying) 1.0f else 0.8f)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "STANDARD PLAYER",
                        color = HighlightSky,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 2. METADATA (TRACK NAME & ARTIST)
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
                color = if (isYoutubeActive) Color(0xFFFF4D4D) else HighlightSky,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // 3. PROGRESS BAR & TIMERS
        Column(modifier = Modifier.fillMaxWidth()) {
            PremiumSlider(
                value = progressFraction,
                onValueChange = { newVal ->
                    val newSecs = (newVal * totalDurationSecs).toInt()
                    viewModel.seekMedia(newSecs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = if (isYoutubeActive) Color(0xFFFF0000) else HighlightSky,
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

        // 4. PLAYER CONTROL BUTTONS WITH YOUTUBE STREAM SYNC ANIMATION
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.playPreviousTrack() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Vorige",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play/Pause Button with Youtube Stream Sync Aura Rings
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (isYoutubeActive && isPlaying) {
                        // Outer Pulsing Aura Ring 2
                        Box(
                            modifier = Modifier
                                .size(80.dp * ytPulseRadius2)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000).copy(alpha = ytPulseAlpha2))
                        )
                        // Outer Pulsing Aura Ring 1
                        Box(
                            modifier = Modifier
                                .size(80.dp * ytPulseRadius)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000).copy(alpha = ytPulseAlpha))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isYoutubeActive) {
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF3333), Color(0xFFFF0000), Color(0xFFCC0000))
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(HighlightSky, AccentPrimary)
                                    )
                                }
                            )
                            .border(
                                width = if (isYoutubeActive && isPlaying) 2.dp else 1.dp,
                                color = if (isYoutubeActive && isPlaying) Color.White.copy(alpha = ytWaveGlow) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.toggleMediaPlayer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pauze" else "Afspelen",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.playNextTrack() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Volgende",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Live YouTube Sync Stream Indicator Bar
            if (isYoutubeActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color(0xFFFF0000).copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(12.dp)
                    ) {
                        for (i in 1..5) {
                            val barH = if (isPlaying) {
                                when (i) {
                                    1 -> ytWaveGlow * 0.8f
                                    2 -> (1f - ytWaveGlow) * 0.9f
                                    3 -> ytWaveGlow
                                    4 -> (1f - ytWaveGlow) * 0.7f
                                    else -> ytWaveGlow * 0.9f
                                }
                            } else 0.2f
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight(barH)
                                    .background(Color(0xFFFF0000), RoundedCornerShape(1.5.dp))
                            )
                        }
                    }
                    Text(
                        text = if (isPlaying) "YOUTUBE MUSIC STREAM SYNCED • LIVE" else "YOUTUBE MUSIC PAUSED",
                        color = Color(0xFFFF4D4D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                YouTubeMusicVisualizer(
                    viewModel = viewModel,
                    settings = settings
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. THEMED SOURCE TABS (SWITCHER BETWEEN QUEUE, YOUTUBE & APP PLAYLIST)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "QUEUE" to "Wachtrij",
                "YOUTUBE" to "YouTube Music",
                "APP" to "App Playlist"
            ).forEach { (tab, label) ->
                val selected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) {
                                when (tab) {
                                    "YOUTUBE" -> Color(0xFFFF0000).copy(alpha = 0.18f)
                                    "QUEUE" -> Color(0xFF0066FF).copy(alpha = 0.18f)
                                    else -> HighlightSky.copy(alpha = 0.18f)
                                }
                            } else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) {
                                when (tab) {
                                    "YOUTUBE" -> Color(0xFFFF0000)
                                    "QUEUE" -> Color(0xFF0066FF)
                                    else -> HighlightSky
                                }
                            } else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { 
                            activeTab = tab
                            if (tab == "YOUTUBE") {
                                viewModel.setYoutubeActive(true)
                            } else if (tab == "APP") {
                                viewModel.playTrack(0)
                            }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("tab_button_$tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (tab) {
                                        "YOUTUBE" -> Color(0xFFFF0000)
                                        "QUEUE" -> Color(0xFF0066FF)
                                        else -> HighlightSky
                                    },
                                    CircleShape
                                )
                        )
                        Text(
                            text = label,
                            color = if (selected) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 6. DETAILED VIEW DEPENDING ON ACTIVE SOURCE TAB
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing))
            },
            label = "media_subtab_crossfade"
        ) { targetSubTab ->
            if (targetSubTab == "QUEUE") {
            // ==========================================
            // NOW PLAYING MEDIA3 QUEUE MODE
            // ==========================================
            NowPlayingQueueView(
                queue = mediaQueue,
                currentIndex = mediaQueueIndex,
                isPlaying = isPlaying,
                isMediaSessionActive = isMediaSessionActive,
                onSkipToTrack = { idx ->
                    viewModel.exoPlayerController.skipToQueueItem(idx)
                    viewModel.currentTrackIndex.value = idx
                },
                onRemoveTrack = { idx ->
                    viewModel.exoPlayerController.removeQueueItem(idx)
                },
                onClearQueue = {
                    viewModel.exoPlayerController.clearQueue()
                },
                onLoadYoutubeQueue = {
                    viewModel.setYoutubeActive(true)
                },
                onLoadAppQueue = {
                    viewModel.playTrack(0)
                }
            )
        } else if (activeTab == "YOUTUBE") {
            // ==========================================
            // YOUTUBE MUSIC PLAYLIST DASHBOARD MODE
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Atmosferische Sfeer & Soundscapes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, if (activeAudioMood != "NONE") HighlightSky.copy(alpha = 0.4f) else DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Atmosferische Sfeer",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Verrijk YT Music met AI-omgevingsgeluiden",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            
                            if (activeAudioMood != "NONE") {
                                Box(
                                    modifier = Modifier
                                        .background(HighlightSky.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "GEMIXT",
                                        color = HighlightSky,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Grid / Row of Mood Options with our custom generated backgrounds!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val moods = listOf(
                                Triple("NONE", "Uit", Icons.Default.MusicOff),
                                Triple("RAINFOREST", "Bos", R.drawable.img_mood_rainforest_1784335733389),
                                Triple("OCEAN", "Zee", R.drawable.img_mood_ocean_1784335744083),
                                Triple("CAFE", "Café", R.drawable.img_mood_cafe_1784335755316),
                                Triple("FIREPLACE", "Haard", R.drawable.img_mood_fireplace_1784335766118)
                            )

                            moods.forEach { (mood, label, asset) ->
                                val isSelected = activeAudioMood == mood
                                val scaleAnimate by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1.0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
                                    label = "mood_scale_select"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .scale(scaleAnimate)
                                        .clickable {
                                            viewModel.setActiveAudioMood(mood)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1.2f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkBg)
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) HighlightSky else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (asset is Int) {
                                            Image(
                                                painter = painterResource(id = asset),
                                                contentDescription = label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().alpha(if (isSelected) 1.0f else 0.5f)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(DarkBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = asset as androidx.compose.ui.graphics.vector.ImageVector,
                                                    contentDescription = label,
                                                    tint = if (isSelected) HighlightSky else TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        if (isSelected && mood != "NONE") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.25f))
                                            )
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Actief",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isSelected) HighlightSky else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Ambient mixer volume slider
                        if (activeAudioMood != "NONE") {
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Mix Volume",
                                        tint = HighlightSky,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Mix Volume Sfeer: ${(audioMoodVolume * 100).toInt()}%",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                val textMoodPlayState = if (isPlaying) "Sfeer actief" else "Muziek gepauzeerd"
                                Text(
                                    text = textMoodPlayState,
                                    color = if (isPlaying) StatusSuccess else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            PremiumSlider(
                                value = audioMoodVolume,
                                onValueChange = { viewModel.setAudioMoodVolume(it) },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = HighlightSky,
                                    inactiveTrackColor = DarkBg
                                ),
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                            )
                        }
                    }
                }

                // Google YT Music Link status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, if (youtubeAccountConnected) Color(0xFFFF0000).copy(alpha = 0.2f) else DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (youtubeAccountConnected) Color(0xFFFF0000) else DarkBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Account icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (youtubeAccountConnected) youtubeAccountName else "Account losgekoppeld",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(if (youtubeAccountConnected) Color(0xFFFFD700) else TextMuted, CircleShape))
                                    Text(
                                        text = if (youtubeAccountConnected) "YouTube Premium Gekoppeld" else "Speel gratis nummers af",
                                        color = if (youtubeAccountConnected) Color(0xFFFFD700) else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleYoutubeAccount() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (youtubeAccountConnected) DarkBg else Color(0xFFFF0000)
                            ),
                            border = BorderStroke(1.dp, if (youtubeAccountConnected) DarkBorder else Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (youtubeAccountConnected) "Ontkoppel" else "Koppel",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Playlist Importer Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Zoek op YouTube Music of Plak Playlist/Video URL",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = playlistInputUrl,
                                onValueChange = { playlistInputUrl = it },
                                placeholder = { Text("Typ een nummer/artiest of plak URL...", fontSize = 11.sp, color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkBg,
                                    unfocusedContainerColor = DarkBg,
                                    focusedBorderColor = Color(0xFFFF0000),
                                    unfocusedBorderColor = DarkBorder
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )

                            Button(
                                onClick = {
                                    if (playlistInputUrl.isNotBlank() && !isYoutubeImporting) {
                                        viewModel.importYoutubePlaylist(playlistInputUrl)
                                        playlistInputUrl = ""
                                    }
                                },
                                enabled = !isYoutubeImporting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0000),
                                    disabledContainerColor = Color(0xFF660000)
                                ),
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isYoutubeImporting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Importeer", tint = Color.White)
                                }
                            }
                        }

                        if (isYoutubeImporting && youtubeImportMessage.isNotEmpty()) {
                            Text(
                                text = youtubeImportMessage,
                                color = Color(0xFFFF0000),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }

                        // Presets Quickloaders for fast evaluation
                        Text(
                            text = "Snelkoppelingen van populaire playlists:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Chill Lofi ☕" to "https://music.youtube.com/playlist?list=PL_CHILL_LOFI_BEATS",
                                "Workout 🏃‍♂️" to "https://music.youtube.com/playlist?list=PL_WORKOUT_ENERGY_BEATS",
                                "Pop Hits 🔥" to "https://music.youtube.com/playlist?list=PL_POP_TOP_HITS"
                            ).forEach { (label, url) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(DarkBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.importYoutubePlaylist(url)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Active YT Playlist Name & Tracks List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF0000), CircleShape))
                            Text(
                                text = youtubePlaylistName,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Geïmporteerde YouTube tracks • ${youtubePlaylistTracks.size} nummers",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(StatusSuccess.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "💾 Room DB Offline",
                                    color = StatusSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (youtubeLastSyncedTime.isNotBlank()) {
                            Text(
                                text = "Laatst gesynchroniseerd: $youtubeLastSyncedTime",
                                color = Color(0xFFFF4D4D).copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Rotating/animated or simple Sync Button
                    Button(
                        onClick = { viewModel.importYoutubePlaylist("") },
                        enabled = !isYoutubeImporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF0000),
                            disabledContainerColor = DarkBorder
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isYoutubeImporting) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFF0000),
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp
                                )
                                Text("Synchroniseren...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Synchroniseren",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    youtubePlaylistTracks.forEachIndexed { index, track ->
                        val isSelected = isYoutubeActive && index == currentTrackIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playYoutubeTrack(index) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFF0000).copy(alpha = 0.12f) else DarkCard
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF0000).copy(alpha = 0.5f) else DarkBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rounded dynamic thumbnail artwork
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://img.youtube.com/vi/${track.youtubeId}/hqdefault.jpg",
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPlaying) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                                    contentDescription = "Nu afspelen",
                                                    tint = Color(0xFFFF0000),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Gepauzeerd",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = if (isSelected) Color(0xFFFF4D4D) else TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = track.artist,
                                        color = TextMuted,
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatTime(track.durationSecs),
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )

                                    Icon(
                                        imageVector = if (track.isOffline) Icons.Default.DownloadDone else Icons.Default.ArrowDownward,
                                        contentDescription = "Download status",
                                        tint = if (track.isOffline) StatusSuccess else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // STANDAARD APP-PLAYLIST MODE
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        val isSelected = !isYoutubeActive && index == currentTrackIndex
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
            }
        }
        }

        // 9. ANC CONTROL CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (settings.ancEnabled) HighlightSky.copy(alpha = 0.6f) else DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Ruisonderdrukking",
                            tint = if (settings.ancMode != "OFF") HighlightSky else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Actieve Ruisonderdrukking",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (settings.ancMode) {
                                    "ON" -> "Smart ANC Actief"
                                    "TRANSPARENCY" -> "Awareness Mode Actief"
                                    else -> "Ruisonderdrukking Uit"
                                },
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    PhilipsPremiumSwitch(
                        checked = settings.ancMode != "OFF",
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                viewModel.toggleAnc(true)
                                viewModel.setAncMode("ON")
                            } else {
                                viewModel.toggleAnc(false)
                                viewModel.setAncMode("OFF")
                            }
                        },
                        modifier = Modifier.testTag("anc_mediadashboard_switch"),
                        activeColor = HighlightSky
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, shape = RoundedCornerShape(24.dp))
                        .border(1.dp, DarkBorder, shape = RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        Triple("ON", "ANC On", Icons.Default.CheckCircle),
                        Triple("TRANSPARENCY", "Awareness Mode", Icons.Default.Hearing),
                        Triple("OFF", "ANC Off", Icons.Default.RadioButtonUnchecked)
                    )

                    modes.forEach { (mode, label, icon) ->
                        val isSelected = settings.ancMode == mode
                        val bgAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) (if (mode == "ON") AccentPrimary else if (mode == "TRANSPARENCY") HighlightSky else DarkBorder) else Color.Transparent,
                            animationSpec = tween(durationMillis = 250),
                            label = "anc_dash_bg"
                        )
                        val tintAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else TextMuted,
                            animationSpec = tween(durationMillis = 250),
                            label = "anc_dash_tint"
                        )
                        val textAnimateColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else TextPrimary,
                            animationSpec = tween(durationMillis = 250),
                            label = "anc_dash_text"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    color = bgAnimateColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { 
                                    viewModel.setAncMode(mode) 
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = tintAnimateColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = textAnimateColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 8. SLEEP TIMER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (sleepTimerRunning) HighlightSky.copy(alpha = 0.6f) else DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with bedtime/access-time icon and active badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Slaaptimer",
                            tint = if (sleepTimerRunning) HighlightSky else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Slaaptimer",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (sleepTimerRunning) "Timer is actief" else "Schakel automatisch uit",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Active Countdown Badge
                    if (sleepTimerRunning) {
                        val minutesLeft = sleepTimerRemainingSec / 60
                        val secondsLeft = sleepTimerRemainingSec % 60
                        val countdownStr = String.format("%02d:%02d", minutesLeft, secondsLeft)
                        Box(
                            modifier = Modifier
                                .background(HighlightSky.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, HighlightSky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = countdownStr,
                                color = HighlightSky,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(DarkBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "UIT",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Preset Buttons Row
                var customMinutes by remember { mutableStateOf(30) }
                
                if (!sleepTimerRunning) {
                    Text(
                        text = "Kies een snelle timer of stel zelf in:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30, 45, 60).forEach { mins ->
                            val isSelected = customMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) HighlightSky.copy(alpha = 0.15f) else DarkBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) HighlightSky else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        customMinutes = mins
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins} m",
                                    color = if (isSelected) TextPrimary else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Custom slider if they want to adjust minutes manually
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aangepaste duur: ${customMinutes} minuten",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        PremiumSlider(
                            value = customMinutes.toFloat(),
                            onValueChange = { customMinutes = it.toInt() },
                            valueRange = 1f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = HighlightSky,
                                inactiveTrackColor = DarkBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Sleep Timer Action Selector (Pause Playback or Close App)
                    Text(
                        text = "Actie na afloop van de timer:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PAUSE" to "Pauzeer muziek", "CLOSE" to "Sluit applicatie").forEach { (action, label) ->
                            val isSelected = sleepTimerAction == action
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) HighlightSky.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) HighlightSky else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setSleepTimerAction(action)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TextPrimary else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Running state display details
                    val actionLabel = if (sleepTimerAction == "PAUSE") "pauzeren van muziek" else "sluiten van de app"
                    Text(
                        text = "De timer is ingesteld op ${sleepTimerTotalMin} minuten. Na afloop zal de speler automatisch ${actionLabel}.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Control Buttons: Start / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sleepTimerRunning) {
                        Button(
                            onClick = { viewModel.stopSleepTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                Text("Annuleer Slaaptimer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startSleepTimer(customMinutes) },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                                Text("Start Slaaptimer", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. SPATIAL SURROUND AUDIO CARD
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

        Spacer(modifier = Modifier.height(16.dp))

        // 8. AMBIENT PARTICLE VISUALIZER CARD (Integrated from AmbientVisualizer.kt)
        var showAmbientVisualizer by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, if (showAmbientVisualizer) AccentPrimary.copy(alpha = 0.6f) else DarkBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentPrimary.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, AccentPrimary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Ambient Visualizer",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sfeer & Deeltjes Visualizer",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "3D deeltjes & frequentiespectrum visualizer",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = showAmbientVisualizer,
                        onCheckedChange = { showAmbientVisualizer = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        )
                    )
                }

                if (showAmbientVisualizer) {
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                    FullScreenAmbientVisualizer(viewModel = viewModel, settings = settings)
                }
            }
        }
    }
}

@Composable
fun NowPlayingQueueView(
    queue: List<com.example.media.MediaQueueItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    isMediaSessionActive: Boolean,
    onSkipToTrack: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onLoadYoutubeQueue: () -> Unit,
    onLoadAppQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("now_playing_queue_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Queue Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, HighlightSky.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HighlightSky.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Now Playing Queue",
                                tint = HighlightSky,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "NOW PLAYING WACHTRIJ",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isMediaSessionActive) StatusSuccess else Color.Yellow, CircleShape)
                                )
                                Text(
                                    text = if (isMediaSessionActive) "MediaSession Actief • ${queue.size} nummers" else "MediaSession Offline",
                                    color = if (isMediaSessionActive) StatusSuccess else Color.Yellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (queue.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearQueue,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4D)),
                            border = BorderStroke(1.dp, Color(0xFFFF4D4D).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("clear_queue_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Wis wachtrij", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (queue.isNotEmpty()) {
                    val totalSecs = queue.sumOf { it.durationSecs }
                    val totalMins = totalSecs / 60
                    val activeNum = (currentIndex + 1).coerceIn(1, queue.size)
                    Text(
                        text = "Nummer $activeNum van ${queue.size} wordt afgespeeld • Totale duur: ~$totalMins min",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (queue.isEmpty()) {
            // Empty State Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Empty Queue",
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Geen nummers in de wachtrij",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Laad nummers uit de App Playlist of YouTube Music om de actieve MediaSession te vullen.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onLoadAppQueue,
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSky),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("load_app_queue_button")
                        ) {
                            Text("Laad App Playlist", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onLoadYoutubeQueue,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("load_yt_queue_button")
                        ) {
                            Text("Laad YT Music", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // List-Based Navigation for MediaQueue
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                queue.forEachIndexed { index, item ->
                    val isCurrent = index == currentIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSkipToTrack(index) }
                            .testTag("queue_item_$index"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                if (item.isYoutube) Color(0xFFFF0000).copy(alpha = 0.15f) else HighlightSky.copy(alpha = 0.15f)
                            } else DarkCard
                        ),
                        border = BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) {
                                if (item.isYoutube) Color(0xFFFF0000) else HighlightSky
                            } else DarkBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Index / Soundwave Badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrent) {
                                            if (item.isYoutube) Color(0xFFFF0000) else HighlightSky
                                        } else DarkBg
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrent && isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Playing Now",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "#${index + 1}",
                                        color = if (isCurrent) Color.White else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Thumbnail or Icon
                            if (item.isYoutube && !item.youtubeId.isNullOrBlank()) {
                                AsyncImage(
                                    model = "https://img.youtube.com/vi/${item.youtubeId}/hqdefault.jpg",
                                    contentDescription = "Queue thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            // Title & Artist
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = if (isCurrent) {
                                        if (item.isYoutube) Color(0xFFFF4D4D) else HighlightSky
                                    } else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.artist,
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (item.isYoutube) Color(0xFFFF0000).copy(alpha = 0.2f) else DarkBg,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (item.isYoutube) "YT MUSIC" else "STUDIO",
                                            color = if (item.isYoutube) Color(0xFFFF4D4D) else HighlightSky,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Duration & Delete Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val m = item.durationSecs / 60
                                val s = item.durationSecs % 60
                                Text(
                                    text = String.format("%d:%02d", m, s),
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                IconButton(
                                    onClick = { onRemoveTrack(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Verwijder uit wachtrij",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
