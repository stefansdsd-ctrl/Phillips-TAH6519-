package com.example.ui

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubePlayer(
    youtubeId: String,
    isPlaying: Boolean,
    progressSecs: Int,
    onVideoEnded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                
                addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onVideoEnded() {
                        post { onVideoEnded() }
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                webChromeClient = WebChromeClient()
            }
        },
        update = { webView ->
            if (webView.tag == null) {
                webView.tag = ""
            }
            val currentTag = webView.tag as? String ?: ""
            val parts = currentTag.split(":")
            val currentVideoId = parts.getOrNull(0) ?: ""
            val currentPlayingState = parts.getOrNull(1) == "play"
            
            if (currentVideoId != youtubeId) {
                webView.tag = "$youtubeId:${if (isPlaying) "play" else "pause"}"
                val autoplayParam = if (isPlaying) 1 else 0
                val htmlData = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            body, html { margin:0; padding:0; width:100%; height:100%; background-color:#000; overflow:hidden; }
                            #ytplayer { width:100%; height:100%; border:0; }
                        </style>
                    </head>
                    <body>
                        <div id="ytplayer"></div>
                        <script>
                            var tag = document.createElement('script');
                            tag.src = "https://www.youtube.com/iframe_api";
                            var firstScriptTag = document.getElementsByTagName('script')[0];
                            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                            var player;
                            function onYouTubeIframeAPIReady() {
                                player = new YT.Player('ytplayer', {
                                    height: '100%',
                                    width: '100%',
                                    videoId: '$youtubeId',
                                    playerVars: {
                                        'playsinline': 1,
                                        'autoplay': $autoplayParam,
                                        'controls': 1,
                                        'rel': 0,
                                        'modestbranding': 1
                                    },
                                    events: {
                                        'onStateChange': function(event) {
                                            if (event.data == YT.PlayerState.ENDED) {
                                                AndroidBridge.onVideoEnded();
                                            }
                                        }
                                    }
                                });
                            }

                            function play() { if (player && typeof player.playVideo === 'function') player.playVideo(); }
                            function pause() { if (player && typeof player.pauseVideo === 'function') player.pauseVideo(); }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null)
            } else if (currentPlayingState != isPlaying) {
                webView.tag = "$youtubeId:${if (isPlaying) "play" else "pause"}"
                if (isPlaying) {
                    webView.evaluateJavascript("play();", null)
                } else {
                    webView.evaluateJavascript("pause();", null)
                }
            }
        }
    )
}
