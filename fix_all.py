import re

vm_path = "/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt"
content = open(vm_path).read()

content = content.replace("class HeadphoneViewModelFactory(private val application: android.app.Application) : ViewModelProvider.Factory {", "import com.example.data.HeadphoneRepository\nclass HeadphoneViewModelFactory(private val application: android.app.Application, private val repository: HeadphoneRepository) : ViewModelProvider.Factory {")
content = content.replace("return HeadphoneViewModel(application) as T", "return HeadphoneViewModel(application, repository) as T")
content = content.replace("class HeadphoneViewModel(application: Application) : AndroidViewModel(application) {", "class HeadphoneViewModel(application: Application, private val repository: HeadphoneRepository) : AndroidViewModel(application) {")

with open(vm_path, "w") as f:
    f.write(content)

dashboard_path = "/app/applet/app/src/main/java/com/example/ui/MediaDashboard.kt"
dashboard = open(dashboard_path).read()
if "import androidx.compose.foundation.BorderStroke" not in dashboard:
    dashboard = "import androidx.compose.foundation.BorderStroke\n" + dashboard
with open(dashboard_path, "w") as f:
    f.write(dashboard)
