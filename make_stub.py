import re

java_code = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.java").read()

props = []
methods = []
seen_methods = set()

# extract fields
for line in java_code.split("\n"):
    line = line.strip()
    m = re.match(r"private final StateFlow<(.+?)> (.+?);", line)
    if m:
        t, name = m.groups()
        t = t.replace("java.lang.", "").replace("Integer", "Int").replace("Boolean", "Boolean").replace("Float", "Float")
        props.append((name, t))

for line in java_code.split("\n"):
    line = line.strip()
    m = re.match(r"public final void ([a-zA-Z0-9_]+)\(", line)
    if m:
        name = m.group(1)
        if name not in seen_methods and not name.startswith("get") and not name.startswith("set") and name != "onCleared":
            seen_methods.add(name)
            methods.append(name)

with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
    f.write("""package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.HeadphoneSettings

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpdateAvailable : UpdateState()
    object Updating : UpdateState()
    object UpToDate : UpdateState()
    object UpdateComplete : UpdateState()
}

data class ScannedDevice(val name: String, val address: String, val rssi: Int = 0)

class HeadphoneViewModel(application: Application) : AndroidViewModel(application) {
""")
    for name, t in props:
        t2 = t.replace("List<ScannedDevice>", "List<ScannedDevice>").replace("Map<String, List<Float>>", "Map<String, List<Float>>")
        f.write(f"    private val _{name} = MutableStateFlow<{t2}>(null as {t2})\n")
        f.write(f"    val {name}: StateFlow<{t2}> = _{name}.asStateFlow()\n\n")

    for m in methods:
        f.write(f"    fun {m}(vararg args: Any?) {{}}\n")
        
    # properties like "setPreset", "setAncMode", etc. because CFR might have missed them
    # wait, setAncMode etc. might be methods
    for m in ["setAncLevel", "setAncMode", "setAutoPowerOffMinutes", "setPreset", "setSimulatedActivity", "setSoundZone", "updateBand", "updateBatteryLevel", "updateMasterGain", "updateMultipointDevices", "toggleTrackOfflineStatus"]:
        if m not in seen_methods:
            f.write(f"    fun {m}(vararg args: Any?) {{}}\n")
            
    # Add playlist tracks
    f.write("""
    data class Track(val id: String, val title: String, val artist: String, val sourceUrl: String, val isOffline: Boolean)
    val playlist = listOf(
        Track("1", "Let It Happen", "Tame Impala", "", false),
        Track("2", "Synthwave Sunset", "Neon Night", "", false),
        Track("3", "Focus Ambient Noise", "Philips Offline", "", true),
        Track("4", "Lofi Beats", "Chillhop", "", false)
    )
    val currentTrackIndex = MutableStateFlow(0).asStateFlow()
    fun playTrack(index: Int) {}
    fun playNextTrack() {}
    fun playPreviousTrack() {}
    fun playMediaPlayer() {}
    fun pauseMediaPlayer() {}
""")

    f.write("}\n")

print("Created stub")
