content = open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt").read()

content = content.replace("class HeadphoneViewModelFactory", "import androidx.lifecycle.ViewModel\nclass HeadphoneViewModelFactory")

content = content.replace("null as HeadphoneSettings", "HeadphoneSettings()")
content = content.replace("null as Boolean", "false")
content = content.replace("null as Int", "0")
content = content.replace("null as Float", "0f")
content = content.replace("null as String", '""')
content = content.replace("null as UpdateState", "UpdateState.Idle")
content = content.replace("null as List<ScannedDevice>", "emptyList()")

with open("/app/applet/app/src/main/java/com/example/ui/HeadphoneViewModel.kt", "w") as f:
    f.write(content)
