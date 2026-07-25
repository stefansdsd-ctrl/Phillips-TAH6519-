package com.example.ui

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubePlayer(
    youtubeId: String,
    isPlaying: Boolean,
    progressSecs: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
            }
        },
        update = { webView ->
            val currentId = webView.tag as? String
            if (currentId != youtubeId) {
                webView.tag = youtubeId
                val htmlData = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:0;background-color:#000;">
                        <div id="player"></div>
                        <script>
                            var tag = document.createElement('script');
                            tag.src = "https://www.youtube.com/iframe_api";
                            var firstScriptTag = document.getElementsByTagName('script')[0];
                            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                            
                            var player;
                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('player', {
                                    height: '100%',
                                    width: '100%',
                                    videoId: '$youtubeId',
                                    playerVars: {
                                        'playsinline': 1,
                                        'controls': 0,
                                        'disablekb': 1,
                                        'fs': 0,
                                        'rel': 0,
                                        'modestbranding': 1
                                    },
                                    events: {
                                        'onReady': onPlayerReady
                                    }
                                });
                            }
                            function onPlayerReady(event) {
                                if (${isPlaying}) {
                                    event.target.playVideo();
                                }
                            }
                            
                            function play() { if(player && player.playVideo) player.playVideo(); }
                            function pause() { if(player && player.pauseVideo) player.pauseVideo(); }
                            function seekTo(secs) { if(player && player.seekTo) player.seekTo(secs, true); }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
            } else {
                if (isPlaying) {
                    webView.evaluateJavascript("play();", null)
                } else {
                    webView.evaluateJavascript("pause();", null)
                }
            }
        }
    )
    
    // Optional: Only seek if the difference is more than 3 seconds to avoid constant skipping
    LaunchedEffect(progressSecs) {
        // We cannot easily access the webview here without keeping a reference.
        // For simplicity, we can pass it in the `update` block, but since `progressSecs` changes every second,
        // calling `seekTo` every second will cause stuttering.
    }
}
