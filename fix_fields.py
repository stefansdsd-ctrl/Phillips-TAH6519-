import re

content = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt").read()

content = content.replace("object UpdateAvailable : UpdateState()", "data class UpdateAvailable(val version: String, val changelog: String) : UpdateState()")
content = content.replace("object Updating : UpdateState()", "data class Updating(val statusMessage: String, val progress: Float) : UpdateState()")
content = content.replace("object UpdateComplete : UpdateState()", "data class UpdateComplete(val newVersion: String) : UpdateState()")

content = content.replace("data class ScannedDevice(val name: String, val address: String, val rssi: Int = 0)", "data class ScannedDevice(val name: String, val address: String, val rssi: Int = 0, val isHeadphone: Boolean = false)")

factory = """
import androidx.lifecycle.ViewModelProvider
class HeadphoneViewModelFactory(private val application: android.app.Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HeadphoneViewModel(application) as T
    }
}
"""

content = content.replace("class HeadphoneViewModel(", factory + "\nclass HeadphoneViewModel(")

with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
    f.write(content)

