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
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }
                webChromeClient = WebChromeClient()
            }
        },
        update = { webView ->
            val tagKey = "$youtubeId-$isPlaying"
            val currentTag = webView.tag as? String
            
            if (currentTag != tagKey) {
                webView.tag = tagKey
                val currentVideoId = currentTag?.split(":")?.firstOrNull()
                
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
                                iframe { width:100%; height:100%; border:0; }
                            </style>
                        </head>
                        <body>
                            <iframe id="ytplayer" 
                                    type="text/html"
                                    src="https://www.youtube.com/embed/$youtubeId?enablejsapi=1&autoplay=$autoplayParam&playsinline=1&controls=1&rel=0&modestbranding=1" 
                                    allow="autoplay; encrypted-media; picture-in-picture" 
                                    allowfullscreen>
                            </iframe>
                            <script>
                                var playerFrame = document.getElementById('ytplayer');
                                function sendCommand(func) {
                                    if (playerFrame && playerFrame.contentWindow) {
                                        playerFrame.contentWindow.postMessage(JSON.stringify({
                                            'event': 'command',
                                            'func': func,
                                            'args': []
                                        }), '*');
                                    }
                                }
                                function play() { sendCommand('playVideo'); }
                                function pause() { sendCommand('pauseVideo'); }
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
        }
    )
}


