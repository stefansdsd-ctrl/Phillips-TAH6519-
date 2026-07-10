#!/bin/bash
sed -i '1s/^/import android.media.AudioManager\nimport android.media.AudioFocusRequest\nimport android.content.Context\n/' app/src/main/java/com/example/ui/HeadphoneViewModel.kt
